package ru.andryss.speech_bot.config;

import io.grpc.CallCredentials;
import net.devh.boot.grpc.client.security.CallCredentialsHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpeechKitClientConfig {

    @Bean
    public CallCredentials apiKeyCredentials(SpeechKitProperties properties) {
        return CallCredentialsHelper.authorizationHeader("Api Key " + properties.getApiKey());
    }
}
