package ru.andryss.speech_bot.executor;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

/**
 * Interface describing update executor. Executor invocation pipeline:
 * 1) If executor is inactive then no other methods will be invoked
 * 2) If executor can process given {@link Update} then process method will be invoked with same update data
 */
public interface UpdateExecutor {
    /**
     * Returns true if executor is active and ready for processing. Any executor is active by default
     */
    default boolean isActive() {
        return true;
    }

    /**
     * Determines whether executor can process given update. Active executor can process any update by default
     */
    default boolean canProcess(Update update) {
        return true;
    }

    /**
     * Method containing main processing logic. Method only invokes when executor is active and
     * {@link UpdateExecutor#canProcess(Update)} was invokek with same {@link Update} object
     */
    void process(Update update, AbsSender sender) throws Exception;
}
