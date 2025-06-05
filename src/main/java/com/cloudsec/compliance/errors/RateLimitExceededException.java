package com.cloudsec.compliance.errors;

public class RateLimitExceededException extends ComplianceException {
    
    public RateLimitExceededException(String message) {
        super(message);
    }
}
