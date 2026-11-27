package com.lockedin.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lockedin.models.BaseEntity;
import com.lockedin.models.BodyWeight;
import com.lockedin.models.Log;
import com.lockedin.models.NutritionEntry;
import com.lockedin.persistence.repositories.BodyWeightRepository;
import com.lockedin.persistence.repositories.LogRepository;
import com.lockedin.persistence.repositories.NutritionEntryRepository;
import com.lockedin.persistence.repositories.UserRepository;

/**
 * Exports (docs/API.md, "Export"). CSV: rows oldest first, UTC dates, RFC 4180 escaping. PDF: a one-page
 * progress report.
 */
@Service
@Transactional(readOnly = true)
public class ExportService {

    private static final String CRLF = "\r\n";

    private final LogRepository logs;
    private final BodyWeightRepository bodyWeights;
    private final NutritionEntryRepository nutrition;
    private final UserRepository users;
    private final AnalyticsService analytics;
    private final Clock clock;

    public ExportService(LogRepository logs, BodyWeightRepository bodyWeights, NutritionEntryRepository nutrition,
                         UserRepository users, AnalyticsService analytics, Clock clock) {
        this.logs = logs;
        this.bodyWeights = bodyWeights;
        this.nutrition = nutrition;
        this.users = users;
        this.analytics = analytics;
        this.clock = clock;
    }

    /**
     * The user's email, today's date (UTC), total sets and volume (Σ weight × reps), first and latest body
     * weight, and the personal records from {@link AnalyticsService#records}.
     */
    public byte[] pdf(UUID userId) {
        String email = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found")).getEmail();
        List<Log> userLogs = logs.findByUserIdOrderByCreatedAtAsc(userId);
        double volume = userLogs.stream().mapToDouble(log -> log.getWeight() * log.getReps()).sum();
        List<BodyWeight> weights = bodyWeights.findByUserIdOrderByCreatedAtAsc(userId);
        return ProgressReport.render(email, LocalDate.now(clock), userLogs.size(), volume,
            weights.isEmpty() ? null : weight(weights.getFirst()),
            weights.isEmpty() ? null : weight(weights.getLast()),
            analytics.records(userId));
    }

    private static ProgressReport.Weight weight(BodyWeight entry) {
        return new ProgressReport.Weight(entry.getWeight(), AnalyticsService.dayKey(entry.getCreatedAt()));
    }

    /** {@code type} is {@code logs}, {@code bodyweight} or {@code nutrition}; anything else is a 400. */
    public String csv(UUID userId, String type) {
        StringBuilder out = new StringBuilder();
        switch (type) {
            case "logs" -> {
                row(out, "date", "exercise", "weight_kg", "reps", "est_1rm_kg");
                for (Log log : logs.findByUserIdOrderByCreatedAtAsc(userId)) {
                    row(out, date(log), log.getExercise().getName(), number(log.getWeight()),
                        String.valueOf(log.getReps()), String.valueOf(log.getOneRepMax()));
                }
            }
            case "bodyweight" -> {
                row(out, "date", "weight_kg");
                for (BodyWeight entry : bodyWeights.findByUserIdOrderByCreatedAtAsc(userId)) {
                    row(out, date(entry), number(entry.getWeight()));
                }
            }
            case "nutrition" -> {
                row(out, "date", "calories", "protein_g", "carbs_g", "fat_g");
                for (NutritionEntry entry : nutrition.findByUserIdOrderByCreatedAtAsc(userId)) {
                    row(out, date(entry), String.valueOf(entry.getCalories()), number(entry.getProtein()),
                        number(entry.getCarbs()), number(entry.getFat()));
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unknown export type: choose logs, bodyweight or nutrition");
        }
        return out.toString();
    }

    private static void row(StringBuilder out, String... fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(escape(fields[i]));
        }
        out.append(CRLF);
    }

    /** RFC 4180: quote a field containing a comma, quote, CR or LF, and double the quotes inside. */
    static String escape(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\r") || field.contains("\n")) {
            return '"' + field.replace("\"", "\"\"") + '"';
        }
        return field;
    }

    /** 100.0 → "100", 82.5 → "82.5". */
    static String number(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String date(BaseEntity entity) {
        return AnalyticsService.dayKey(entity.getCreatedAt()).toString();
    }
}
