package com.cloudsec.compliance.component;

import com.cloudsec.compliance.exception.InvalidInputException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class InputValidator {
    
    private static final Set<String> VALID_REGIONS = new HashSet<>(java.util.Arrays.asList(
        "us-east-1", "us-east-2", "us-west-1", "us-west-2",
        "eu-west-1", "eu-west-2", "eu-west-3", "eu-central-1", "eu-north-1",
        "ap-southeast-1", "ap-southeast-2", "ap-northeast-1", "ap-northeast-2",
        "ap-south-1", "ca-central-1", "sa-east-1"
    ));
    
    private static final String DEFAULT_REGION = "us-east-1";
    private static final int MAX_PAGE_SIZE = 100;
    
    public String validateAndSanitizeRegion(String region) {
        if (region == null || region.trim().isEmpty()) {
            return DEFAULT_REGION;
        }
        
        String sanitized = region.trim().toLowerCase().replaceAll("[^a-z0-9-]", "");
        
        if (!VALID_REGIONS.contains(sanitized)) {
            log.warn("Invalid region attempted: {}", region);
            throw new InvalidInputException("Invalid region specified: " + region);
        }
        
        return sanitized;
    }
    
    public int validatePageSize(Integer pageSize, int defaultSize) {
        if (pageSize == null) {
            return defaultSize;
        }
        
        if (pageSize < 1) {
            log.warn("Page size too small: {}, using 1", pageSize);
            return 1;
        }
        
        if (pageSize > MAX_PAGE_SIZE) {
            log.warn("Page size too large: {}, using max: {}", pageSize, MAX_PAGE_SIZE);
            return MAX_PAGE_SIZE;
        }
        
        return pageSize;
    }
    
    public String sanitizeBucketName(String bucketName) {
        if (bucketName == null) {
            return "unknown";
        }
        
        String lowerName = bucketName.toLowerCase();
        if (containsSensitivePatterns(lowerName)) {
            log.debug("Masking sensitive bucket name: {}", bucketName.substring(0, Math.min(3, bucketName.length())));
            return bucketName.substring(0, Math.min(3, bucketName.length())) + "***";
        }
        
        return bucketName;
    }
    
    private boolean containsSensitivePatterns(String bucketName) {
        return bucketName.contains("secret") || 
               bucketName.contains("private") || 
               bucketName.contains("internal") || 
               bucketName.contains("confidential");
    }
}