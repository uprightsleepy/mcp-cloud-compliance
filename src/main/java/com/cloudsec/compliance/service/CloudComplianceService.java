package com.cloudsec.compliance.service;

import com.cloudsec.compliance.model.ComplianceResult;
import com.cloudsec.compliance.model.ComplianceStandard;

import java.util.List;

/**
 * Cloud-agnostic interface for compliance checking across different cloud providers.
 * Implementations should handle provider-specific logic while maintaining consistent behavior.
 */
public interface CloudComplianceService {
    
    ComplianceResult checkCompliance(String resourceType, ComplianceStandard standard);
    
    List<String> getSupportedResourceTypes();
    
    String getCloudProvider();

    List<ComplianceStandard> getSupportedStandards();
}
