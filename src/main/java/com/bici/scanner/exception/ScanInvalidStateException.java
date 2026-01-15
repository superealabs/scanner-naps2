package com.bici.scanner.exception;

public class ScanInvalidStateException extends ScannerException {
    public ScanInvalidStateException(String message) {
        super("Invalid state transition: " + message);
    }
}

