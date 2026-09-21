package com.westlake.advisor.notification;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sends advisor-facing notifications. In production this publishes to the
 * AdvisorInbox MQ topic; here it logs the rendered message.
 */
@Component
public class AdvisorNotifier {

    private static final Logger log = LoggerFactory.getLogger(AdvisorNotifier.class);

    private static final String TRANSFER_TEMPLATE =
            "Transfer ${transferId} completed: ${quantity} shares of ${symbol} moved. Review in Advisor Center.";

    public void transferCompleted(String advisorId, String transferId, String symbol, BigDecimal quantity) {
        Map<String, String> values = new HashMap<>();
        values.put("transferId", transferId);
        values.put("symbol", symbol);
        values.put("quantity", quantity.toPlainString());
        String message = new StringSubstitutor(values).replace(TRANSFER_TEMPLATE);
        log.info("Notify {}: {}", advisorId, message);
    }
}
