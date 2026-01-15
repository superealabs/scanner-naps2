package com.bici.scanner.exception;

public class Naps2NotAvailableException extends ScannerException {
    public Naps2NotAvailableException(String message) {
        super("NAPS2 is not available: " + message);
    }
}

