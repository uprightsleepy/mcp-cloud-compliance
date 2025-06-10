package com.cloudsec.compliance.model;

public enum ComplianceStandard {
    SOC2("SOC 2", "Service Organization Control 2"),
    CIS("CIS", "Center for Internet Security Benchmarks"),
    NIST("NIST", "NIST Cybersecurity Framework"),
    ISO27001("ISO 27001", "ISO/IEC 27001 Information Security Management"),
    PCI_DSS("PCI DSS", "Payment Card Industry Data Security Standard");
    
    private final String shortName;
    private final String fullName;
    
    ComplianceStandard(String shortName, String fullName) {
        this.shortName = shortName;
        this.fullName = fullName;
    }
    
    public String getShortName() {
        return shortName;
    }
    
    public String getFullName() {
        return fullName;
    }
}