package com.npst.loggingapi;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// full startup test, disabled until it can run against a test database
@SpringBootTest
@Disabled("""
        Placeholder context test - needs a live MySQL, so it cannot run in CI.
        Re-enabled in Step 8, backed by Testcontainers MySQL, together with the
        real repository and controller tests.
        """)
class LoggingApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
