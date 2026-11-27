package com.lockedin.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * docs/API.md, "Export": endpoints 37–38. Both send {@code Content-Disposition: attachment; filename="..."},
 * which the frontend uses as the file name.
 */
@RestController
@RequestMapping("/api/export")
class ExportController {

    /**
     * 37. {@code type} is {@code logs}, {@code bodyweight} or {@code nutrition}; anything else is a 400.
     * {@code text/csv; charset=utf-8}, rows oldest first, fields escaped per RFC 4180.
     */
    @GetMapping("/csv")
    ResponseEntity<String> csv(@RequestParam(defaultValue = "logs") String type) {
        throw new NotImplementedException();
    }

    /** 38. A one-page progress report, {@code application/pdf}. */
    @GetMapping("/pdf")
    ResponseEntity<byte[]> pdf() {
        throw new NotImplementedException();
    }
}
