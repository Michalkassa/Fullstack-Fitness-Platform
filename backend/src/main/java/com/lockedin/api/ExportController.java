package com.lockedin.api;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lockedin.services.ExportService;

/**
 * docs/API.md, "Export": endpoints 37–38. Both send {@code Content-Disposition: attachment; filename="..."},
 * which the frontend uses as the file name.
 */
@RestController
@RequestMapping("/api/export")
class ExportController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ExportService exportService;

    ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * 37. {@code type} is {@code logs}, {@code bodyweight} or {@code nutrition}; anything else is a 400.
     * {@code text/csv; charset=utf-8}, rows oldest first, fields escaped per RFC 4180.
     */
    @GetMapping("/csv")
    ResponseEntity<String> csv(@CurrentUser UUID userId, @RequestParam(defaultValue = "logs") String type) {
        String body = exportService.csv(userId, type);
        return ResponseEntity.ok()
            .contentType(CSV)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("lockedin-" + type + ".csv").build().toString())
            .body(body);
    }

    /** 38. A one-page progress report, {@code application/pdf}. */
    @GetMapping("/pdf")
    ResponseEntity<byte[]> pdf(@CurrentUser UUID userId) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("lockedin-progress-report.pdf").build().toString())
            .body(exportService.pdf(userId));
    }
}
