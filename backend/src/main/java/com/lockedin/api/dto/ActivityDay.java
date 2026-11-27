package com.lockedin.api.dto;

import java.time.LocalDate;

/** Sets logged on one UTC day. */
public record ActivityDay(LocalDate date, int count) {
}
