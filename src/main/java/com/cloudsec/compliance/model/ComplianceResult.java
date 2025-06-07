package com.cloudsec.compliance.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record ComplianceResult(
    @NotBlank(message = "Resource ID cannot be blank")
    String resourceId,
    
    @NotBlank(message = "Resource type cannot be blank")
    String resourceType,
    
    @NotNull(message = "Compliance standard cannot be null")
    ComplianceStandard standard,
    
    @NotNull(message = "Compliance status cannot be null")
    ComplianceStatus status,
    
    @NotNull(message = "Findings list cannot be null")
    @Valid
    List<Finding> findings,
    
    @NotBlank(message = "Cloud provider cannot be blank")
    String cloudProvider,
    
    @NotBlank(message = "Timestamp cannot be blank")
    String timestamp,
    
    String region
) {
    public ComplianceResult {
        if (findings == null) findings = List.of();
        if (timestamp == null) timestamp = LocalDateTime.now().toString();
        if (resourceId == null || resourceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource ID cannot be blank");
        }
        if (resourceType == null || resourceType.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource type cannot be blank");
        }
        if (cloudProvider == null || cloudProvider.trim().isEmpty()) {
            throw new IllegalArgumentException("Cloud provider cannot be blank");
        }
    }
    
    public ComplianceResult(String resourceId, String resourceType, ComplianceStandard standard,
                           ComplianceStatus status, List<Finding> findings, String cloudProvider, String region) {
        this(resourceId, resourceType, standard, status, findings, cloudProvider, 
             LocalDateTime.now().toString(), region);
    }
    
    public boolean isFullyCompliant() {
        return status == ComplianceStatus.COMPLIANT;
    }
    
    public long getCountBySeverity(Finding.Severity severity) {
        return findings.stream()
            .filter(finding -> finding.severity() == severity)
            .count();
    }
    
    public Finding.Severity getHighestSeverity() {
        return findings.stream()
            .map(Finding::severity)
            .max((s1, s2) -> Integer.compare(s1.getNumericValue(), s2.getNumericValue()))
            .orElse(Finding.Severity.INFO);
    }
}
