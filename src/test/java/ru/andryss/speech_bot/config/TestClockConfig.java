package ru.andryss.speech_bot.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestClockConfig {

    // 2025-06-15 16:30:10 +00:00
    public static final int EPOCH_SECONDS = 1749994210;

    @Bean
    public Clock clock() {
        return Clock.fixed(Instant.ofEpochSecond(EPOCH_SECONDS), ZoneOffset.UTC);
    }
}
