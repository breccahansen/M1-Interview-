package com.westlake.advisor.costbasis;

import java.math.BigDecimal;

public class InsufficientSharesException extends RuntimeException {

    public InsufficientSharesException(String symbol, BigDecimal requested, BigDecimal available) {
        super("Insufficient shares of " + symbol + ": requested " + requested + ", available " + available);
    }
}
