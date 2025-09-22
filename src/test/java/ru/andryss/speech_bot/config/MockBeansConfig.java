package ru.andryss.speech_bot.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import yandex.cloud.api.ai.stt.v3.AsyncRecognizerGrpc.AsyncRecognizerBlockingStub;

@Configuration
public class MockBeansConfig {

    @Bean
    public AsyncRecognizerBlockingStub yandexGptApi() {
        return Mockito.mock(AsyncRecognizerBlockingStub.class);
    }
}
