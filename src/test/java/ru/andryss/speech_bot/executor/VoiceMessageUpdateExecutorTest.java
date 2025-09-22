package ru.andryss.speech_bot.executor;

import java.util.List;

import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.andryss.speech_bot.BaseTest;

import yandex.cloud.api.ai.stt.v3.AsyncRecognizerGrpc.AsyncRecognizerBlockingStub;
import yandex.cloud.api.ai.stt.v3.Stt.Alternative;
import yandex.cloud.api.ai.stt.v3.Stt.AlternativeUpdate;
import yandex.cloud.api.ai.stt.v3.Stt.AudioCursors;
import yandex.cloud.api.ai.stt.v3.Stt.ContainerAudio.ContainerAudioType;
import yandex.cloud.api.ai.stt.v3.Stt.FinalRefinement;
import yandex.cloud.api.ai.stt.v3.Stt.StreamingResponse;
import yandex.cloud.api.ai.stt.v3.SttService.GetRecognitionRequest;
import yandex.cloud.api.operation.OperationOuterClass;

class VoiceMessageUpdateExecutorTest extends BaseTest {

    @Autowired
    VoiceMessageUpdateExecutor executor;

    @MockitoBean
    DefaultAbsSender sender;

    @Autowired
    AsyncRecognizerBlockingStub asyncRecognizerStub;

    @BeforeEach
    @SneakyThrows
    void before() {
        Mockito.clearInvocations(asyncRecognizerStub);
    }

    @Test
    void testIsActiveDefault() {
        Assertions.assertThat(executor.isActive()).isTrue();
    }

    @Test
    void testCanProcessNoMessage() {
        Update update = new Update();

        Assertions.assertThat(executor.canProcess(update)).isFalse();
    }

    @Test
    void testCanProcessNoVoice() {
        User user = new User();
        user.setId(123L);
        Message message = new Message();
        message.setFrom(user);
        message.setText("hi");
        Update update = new Update();
        update.setMessage(message);

        Assertions.assertThat(executor.canProcess(update)).isFalse();
    }

    @Test
    void testCanProcessNotAllowedUser() {
        User user = new User();
        user.setId(456L);
        Voice voice = new Voice();
        voice.setFileId("voice-id");
        Message message = new Message();
        message.setFrom(user);
        message.setVoice(voice);
        Update update = new Update();
        update.setMessage(message);

        Assertions.assertThat(executor.canProcess(update)).isFalse();
    }

    @Test
    void testCanProcessAllowedUser() {
        User user = new User();
        user.setId(123L);
        Voice voice = new Voice();
        voice.setFileId("voice-id");
        Message message = new Message();
        message.setFrom(user);
        message.setVoice(voice);
        Update update = new Update();
        update.setMessage(message);

        Assertions.assertThat(executor.canProcess(update)).isTrue();
    }

    @Test
    @SneakyThrows
    void testProcessUnknownMimeType() {
        User user = new User();
        user.setId(123L);
        Chat chat = new Chat();
        chat.setId(456L);
        Voice voice = new Voice();
        voice.setFileId("voice-id");
        voice.setMimeType("audio/mp3");
        Message message = new Message();
        message.setFrom(user);
        message.setChat(chat);
        message.setVoice(voice);
        Update update = new Update();
        update.setMessage(message);

        executor.process(update, sender);

        ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(sender).execute(argumentCaptor.capture());
        Assertions.assertThat(argumentCaptor.getValue())
                .extracting("chatId", "text")
                .containsExactly("456", "Mime type audio/mp3 is not supported");
        Mockito.verifyNoMoreInteractions(sender);
    }

    @Test
    @SneakyThrows
    void testProcessFileSizeLimitExceeded() {
        User user = new User();
        user.setId(123L);
        Chat chat = new Chat();
        chat.setId(456L);
        Voice voice = new Voice();
        voice.setFileId("voice-id");
        voice.setMimeType("audio/ogg");
        voice.setFileSize(30_000_001L);
        Message message = new Message();
        message.setFrom(user);
        message.setChat(chat);
        message.setVoice(voice);
        Update update = new Update();
        update.setMessage(message);

        executor.process(update, sender);

        ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(sender).execute(argumentCaptor.capture());
        Assertions.assertThat(argumentCaptor.getValue())
                .extracting("chatId", "text")
                .containsExactly("456", "Voice message is too large");
        Mockito.verifyNoMoreInteractions(sender);
    }

    @Test
    @SneakyThrows
    void testProcessSuccess() {
        Update update = getUpdateData();

        File tgFileInfo = mockGetFile();
        mockRecognizeFile();

        Mockito.when(asyncRecognizerStub.getRecognition(Mockito.any()))
                .thenReturn(List.of(
                        StreamingResponse.newBuilder()
                                .setFinalRefinement(FinalRefinement.newBuilder()
                                        .setNormalizedText(AlternativeUpdate.newBuilder()
                                                .addAlternatives(Alternative.newBuilder()
                                                        .setText("final refinement")
                                                        .build())
                                                .build())
                                        .build())
                                .setAudioCursors(AudioCursors.newBuilder()
                                        .setReceivedDataMs(1010)
                                        .setFinalTimeMs(1010)
                                        .build())
                                .build()
                ).iterator());

        executor.process(update, sender);

        verifyGetFile();
        verifyDownloadFile(tgFileInfo);
        verifyRecognizeFile();
        verifyGetRecognition(1);

        verifySendTypingAction(2);

        ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(sender).execute(argumentCaptor.capture());
        Assertions.assertThat(argumentCaptor.getValue())
                .extracting("chatId", "text")
                .containsExactly("456", "final refinement");
        Mockito.verifyNoMoreInteractions(sender);
    }

    @Test
    @SneakyThrows
    void testProcessWaitingExceeded() {
        Update update = getUpdateData();

        File tgFileInfo = mockGetFile();
        mockRecognizeFile();

        Mockito.when(asyncRecognizerStub.getRecognition(Mockito.any()))
                .thenReturn(List.of(
                        StreamingResponse.newBuilder()
                                .setFinal(AlternativeUpdate.newBuilder()
                                        .addAlternatives(Alternative.newBuilder()
                                                .setText("...")
                                                .build())
                                        .build())
                                .build()
                ).iterator());

        executor.process(update, sender);

        verifyGetFile();
        verifyDownloadFile(tgFileInfo);
        verifyRecognizeFile();
        verifyGetRecognition(5);

        verifySendTypingAction(6);

        ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(sender).execute(argumentCaptor.capture());
        Assertions.assertThat(argumentCaptor.getValue())
                .extracting("chatId", "text")
                .containsExactly("456", "Recognition takes too much time...");
        Mockito.verifyNoMoreInteractions(sender);
    }

    private static Update getUpdateData() {
        User user = new User();
        user.setId(123L);
        Chat chat = new Chat();
        chat.setId(456L);
        Voice voice = new Voice();
        voice.setFileId("voice-id");
        voice.setMimeType("audio/ogg");
        voice.setFileSize(30_000_000L);
        Message message = new Message();
        message.setFrom(user);
        message.setChat(chat);
        message.setVoice(voice);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private File mockGetFile() throws TelegramApiException {
        File tgFileInfo = new File();
        Mockito.when(sender.execute(Mockito.<GetFile>any()))
                .thenReturn(tgFileInfo);
        return tgFileInfo;
    }

    private void verifyGetFile() throws TelegramApiException {
        ArgumentCaptor<GetFile> getFileArgumentCaptor = ArgumentCaptor.forClass(GetFile.class);
        Mockito.verify(sender).execute(getFileArgumentCaptor.capture());
        Assertions.assertThat(getFileArgumentCaptor.getValue())
                .extracting("fileId")
                .isEqualTo("voice-id");
    }

    private void verifyDownloadFile(File tgFileInfo) throws TelegramApiException {
        Mockito.verify(sender).downloadFile(Mockito.same(tgFileInfo), Mockito.any());
    }

    private void mockRecognizeFile() {
        Mockito.when(asyncRecognizerStub.recognizeFile(Mockito.any()))
                .thenReturn(OperationOuterClass.Operation.newBuilder()
                        .setId("operation-id")
                        .build());
    }

    private void verifyRecognizeFile() {
        Mockito.verify(asyncRecognizerStub).recognizeFile(Mockito.assertArg(request -> {
            Assertions.assertThat(request.getContent()).isNotNull();
            Assertions.assertThat(request.getRecognitionModel().getAudioFormat().getContainerAudio().getContainerAudioType())
                    .isEqualTo(ContainerAudioType.OGG_OPUS);
        }));
    }

    private void verifyGetRecognition(int times) {
        Mockito.verify(asyncRecognizerStub, Mockito.times(times)).getRecognition(GetRecognitionRequest.newBuilder()
                .setOperationId("operation-id")
                .build());
    }

    private void verifySendTypingAction(int times) throws TelegramApiException {
        ArgumentCaptor<SendChatAction> sendActionArgumentCaptor = ArgumentCaptor.forClass(SendChatAction.class);
        Mockito.verify(sender, Mockito.times(times)).execute(sendActionArgumentCaptor.capture());
        Assertions.assertThat(sendActionArgumentCaptor.getAllValues())
                .allMatch(action -> action.getActionType() == ActionType.TYPING);
    }

}