package com.lockedin.nutrition;

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

import com.lockedin.common.NotImplementedException;

import jakarta.validation.Valid;

/** docs/API.md, "Nutrition": endpoints 29–33. */
@RestController
@RequestMapping("/api/nutrition")
class NutritionController {

    /** 29. Oldest first. */
    @GetMapping("/entries")
    List<NutritionEntryResponse> listEntries() {
        throw new NotImplementedException();
    }

    /** 30. Recorded with the current time. */
    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    NutritionEntryResponse createEntry(@Valid @RequestBody MacrosRequest request) {
        throw new NotImplementedException();
    }

    /** 31. 404 if it doesn't exist or isn't the user's. */
    @DeleteMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteEntry(@PathVariable UUID id) {
        throw new NotImplementedException();
    }

    /** 32. Creates or replaces the user's single daily goal. */
    @PutMapping("/goal")
    Macros setGoal(@Valid @RequestBody MacrosRequest request) {
        throw new NotImplementedException();
    }

    /**
     * 33. Adds up entries with {@code createdAt >= since}, the start of the user's local day.
     * Without {@code since}, use the start of the current UTC day.
     */
    @GetMapping("/summary")
    NutritionSummaryResponse summary(@RequestParam(required = false) Instant since) {
        throw new NotImplementedException();
    }
}
