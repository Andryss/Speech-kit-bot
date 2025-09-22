package ru.andryss.speech_bot.config;

import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.client.security.CallCredentialsHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import yandex.cloud.api.ai.stt.v3.AsyncRecognizerGrpc.AsyncRecognizerBlockingStub;

@Configuration
@Profile("!functionalTest")
public class SpeechKitClientConfig {

    @GrpcClient("speech-kit-grpc")
    private AsyncRecognizerBlockingStub asyncRecognizerStub;

    @Bean
    public AsyncRecognizerBlockingStub asyncRecognizerStub(SpeechKitProperties properties) {
        return asyncRecognizerStub.withCallCredentials(
                CallCredentialsHelper.authorizationHeader("Api-Key " + properties.getApiKey())
        );
    }
}
