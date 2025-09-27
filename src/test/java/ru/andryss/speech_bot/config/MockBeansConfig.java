package ru.andryss.speech_bot.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import yandex.cloud.api.ai.stt.v3.AsyncRecognizerGrpc.AsyncRecognizerBlockingV2Stub;

@Configuration
public class MockBeansConfig {

    @Bean
    public AsyncRecognizerBlockingV2Stub asyncRecognizerStub() {
        return Mockito.mock(AsyncRecognizerBlockingV2Stub.class);
    }
}
