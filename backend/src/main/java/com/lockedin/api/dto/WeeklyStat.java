package com.lockedin.api.dto;

import java.time.LocalDate;

/** {@code week} is the Monday (UTC) that starts it; {@code sessions} counts distinct training days. */
public record WeeklyStat(LocalDate week, double volume, int sessions) {
}
