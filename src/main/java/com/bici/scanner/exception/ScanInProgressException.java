package com.bici.scanner.exception;

public class ScanInProgressException extends ScannerException {
    public ScanInProgressException() {
        super("A scan is already in progress");
    }

    public ScanInProgressException(String message) {
        super(message);
    }
}

