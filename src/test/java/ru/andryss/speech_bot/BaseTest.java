package ru.andryss.speech_bot;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.andryss.speech_bot.config.TestClockConfig;

@SpringBootTest
@ActiveProfiles("functionalTest")
@Import(TestClockConfig.class)
public abstract class BaseTest {
}
