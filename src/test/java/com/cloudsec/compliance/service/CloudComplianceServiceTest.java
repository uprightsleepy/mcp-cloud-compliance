package com.cloudsec.compliance.service;

import com.cloudsec.compliance.model.ComplianceResult;
import com.cloudsec.compliance.model.ComplianceStandard;
import com.cloudsec.compliance.model.ComplianceStatus;
import com.cloudsec.compliance.model.Finding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CloudComplianceServiceTest {

    private CloudComplianceService cloudComplianceService;

    static class TestCloudComplianceService implements CloudComplianceService {
        
        @Override
        public ComplianceResult checkCompliance(String resourceType, ComplianceStandard standard) {
            ComplianceStatus status = "storage".equals(resourceType) ? 
                ComplianceStatus.COMPLIANT : ComplianceStatus.NON_COMPLIANT;
                
            List<Finding> findings = status == ComplianceStatus.NON_COMPLIANT ? 
                List.of(new Finding(
                    "F001", 
                    Finding.Severity.MEDIUM, 
                    "CC6.1", 
                    "Test compliance violation", 
                    "Fix the issue",
                    "Evidence of violation"
                )) : List.of();
                
            return new ComplianceResult(
                "test-resource-123",
                resourceType,
                standard,
                status,
                findings,
                "TestCloud",
                "test-region"
            );
        }

        @Override
        public List<String> getSupportedResourceTypes() {
            return List.of("storage", "compute", "database", "network");
        }

        @Override
        public String getCloudProvider() {
            return "TestCloud";
        }

        @Override
        public List<ComplianceStandard> getSupportedStandards() {
            return List.of(
                ComplianceStandard.SOC2, 
                ComplianceStandard.CIS, 
                ComplianceStandard.NIST
            );
        }
    }

    @BeforeEach
    void setUp() {
        cloudComplianceService = new TestCloudComplianceService();
    }

    @Test
    void checkCompliance_WithStorageResource_ReturnsCompliantResult() {
        ComplianceResult result = cloudComplianceService.checkCompliance("storage", ComplianceStandard.SOC2);

        assertThat(result).isNotNull();
        assertThat(result.resourceType()).isEqualTo("storage");
        assertThat(result.standard()).isEqualTo(ComplianceStandard.SOC2);
        assertThat(result.status()).isEqualTo(ComplianceStatus.COMPLIANT);
        assertThat(result.cloudProvider()).isEqualTo("TestCloud");
        assertThat(result.region()).isEqualTo("test-region");
        assertThat(result.findings()).isEmpty();
        assertThat(result.isFullyCompliant()).isTrue();
    }

    @Test
    void checkCompliance_WithComputeResource_ReturnsNonCompliantResult() {
        ComplianceResult result = cloudComplianceService.checkCompliance("compute", ComplianceStandard.CIS);

        assertThat(result).isNotNull();
        assertThat(result.resourceType()).isEqualTo("compute");
        assertThat(result.standard()).isEqualTo(ComplianceStandard.CIS);
        assertThat(result.status()).isEqualTo(ComplianceStatus.NON_COMPLIANT);
        assertThat(result.cloudProvider()).isEqualTo("TestCloud");
        assertThat(result.findings()).hasSize(1);
        assertThat(result.isFullyCompliant()).isFalse();
        
        Finding finding = result.findings().get(0);
        assertThat(finding.severity()).isEqualTo(Finding.Severity.MEDIUM);
        assertThat(finding.controlId()).isEqualTo("CC6.1");
    }

    @Test
    void checkCompliance_WithDifferentStandards_ReturnsCorrectStandard() {
        ComplianceResult soc2Result = cloudComplianceService.checkCompliance("database", ComplianceStandard.SOC2);
        ComplianceResult nisResult = cloudComplianceService.checkCompliance("database", ComplianceStandard.NIST);

        assertThat(soc2Result.standard()).isEqualTo(ComplianceStandard.SOC2);
        assertThat(nisResult.standard()).isEqualTo(ComplianceStandard.NIST);
    }

    @Test
    void getSupportedResourceTypes_ReturnsExpectedTypes() {
        List<String> resourceTypes = cloudComplianceService.getSupportedResourceTypes();

        assertThat(resourceTypes).isNotNull();
        assertThat(resourceTypes).containsExactly("storage", "compute", "database", "network");
        assertThat(resourceTypes).hasSize(4);
    }

    @Test
    void getCloudProvider_ReturnsCorrectProvider() {
        String provider = cloudComplianceService.getCloudProvider();

        assertThat(provider).isNotNull();
        assertThat(provider).isEqualTo("TestCloud");
        assertThat(provider).isNotBlank();
    }

    @Test
    void getSupportedStandards_ReturnsExpectedStandards() {
        List<ComplianceStandard> standards = cloudComplianceService.getSupportedStandards();

        assertThat(standards).isNotNull();
        assertThat(standards).containsExactly(
            ComplianceStandard.SOC2, 
            ComplianceStandard.CIS, 
            ComplianceStandard.NIST
        );
        assertThat(standards).hasSize(3);
    }

    @Test
    void checkCompliance_ResultContainsValidTimestamp() {
        ComplianceResult result = cloudComplianceService.checkCompliance("storage", ComplianceStandard.SOC2);

        assertThat(result.timestamp()).isNotNull();
        assertThat(result.timestamp()).isNotBlank();
        assertThat(result.timestamp()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*");
    }

    @Test
    void checkCompliance_ResultContainsValidResourceId() {
        ComplianceResult result = cloudComplianceService.checkCompliance("network", ComplianceStandard.SOC2);

        assertThat(result.resourceId()).isNotNull();
        assertThat(result.resourceId()).isNotBlank();
        assertThat(result.resourceId()).isEqualTo("test-resource-123");
    }
}
