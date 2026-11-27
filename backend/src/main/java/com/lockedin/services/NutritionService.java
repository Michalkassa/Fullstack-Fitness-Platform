package com.lockedin.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lockedin.api.dto.Macros;
import com.lockedin.api.dto.MacrosRequest;
import com.lockedin.api.dto.NutritionEntryResponse;
import com.lockedin.api.dto.NutritionSummaryResponse;
import com.lockedin.models.NutritionEntry;
import com.lockedin.models.NutritionGoal;
import com.lockedin.persistence.repositories.NutritionEntryRepository;
import com.lockedin.persistence.repositories.NutritionGoalRepository;
import com.lockedin.persistence.repositories.UserRepository;

/** Food entries and the daily goal. Missing macros count as 0. */
@Service
@Transactional
public class NutritionService {

    private final NutritionEntryRepository entries;
    private final NutritionGoalRepository goals;
    private final UserRepository users;

    public NutritionService(NutritionEntryRepository entries, NutritionGoalRepository goals, UserRepository users) {
        this.entries = entries;
        this.goals = goals;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<NutritionEntryResponse> listEntries(UUID userId) {
        return entries.findByUserIdOrderByCreatedAtAsc(userId).stream().map(Responses::of).toList();
    }

    public NutritionEntryResponse createEntry(UUID userId, MacrosRequest request) {
        NutritionEntry entry = new NutritionEntry(users.getReferenceById(userId), request.calories(),
            orZero(request.protein()), orZero(request.carbs()), orZero(request.fat()));
        return Responses.of(entries.save(entry));
    }

    public void deleteEntry(UUID userId, UUID id) {
        entries.delete(entries.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new NotFoundException("Nutrition entry not found")));
    }

    /** Creates or replaces the user's single goal. */
    public Macros setGoal(UUID userId, MacrosRequest request) {
        int calories = request.calories();
        double protein = orZero(request.protein());
        double carbs = orZero(request.carbs());
        double fat = orZero(request.fat());
        NutritionGoal goal = goals.findByUserId(userId)
            .map(existing -> {
                existing.update(calories, protein, carbs, fat);
                return existing;
            })
            .orElseGet(() -> goals.save(new NutritionGoal(users.getReferenceById(userId), calories, protein, carbs, fat)));
        return Responses.of(goal);
    }

    /** Adds up entries with {@code createdAt >= since}; without {@code since}, from the start of today (UTC). */
    @Transactional(readOnly = true)
    public NutritionSummaryResponse summary(UUID userId, Instant since) {
        Instant from = Objects.requireNonNullElseGet(since,
            () -> LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant());
        int calories = 0;
        double protein = 0;
        double carbs = 0;
        double fat = 0;
        for (NutritionEntry entry : entries.findByUserIdAndCreatedAtGreaterThanEqual(userId, from)) {
            calories += entry.getCalories();
            protein += entry.getProtein();
            carbs += entry.getCarbs();
            fat += entry.getFat();
        }
        Macros goal = goals.findByUserId(userId).map(Responses::of).orElse(null);
        return new NutritionSummaryResponse(goal, new Macros(calories, protein, carbs, fat));
    }

    private static double orZero(Double value) {
        return value == null ? 0 : value;
    }
}
