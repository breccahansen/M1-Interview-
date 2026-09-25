package com.westlake.advisor.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.slf4j.LoggerFactory;

class AdvisorNotifierTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(AdvisorNotifier.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attach() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
    }

    @Test
    void rendersTemplateWithAllPlaceholdersSubstituted() {
        new AdvisorNotifier().transferCompleted("ADV-114", "TRF-2026-ABCD1234", "AAPL", new BigDecimal("40"));

        assertEquals(1, appender.list.size());
        String message = appender.list.get(0).getFormattedMessage();
        assertEquals("Notify ADV-114: Transfer TRF-2026-ABCD1234 completed: 40 shares of AAPL moved. "
                + "Review in Advisor Center.", message);
    }

    @Test
    void quantityIsRenderedAsPlainStringNotScientific() {
        new AdvisorNotifier().transferCompleted("ADV-1", "TRF-1", "VTI", new BigDecimal("1E+3"));

        String message = appender.list.get(0).getFormattedMessage();
        assertTrue(message.contains("1000 shares of VTI"), message);
    }
}
