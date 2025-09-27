package ru.andryss.speech_bot.config.requestid;

import java.util.UUID;

import org.slf4j.MDC;

/**
 * Интерфейс, описывающий общую работу с идентификатором запроса
 */
public interface RequestIdAware {

    /**
     * Переменная для хранения идентификатора запроса в контексте.
     */
    String REQUEST_ID_VARIABLE = "requestId";
    /**
     * Заголовок для хранения идентификатора запроса.
     */
    String REQUEST_ID_HEADER = "X-Request-Id";

    /**
     * Сгенерировать и вернуть новый идентификатор запроса.
     */
    default String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Присвоить текущему обработчику заданный идентификатор запроса.
     * Должен быть очищен после обработки запроса при помощи {@link #clearRequestId()}
     */
    default void assignRequestId(String requestId) {
        MDC.put(REQUEST_ID_VARIABLE, requestId);
    }

    /**
     * Получить идентификатор обрабатываемого запроса
     */
    default String fetchRequestId() {
        return MDC.get(REQUEST_ID_VARIABLE);
    }

    /**
     * Очищение у обработчика идентификатор запроса
     */
    default void clearRequestId() {
        MDC.remove(REQUEST_ID_VARIABLE);
    }

}
