package com.cloudsec.compliance.dto.response;

import jakarta.validation.constraints.NotBlank;

public record HealthCheckResponse(
    @NotBlank(message = "Status cannot be blank")
    String status,
    
    @NotBlank(message = "Timestamp cannot be blank") 
    String timestamp,
    
    @NotBlank(message = "Message cannot be blank")
    String message,
    
    @NotBlank(message = "Version cannot be blank")
    String version
) {}