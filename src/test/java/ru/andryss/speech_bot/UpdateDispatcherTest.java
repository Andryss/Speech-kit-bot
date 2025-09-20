package ru.andryss.speech_bot;

import java.util.List;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.andryss.speech_bot.executor.UpdateExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.andryss.speech_bot.config.TestClockConfig.EPOCH_SECONDS;

class UpdateDispatcherTest extends BaseTest {

    @TestConfiguration
    static class UpdateDispatcherTestConfig {
        @Bean
        public UpdateExecutor inactiveExecutor() {
            UpdateExecutor executor = Mockito.mock(UpdateExecutor.class);
            Mockito.when(executor.isActive()).thenReturn(false);
            return executor;
        }
        @Bean
        public UpdateExecutor nothingProcessExecutor() {
            UpdateExecutor executor = Mockito.mock(UpdateExecutor.class);
            Mockito.when(executor.isActive()).thenReturn(true);
            Mockito.when(executor.canProcess(any())).thenReturn(false);
            return executor;
        }
        @Bean
        public UpdateExecutor alwaysProcessingExecutor() {
            UpdateExecutor executor = Mockito.mock(UpdateExecutor.class);
            Mockito.when(executor.isActive()).thenReturn(true);
            Mockito.when(executor.canProcess(any())).thenReturn(true);
            return executor;
        }
        @Bean
        @SneakyThrows
        public UpdateExecutor alwaysFailingExecutor() {
            UpdateExecutor executor = Mockito.mock(UpdateExecutor.class);
            Mockito.when(executor.isActive()).thenReturn(true);
            Mockito.when(executor.canProcess(any())).thenReturn(true);
            Mockito.doThrow(new RuntimeException("process failed")).when(executor).process(any(), any());
            return executor;
        }
        @Bean
        public List<UpdateExecutor> executors() {
            return List.of(
                    inactiveExecutor(),
                    nothingProcessExecutor(),
                    alwaysProcessingExecutor(),
                    alwaysFailingExecutor()
            );
        }
    }

    @Autowired
    UpdateDispatcher dispatcher;

    @Autowired
    UpdateExecutor inactiveExecutor;

    @Autowired
    UpdateExecutor nothingProcessExecutor;

    @Autowired
    UpdateExecutor alwaysProcessingExecutor;

    @Autowired
    UpdateExecutor alwaysFailingExecutor;

    @BeforeEach
    void before() {
        Mockito.clearInvocations(
                inactiveExecutor,
                nothingProcessExecutor,
                alwaysProcessingExecutor,
                alwaysFailingExecutor
        );
    }

    @Test
    void testEmptyUpdate() {
        Update emptyUpdate = new Update();

        dispatcher.onUpdateReceived(emptyUpdate);

        verifyExecutors();
        verifyNoMoreMockInteractions();
    }

    @Test
    void testNewMessageUpdate() {
        Message message = new Message();
        message.setDate(EPOCH_SECONDS - 60);

        Update update = new Update();
        update.setMessage(message);

        dispatcher.onUpdateReceived(update);

        verifyExecutors();
        verifyNoMoreMockInteractions();
    }

    @Test
    void testOldMessageUpdate() {
        Message message = new Message();
        message.setDate(EPOCH_SECONDS - 61);

        Update update = new Update();
        update.setMessage(message);

        dispatcher.onUpdateReceived(update);

        verifyNoMoreMockInteractions();
    }

    @SneakyThrows
    private void verifyExecutors() {
        verify(inactiveExecutor).isActive();

        verify(nothingProcessExecutor).isActive();
        verify(nothingProcessExecutor).canProcess(any());

        verify(alwaysProcessingExecutor).isActive();
        verify(alwaysProcessingExecutor).canProcess(any());
        verify(alwaysProcessingExecutor).process(any(), any());

        verify(alwaysFailingExecutor).isActive();
        verify(alwaysFailingExecutor).canProcess(any());
        verify(alwaysFailingExecutor).process(any(), any());
    }

    private void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(inactiveExecutor);
        verifyNoMoreInteractions(nothingProcessExecutor);
    }
}