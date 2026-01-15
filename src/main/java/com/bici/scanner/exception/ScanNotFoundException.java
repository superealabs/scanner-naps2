package com.bici.scanner.exception;

public class ScanNotFoundException extends ScannerException {
    public ScanNotFoundException(String scanId) {
        super("Scan session not found: " + scanId);
    }
}

