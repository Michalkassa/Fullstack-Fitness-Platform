package com.lockedin.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.lockedin.api.dto.Macros;
import com.lockedin.api.dto.MacrosRequest;
import com.lockedin.api.dto.NutritionEntryResponse;
import com.lockedin.api.dto.NutritionSummaryResponse;
import com.lockedin.services.NutritionService;

/** docs/API.md, "Nutrition": endpoints 29–33. */
@RestController
@RequestMapping("/api/nutrition")
class NutritionController {

    private final NutritionService nutritionService;

    NutritionController(NutritionService nutritionService) {
        this.nutritionService = nutritionService;
    }

    /** 29. Oldest first. */
    @GetMapping("/entries")
    List<NutritionEntryResponse> listEntries(@CurrentUser UUID userId) {
        return nutritionService.listEntries(userId);
    }

    /** 30. Recorded with the current time. */
    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    NutritionEntryResponse createEntry(@CurrentUser UUID userId, @Valid @RequestBody MacrosRequest request) {
        return nutritionService.createEntry(userId, request);
    }

    /** 31. 404 if it doesn't exist or isn't the user's. */
    @DeleteMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteEntry(@CurrentUser UUID userId, @PathVariable UUID id) {
        nutritionService.deleteEntry(userId, id);
    }

    /** 32. Creates or replaces the user's single daily goal. */
    @PutMapping("/goal")
    Macros setGoal(@CurrentUser UUID userId, @Valid @RequestBody MacrosRequest request) {
        return nutritionService.setGoal(userId, request);
    }

    /**
     * 33. Adds up entries with {@code createdAt >= since}, the start of the user's local day.
     * Without {@code since}, the service uses the start of the current UTC day.
     */
    @GetMapping("/summary")
    NutritionSummaryResponse summary(@CurrentUser UUID userId, @RequestParam(required = false) Instant since) {
        return nutritionService.summary(userId, since);
    }
}
