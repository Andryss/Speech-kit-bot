package ru.andryss.speech_bot.executor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
@RequiredArgsConstructor
public class StartCommandExecutor extends AbstractCommandExecutor {

    @Getter
    private final CommandInfo commandInfo = new CommandInfo("/start");

    @Override
    public void process(Update update, AbsSender sender) throws Exception {
        Message message = update.getMessage();
        String chatId = message.getChatId().toString();

        User user = message.getFrom();

        String text = "Hi %s!\n`%s`".formatted(user.getFirstName(), user.getId());

        SendMessage sendMessage = new SendMessage(chatId, text);
        sendMessage.enableMarkdown(true);

        sender.execute(sendMessage);
    }
}
