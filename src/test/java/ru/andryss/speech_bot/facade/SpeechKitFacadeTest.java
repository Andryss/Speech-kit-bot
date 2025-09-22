package ru.andryss.speech_bot.facade;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import com.google.protobuf.ByteString;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.andryss.speech_bot.BaseTest;

import yandex.cloud.api.ai.stt.v3.AsyncRecognizerGrpc.AsyncRecognizerBlockingStub;
import yandex.cloud.api.ai.stt.v3.Stt.Alternative;
import yandex.cloud.api.ai.stt.v3.Stt.AlternativeUpdate;
import yandex.cloud.api.ai.stt.v3.Stt.AudioCursors;
import yandex.cloud.api.ai.stt.v3.Stt.AudioFormatOptions;
import yandex.cloud.api.ai.stt.v3.Stt.ContainerAudio;
import yandex.cloud.api.ai.stt.v3.Stt.ContainerAudio.ContainerAudioType;
import yandex.cloud.api.ai.stt.v3.Stt.FinalRefinement;
import yandex.cloud.api.ai.stt.v3.Stt.RecognitionModelOptions;
import yandex.cloud.api.ai.stt.v3.Stt.RecognizeFileRequest;
import yandex.cloud.api.ai.stt.v3.Stt.StreamingResponse;
import yandex.cloud.api.ai.stt.v3.SttService.GetRecognitionRequest;
import yandex.cloud.api.operation.OperationOuterClass.Operation;

class SpeechKitFacadeTest extends BaseTest {

    @Autowired
    SpeechKitFacade speechKitFacade;

    @Autowired
    AsyncRecognizerBlockingStub asyncRecognizerStub;

    @Test
    void testRecognizeOggFileAsync() throws IOException {
        File file = File.createTempFile("tmp-", "-file");
        byte[] fileContent = {1, 2, 3, 4, 5};

        try (OutputStream stream = new FileOutputStream(file)) {
            stream.write(fileContent);
        }

        Mockito.when(asyncRecognizerStub.recognizeFile(Mockito.any()))
                .thenReturn(Operation.newBuilder()
                        .setId("operation-id")
                        .build());

        String operationId = speechKitFacade.recognizeOggFileAsync(file);

        Assertions.assertThat(operationId).isEqualTo("operation-id");

        Mockito.verify(asyncRecognizerStub).recognizeFile(RecognizeFileRequest.newBuilder()
                .setContent(ByteString.copyFrom(fileContent))
                .setRecognitionModel(RecognitionModelOptions.newBuilder()
                        .setAudioFormat(AudioFormatOptions.newBuilder()
                                .setContainerAudio(ContainerAudio.newBuilder()
                                        .setContainerAudioType(ContainerAudioType.OGG_OPUS)
                                        .build())
                                .build())
                        .build())
                .build());

        FileUtils.deleteQuietly(file);
    }

    @Test
    void testGetFinalRefinement() {
        Mockito.when(asyncRecognizerStub.getRecognition(Mockito.any()))
                .thenReturn(List.of(
                        buildFinalWithText("final 1"),
                        buildFinalRefinementWithText("final refinement 1"),
                        buildFinalWithText("final 2"),
                        buildFinalRefinementWithText("final refinement 2"),
                        buildFinalWithText("final 3")
                ).iterator());

        Optional<String> refinement = speechKitFacade.getFinalRefinement("operation-id");

        Assertions.assertThat(refinement).isPresent()
                .get().isEqualTo("final refinement 1\nfinal refinement 2");

        Mockito.verify(asyncRecognizerStub).getRecognition(GetRecognitionRequest.newBuilder()
                .setOperationId("operation-id")
                .build());
    }

    private static StreamingResponse buildFinalWithText(String text) {
        return StreamingResponse.newBuilder()
                .setFinal(AlternativeUpdate.newBuilder()
                        .addAlternatives(Alternative.newBuilder()
                                .setText(text)
                                .build())
                        .build())
                .build();
    }

    private static StreamingResponse buildFinalRefinementWithText(String text) {
        return StreamingResponse.newBuilder()
                .setFinalRefinement(FinalRefinement.newBuilder()
                        .setNormalizedText(AlternativeUpdate.newBuilder()
                                .addAlternatives(Alternative.newBuilder()
                                        .setText(text)
                                        .build())
                                .build())
                        .build())
                .setAudioCursors(AudioCursors.newBuilder()
                        .setReceivedDataMs(2020)
                        .setFinalTimeMs(2020)
                        .build())
                .build();
    }

}