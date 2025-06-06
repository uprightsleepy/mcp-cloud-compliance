package com.cloudsec.compliance.errors;

public abstract class ComplianceException extends RuntimeException {
    
    protected ComplianceException(String message) {
        super(message);
    }
    
    protected ComplianceException(String message, Throwable cause) {
        super(message, cause);
    }
}
