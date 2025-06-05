package com.cloudsec.compliance.dto.response;

import jakarta.validation.constraints.NotBlank;

public record S3BucketInfo(
    @NotBlank(message = "Bucket name cannot be blank")
    String name,
    
    @NotBlank(message = "Creation date cannot be blank")
    String creationDate,
    
    @NotBlank(message = "Region cannot be blank") 
    String region
) {
    public S3BucketInfo {
        if (name == null || name.trim().isEmpty()) name = "unknown";
        if (creationDate == null) creationDate = "unknown";
        if (region == null) region = "unknown";
    }
}
