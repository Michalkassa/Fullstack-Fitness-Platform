package com.lockedin.services;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lockedin.api.dto.ActivityDay;
import com.lockedin.api.dto.PersonalRecord;
import com.lockedin.api.dto.WeeklyStat;
import com.lockedin.models.Log;
import com.lockedin.persistence.repositories.LogRepository;

/** Stats calculated from the user's logs; rules in docs/API.md, "Business rules". All days are UTC. */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final LogRepository logs;

    public AnalyticsService(LogRepository logs) {
        this.logs = logs;
    }

    /** Volume (Σ weight × reps) and distinct training days per week, oldest week first. */
    public List<WeeklyStat> weekly(UUID userId) {
        Map<LocalDate, Double> volume = new TreeMap<>();
        Map<LocalDate, Set<LocalDate>> days = new TreeMap<>();
        for (Log log : logs.findByUserIdOrderByCreatedAtAsc(userId)) {
            LocalDate day = dayKey(log.getCreatedAt());
            LocalDate week = weekKey(day);
            volume.merge(week, log.getWeight() * log.getReps(), Double::sum);
            days.computeIfAbsent(week, k -> new HashSet<>()).add(day);
        }
        return volume.entrySet().stream()
            .map(e -> new WeeklyStat(e.getKey(), e.getValue(), days.get(e.getKey()).size()))
            .toList();
    }

    /** Logs per day, oldest first. */
    public List<ActivityDay> activity(UUID userId) {
        Map<LocalDate, Integer> counts = new TreeMap<>();
        for (Log log : logs.findByUserIdOrderByCreatedAtAsc(userId)) {
            counts.merge(dayKey(log.getCreatedAt()), 1, Integer::sum);
        }
        return counts.entrySet().stream().map(e -> new ActivityDay(e.getKey(), e.getValue())).toList();
    }

    /** Best weight, one-rep max and single-set volume per logged exercise, sorted by name. */
    public List<PersonalRecord> records(UUID userId) {
        Map<UUID, Best> best = new LinkedHashMap<>();
        // Oldest first, so a later set only replaces the best one-rep max if it's strictly higher.
        for (Log log : logs.findByUserIdOrderByCreatedAtAsc(userId)) {
            best.computeIfAbsent(log.getExercise().getId(), id -> new Best(id, log.getExercise().getName())).add(log);
        }
        List<PersonalRecord> records = new ArrayList<>();
        for (Best b : best.values()) {
            records.add(new PersonalRecord(b.exerciseId, b.name, b.weight, b.oneRepMax, b.volume, b.achievedAt));
        }
        records.sort(Comparator.comparing(PersonalRecord::exerciseName));
        return records;
    }

    static LocalDate dayKey(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    static LocalDate weekKey(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static final class Best {
        final UUID exerciseId;
        final String name;
        double weight = Double.NEGATIVE_INFINITY;
        int oneRepMax = Integer.MIN_VALUE;
        double volume = Double.NEGATIVE_INFINITY;
        LocalDate achievedAt;

        Best(UUID exerciseId, String name) {
            this.exerciseId = exerciseId;
            this.name = name;
        }

        void add(Log log) {
            weight = Math.max(weight, log.getWeight());
            volume = Math.max(volume, log.getWeight() * log.getReps());
            if (log.getOneRepMax() > oneRepMax) {
                oneRepMax = log.getOneRepMax();
                achievedAt = dayKey(log.getCreatedAt());
            }
        }
    }
}
