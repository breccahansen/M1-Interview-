package com.westlake.advisor.transfer;

public class TransferNotAllowedException extends RuntimeException {

    public TransferNotAllowedException(String message) {
        super(message);
    }
}
