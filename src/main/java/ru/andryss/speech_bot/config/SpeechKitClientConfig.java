package ru.andryss.speech_bot.config;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import yandex.cloud.api.ai.stt.v3.AsyncRecognizerGrpc;
import yandex.cloud.api.ai.stt.v3.AsyncRecognizerGrpc.AsyncRecognizerBlockingV2Stub;
import yandex.cloud.api.ai.stt.v3.AsyncRecognizerGrpc.AsyncRecognizerStub;

import static net.devh.boot.grpc.client.security.CallCredentialsHelper.authorizationHeader;

@Configuration
@Profile("!functionalTest")
public class SpeechKitClientConfig {

    @GrpcClient("speech-kit-grpc")
    private AsyncRecognizerStub asyncRecognizerStub;

    @Bean
    public AsyncRecognizerBlockingV2Stub asyncRecognizerStub(SpeechKitProperties properties) {
        return AsyncRecognizerGrpc
                .newBlockingV2Stub(asyncRecognizerStub.getChannel())
                .withCallCredentials(authorizationHeader("Api-Key " + properties.getApiKey()));
    }
}
