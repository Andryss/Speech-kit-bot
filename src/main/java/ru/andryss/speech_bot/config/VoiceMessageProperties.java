package ru.andryss.speech_bot.config;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Configuration
@ConfigurationProperties("executors.voice-message-executor.properties")
public class VoiceMessageProperties {
    @NotNull
    private List<Long> allowedUserIds = List.of();
    @NotEmpty
    private List<String> supportedVoiceMimeTypes = List.of("audio/ogg");
    @NotNull
    @Positive
    private Long maxVoiceMaxSizeBytes = 30_000_000L;
    @NotNull
    @Positive
    private Integer maxVoiceRecognitionAttempts = 5;
    @NotNull
    @Positive
    private Long waitBetweenRecognitionAttemptsMs = 4_000L;
}
