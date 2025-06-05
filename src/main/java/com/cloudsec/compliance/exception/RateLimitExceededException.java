package com.cloudsec.compliance.exception;

public class RateLimitExceededException extends ComplianceException {
    
    public RateLimitExceededException(String message) {
        super(message);
    }
}
