package ru.andryss.speech_bot.facade;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;
import java.util.StringJoiner;

import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import yandex.cloud.api.ai.stt.v3.AsyncRecognizerGrpc.AsyncRecognizerBlockingStub;
import yandex.cloud.api.ai.stt.v3.Stt;
import yandex.cloud.api.ai.stt.v3.Stt.AudioCursors;
import yandex.cloud.api.ai.stt.v3.Stt.AudioFormatOptions;
import yandex.cloud.api.ai.stt.v3.Stt.ContainerAudio;
import yandex.cloud.api.ai.stt.v3.Stt.ContainerAudio.ContainerAudioType;
import yandex.cloud.api.ai.stt.v3.Stt.FinalRefinement;
import yandex.cloud.api.ai.stt.v3.Stt.RecognitionModelOptions;
import yandex.cloud.api.ai.stt.v3.Stt.StreamingResponse;
import yandex.cloud.api.ai.stt.v3.SttService.GetRecognitionRequest;
import yandex.cloud.api.operation.OperationOuterClass.Operation;

/**
 * Facade class for Yandex SpeechKit API
 */
@Component
@RequiredArgsConstructor
public class SpeechKitFacade {

    private final AsyncRecognizerBlockingStub asyncRecognizerStub;

    /**
     * Start async file recognition with type audio/ogg. Returns operation id
     */
    public String recognizeOggFileAsync(File file) {
        byte[] fileBytes;
        try (InputStream input = new FileInputStream(file)) {
            fileBytes = input.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Operation operation = asyncRecognizerStub
                .recognizeFile(Stt.RecognizeFileRequest.newBuilder()
                        .setContent(ByteString.copyFrom(fileBytes))
                        .setRecognitionModel(RecognitionModelOptions.newBuilder()
                                .setAudioFormat(AudioFormatOptions.newBuilder()
                                        .setContainerAudio(ContainerAudio.newBuilder()
                                                .setContainerAudioType(ContainerAudioType.OGG_OPUS)
                                                .build())
                                        .build())
                                .build())
                        .build());

        return operation.getId();
    }

    /**
     * Gets recognition by operation id. If recognition is still in progress empty options is returned.
     */
    public Optional<String> getFinalRefinement(String operationId) {
        Iterator<StreamingResponse> response = asyncRecognizerStub
                .getRecognition(GetRecognitionRequest.newBuilder()
                        .setOperationId(operationId)
                        .build());

        StringJoiner joiner = new StringJoiner("\n");
        boolean isEnded = false;

        while (response.hasNext()) {
            StreamingResponse next = response.next();

            if (next.getFinalRefinement() != FinalRefinement.getDefaultInstance()) {
                String text = next.getFinalRefinement().getNormalizedText().getAlternatives(0).getText();
                joiner.add(text);
            }

            if (!isEnded && next.hasAudioCursors()) {
                AudioCursors audioCursors = next.getAudioCursors();
                isEnded = (audioCursors.getReceivedDataMs() == audioCursors.getFinalTimeMs());
            }
        }

        if (isEnded) {
            return Optional.of(joiner.toString());
        }
        return Optional.empty();
    }
}
