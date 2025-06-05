package com.cloudsec.compliance.service;

import com.cloudsec.compliance.dto.response.HealthCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class HealthCheckService {
    
    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);
    private static final String VERSION = "0.2.0";
    
    public HealthCheckResponse performHealthCheck(String message) {
        log.debug("Performing health check with message: {}", message);
        
        return new HealthCheckResponse(
            "OK",
            LocalDateTime.now().toString(),
            message != null ? "Echo: " + message : "MCP Cloud Compliance Server is running",
            VERSION
        );
    }
}