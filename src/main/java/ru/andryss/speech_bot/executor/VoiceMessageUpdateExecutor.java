package ru.andryss.speech_bot.executor;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.andryss.speech_bot.config.VoiceMessageProperties;
import ru.andryss.speech_bot.facade.SpeechKitFacade;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceMessageUpdateExecutor implements UpdateExecutor {

    private final VoiceMessageProperties properties;
    private final SpeechKitFacade speechKitFacade;

    @Override
    public boolean canProcess(Update update) {
        return update.hasMessage()
                && update.getMessage().hasVoice()
                && properties.getAllowedUserIds().contains(update.getMessage().getFrom().getId());
    }

    @Override
    public void process(Update update, AbsSender sender) throws Exception {
        DefaultAbsSender defaultAbsSender = (DefaultAbsSender) sender;
        Message message = update.getMessage();
        String chatId = message.getChatId().toString();
        Voice voice = message.getVoice();

        if (!properties.getSupportedVoiceMimeTypes().contains(voice.getMimeType())) {
            sender.execute(new SendMessage(chatId, "Mime type " + voice.getMimeType() + " is not supported"));
            return;
        }

        if (voice.getFileSize() > properties.getMaxVoiceMaxSizeBytes()) {
            sender.execute(new SendMessage(chatId, "Voice message is too large"));
            return;
        }

        sender.execute(new SendChatAction(chatId, ActionType.TYPING.toString(), null));

        String fileId = voice.getFileId();

        String operationId;
        File voiceMessageFile = null;
        try {
            voiceMessageFile = Files.createTempFile("voice_", "").toFile();
            defaultAbsSender.downloadFile(sender.execute(new GetFile(fileId)), voiceMessageFile);
            operationId = speechKitFacade.recognizeOggFileAsync(voiceMessageFile);
        } catch (Exception e) {
            log.error("Unexpected error occurred during voice file downloading and recognition", e);
            sender.execute(new SendMessage(chatId, "Internal error occurred"));
            throw new RuntimeException(e);
        } finally {
            FileUtils.deleteQuietly(voiceMessageFile);
        }

        String result = null;
        int totalAttempts = 0;
        while (totalAttempts < properties.getMaxVoiceRecognitionAttempts()) {
            sender.execute(new SendChatAction(chatId, ActionType.TYPING.toString(), null));

            //noinspection BusyWait
            Thread.sleep(properties.getWaitBetweenRecognitionAttemptsMs());

            try {
                Optional<String> finalRefinement = speechKitFacade.getFinalRefinement(operationId);
                if (finalRefinement.isPresent()) {
                    result = finalRefinement.get();
                    break;
                }
            } catch (Exception e) {
                log.error("Unexpected error occurred during getting final refinement", e);
                sender.execute(new SendMessage(chatId, "Internal error occurred"));
                throw new RuntimeException(e);
            }

            totalAttempts++;
        }

        if (result != null) {
            SendMessage sendMessage = new SendMessage(chatId, result);
            sendMessage.setReplyToMessageId(message.getMessageId());
            sender.execute(sendMessage);
        } else {
            sender.execute(new SendMessage(chatId, "Recognition takes too much time..."));
        }
    }
}
