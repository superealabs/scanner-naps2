package com.bici.scanner.exception;

public class SaneNotAvailableException extends ScannerException {
    public SaneNotAvailableException(String message) {
        super("SANE is not available: " + message);
    }
}

