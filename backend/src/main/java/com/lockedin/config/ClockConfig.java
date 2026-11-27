package com.lockedin.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** UTC system clock, so tests can swap in a fixed one. */
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
