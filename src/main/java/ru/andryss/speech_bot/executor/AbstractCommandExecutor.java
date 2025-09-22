package ru.andryss.speech_bot.executor;

import java.util.Objects;

import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

public abstract class AbstractCommandExecutor implements UpdateExecutor {

    /**
     * Returns object describing command. See {@link CommandInfo}
     */
    public abstract CommandInfo getCommandInfo();

    @Override
    public boolean canProcess(Update update) {
        return update.hasMessage() &&
                update.getMessage().hasEntities() &&
                update.getMessage().getEntities().stream()
                        .filter(entity -> Objects.equals("bot_command", entity.getType()))
                        .map(this::extractCommand)
                        .anyMatch(getCommandInfo().command()::equals);
    }

    private String extractCommand(MessageEntity entity) {
        String text = entity.getText();
        int tagIndex = text.indexOf('@');
        return tagIndex == -1 ? text : text.substring(0, tagIndex);
    }

    /**
     * Record object describing bot command
     * @param command name of the command including leading slash (e.g. "/help")
     */
    public record CommandInfo(String command) {
    }
}
