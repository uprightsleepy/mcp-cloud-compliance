package com.cloudsec.compliance.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Finding(
    @NotBlank(message = "Finding ID cannot be blank")
    String id,
    
    @NotNull(message = "Severity cannot be null")
    Severity severity,
    
    @NotBlank(message = "Control ID cannot be blank")
    String controlId,
    
    @NotBlank(message = "Description cannot be blank")
    String description,
    
    String remediation,
    
    String evidence
) {
    public Finding {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Finding ID cannot be blank");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
    }
    
    public enum Severity {
        CRITICAL("Critical", 4),
        HIGH("High", 3),
        MEDIUM("Medium", 2),
        LOW("Low", 1),
        INFO("Information", 0);
        
        private final String displayName;
        private final int numericValue;
        
        Severity(String displayName, int numericValue) {
            this.displayName = displayName;
            this.numericValue = numericValue;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getNumericValue() {
            return numericValue;
        }
    }
}