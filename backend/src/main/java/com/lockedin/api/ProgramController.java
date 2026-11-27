package com.lockedin.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lockedin.api.dto.WorkoutResponse;
import com.lockedin.services.ProgramService;

/** docs/API.md, "Programs": endpoints 27–28. Programs are public template workouts seeded by Flyway V2. */
@RestController
@RequestMapping("/api/programs")
class ProgramController {

    private final ProgramService programService;

    ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    /** 27. All public templates, sorted by name. */
    @GetMapping
    List<WorkoutResponse> list() {
        return programService.list();
    }

    /**
     * 28. Copies the program into the user's workouts, reusing the user's exercise with the same name or
     * creating a copy of it. 404 if {@code id} isn't a public template.
     */
    @PostMapping("/{id}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    WorkoutResponse copy(@CurrentUser UUID userId, @PathVariable UUID id) {
        return programService.copy(userId, id);
    }
}
