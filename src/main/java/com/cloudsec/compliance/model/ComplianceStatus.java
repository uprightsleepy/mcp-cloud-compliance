package com.cloudsec.compliance.model;

public enum ComplianceStatus {
    COMPLIANT("Compliant", "Resource meets all compliance requirements"),
    NON_COMPLIANT("Non-Compliant", "Resource has compliance violations"),
    PARTIAL_COMPLIANT("Partially Compliant", "Resource meets some but not all requirements"),
    NOT_APPLICABLE("Not Applicable", "Compliance check does not apply to this resource"),
    ERROR("Error", "Unable to determine compliance status due to error");
    
    private final String displayName;
    private final String description;
    
    ComplianceStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}