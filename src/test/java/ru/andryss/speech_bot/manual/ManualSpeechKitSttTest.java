package ru.andryss.speech_bot.manual;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import com.google.protobuf.ByteString;
import io.grpc.CallCredentials;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.client.security.CallCredentialsHelper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.andryss.speech_bot.BaseTest;

import yandex.cloud.api.ai.stt.v3.AsyncRecognizerGrpc.AsyncRecognizerBlockingStub;
import yandex.cloud.api.ai.stt.v3.Stt;
import yandex.cloud.api.ai.stt.v3.Stt.AlternativeUpdate;
import yandex.cloud.api.ai.stt.v3.Stt.AudioFormatOptions;
import yandex.cloud.api.ai.stt.v3.Stt.ContainerAudio;
import yandex.cloud.api.ai.stt.v3.Stt.ContainerAudio.ContainerAudioType;
import yandex.cloud.api.ai.stt.v3.Stt.RecognitionModelOptions;
import yandex.cloud.api.ai.stt.v3.Stt.RecognizeFileRequest;
import yandex.cloud.api.ai.stt.v3.Stt.StreamingResponse;
import yandex.cloud.api.ai.stt.v3.SttService;
import yandex.cloud.api.operation.OperationOuterClass.Operation;

@Slf4j
@Disabled("For manual testing only")
public class ManualSpeechKitSttTest extends BaseTest {

    private static final CallCredentials CREDENTIALS = CallCredentialsHelper.authorizationHeader("Api-Key mock-api-key");

    @GrpcClient("speech-kit-grpc")
    AsyncRecognizerBlockingStub asyncRecognizerStub;

    @Test
    void testSpeechToText() {
        byte[] fileBytes;
        try (InputStream input = new FileInputStream("/Users/andryssssss/Downloads/2025-09-20 18.08.48.ogg")) {
            fileBytes = input.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Operation response = asyncRecognizerStub.withCallCredentials(CREDENTIALS)
                .recognizeFile(RecognizeFileRequest.newBuilder()
                        .setContent(ByteString.copyFrom(fileBytes))
                        .setRecognitionModel(RecognitionModelOptions.newBuilder()
                                .setAudioFormat(AudioFormatOptions.newBuilder()
                                        .setContainerAudio(ContainerAudio.newBuilder()
                                                .setContainerAudioType(ContainerAudioType.OGG_OPUS)
                                                .build())
                                        .build())
                                .build())
                        .build());

        log.info("{}", response);
    }

    @Test
    void testGetRecognition() {
        Iterator<StreamingResponse> response = asyncRecognizerStub.withCallCredentials(CREDENTIALS)
                .getRecognition(SttService.GetRecognitionRequest.newBuilder()
                        .setOperationId("f8dp2qsrmobtsa4o2d06")
                        .build());

        while (response.hasNext()) {
            StreamingResponse next = response.next();
            if (next.getFinal() != AlternativeUpdate.getDefaultInstance()) {
                String text = next.getFinal().getAlternatives(0).getText();
                log.info("{}", text);
            } else if (next.getFinalRefinement() != Stt.FinalRefinement.getDefaultInstance()) {
                String text = next.getFinalRefinement().getNormalizedText().getAlternatives(0).getText();
                log.info("{}", text);
            } else {
                log.info("{}", next);
            }
        }
    }
}
