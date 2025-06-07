package com.cloudsec.compliance.controller;

import com.cloudsec.compliance.service.HealthCheckService;
import com.cloudsec.compliance.service.S3ComplianceService;
import com.cloudsec.compliance.model.ComplianceStandard;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpController implements CommandLineRunner {
    
    private final ObjectMapper objectMapper;
    private final HealthCheckService healthCheckService;
    private final S3ComplianceService s3ComplianceService;
    
    @Override
    public void run(String... args) {
        log.info("Starting MCP Cloud Compliance Server...");
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().isEmpty()) continue;
                
                try {
                    handleRequest(line);
                } catch (Exception e) {
                    log.error("Error handling MCP request", e);
                }
            }
        }
    }
    
    void handleRequest(String jsonRequest) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> request = objectMapper.readValue(jsonRequest, Map.class);
        
        String method = (String) request.get("method");
        Object id = request.get("id");
        
        switch (method) {
            case "initialize" -> sendInitializeResponse(id);
            case "tools/list" -> sendToolsList(id);
            case "tools/call" -> handleToolCall(request, id);
            default -> sendError(id, "Unknown method: " + method);
        }
    }
    
    private void sendInitializeResponse(Object id) throws Exception {
        Map<String, Object> response = Map.of(
            "jsonrpc", "2.0",
            "id", id,
            "result", Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of("tools", Map.of()),
                "serverInfo", Map.of(
                    "name", "cloud-compliance-mcp",
                    "version", "0.2.0"
                )
            )
        );
        
        System.out.println(objectMapper.writeValueAsString(response));
    }
    
    private void sendToolsList(Object id) throws Exception {
        Map<String, Object> response = Map.of(
            "jsonrpc", "2.0",
            "id", id,
            "result", Map.of(
                "tools", List.of(
                    createHealthCheckTool(),
                    createS3ListTool(),
                    createComplianceCheckTool(),
                    createSupportedStandardsTool(),
                    createSupportedResourceTypesTool()
                )
            )
        );
        
        System.out.println(objectMapper.writeValueAsString(response));
    }
    
    private Map<String, Object> createHealthCheckTool() {
        return Map.of(
            "name", "health_check",
            "description", "Check if the MCP server is running properly",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "message", Map.of(
                        "type", "string",
                        "description", "Optional message to echo back"
                    )
                )
            )
        );
    }
    
    private Map<String, Object> createS3ListTool() {
        return Map.of(
            "name", "list_s3_buckets",
            "description", "List S3 buckets in the AWS account with pagination support",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "region", Map.of(
                        "type", "string",
                        "description", "AWS region (optional, defaults to us-east-1)"
                    ),
                    "pageSize", Map.of(
                        "type", "integer",
                        "description", "Number of buckets per page (1-100, default 20)",
                        "minimum", 1,
                        "maximum", 100
                    ),
                    "pageToken", Map.of(
                        "type", "string",
                        "description", "Token for next page (from previous response)"
                    )
                )
            )
        );
    }
    
    private Map<String, Object> createComplianceCheckTool() {
        return Map.of(
            "name", "check_resource_compliance",
            "description", "Check compliance for a specific resource type against a compliance standard",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "resourceType", Map.of(
                        "type", "string",
                        "description", "Type of cloud resource (e.g., 'storage', 'compute', 'database')",
                        "enum", List.of("storage", "compute", "database", "network")
                    ),
                    "standard", Map.of(
                        "type", "string",
                        "description", "Compliance standard to check against",
                        "enum", List.of("SOC2", "CIS", "NIST", "ISO27001", "PCI_DSS")
                    )
                ),
                "required", List.of("resourceType", "standard")
            )
        );
    }
    
    private Map<String, Object> createSupportedStandardsTool() {
        return Map.of(
            "name", "list_supported_standards",
            "description", "Get list of compliance standards supported by this cloud provider",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of()
            )
        );
    }
    
    private Map<String, Object> createSupportedResourceTypesTool() {
        return Map.of(
            "name", "list_supported_resource_types",
            "description", "Get list of resource types that can be checked for compliance",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of()
            )
        );
    }
    
    @SuppressWarnings("unchecked")
    private void handleToolCall(Map<String, Object> request, Object id) throws Exception {
        try {
            Map<String, Object> params = (Map<String, Object>) request.get("params");
            String toolName = (String) params.get("name");
            Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
            
            Object result = switch (toolName) {
                case "health_check" -> {
                    String message = arguments != null ? (String) arguments.get("message") : null;
                    yield healthCheckService.performHealthCheck(message);
                }
                case "list_s3_buckets" -> {
                    String region = arguments != null ? (String) arguments.get("region") : null;
                    Integer pageSize = arguments != null ? (Integer) arguments.get("pageSize") : null;
                    String pageToken = arguments != null ? (String) arguments.get("pageToken") : null;
                    yield s3ComplianceService.listBuckets(region, pageSize, pageToken);
                }
                case "check_resource_compliance" -> {
                    if (arguments == null) {
                        throw new IllegalArgumentException("Missing required arguments for check_resource_compliance");
                    }
                    String resourceType = (String) arguments.get("resourceType");
                    String standardStr = (String) arguments.get("standard");
                    
                    if (resourceType == null || standardStr == null) {
                        throw new IllegalArgumentException("Both resourceType and standard are required");
                    }
                    
                    try {
                        ComplianceStandard standard = ComplianceStandard.valueOf(standardStr);
                        yield s3ComplianceService.checkCompliance(resourceType, standard);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid compliance standard: " + standardStr);
                    }
                }
                case "list_supported_standards" -> {
                    yield s3ComplianceService.getSupportedStandards().stream()
                        .map(standard -> Map.of(
                            "name", standard.name(),
                            "shortName", standard.getShortName(),
                            "fullName", standard.getFullName()
                        ))
                        .toList();
                }
                case "list_supported_resource_types" -> {
                    yield s3ComplianceService.getSupportedResourceTypes().stream()
                        .map(resourceType -> Map.of(
                            "type", resourceType,
                            "description", getResourceTypeDescription(resourceType)
                        ))
                        .toList();
                }
                default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
            };
            
            Map<String, Object> response = Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "result", Map.of(
                    "content", List.of(
                        Map.of(
                            "type", "text",
                            "text", objectMapper.writeValueAsString(result)
                        )
                    )
                )
            );
            
            System.out.println(objectMapper.writeValueAsString(response));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid tool call: {}", e.getMessage());
            sendError(id, e.getMessage());
        } catch (Exception e) {
            log.error("Error handling tool call", e);
            sendError(id, "Internal server error");
        }
    }
    
    private String getResourceTypeDescription(String resourceType) {
        return switch (resourceType) {
            case "storage" -> "Cloud storage services (S3, Azure Blob, GCS)";
            case "compute" -> "Virtual machines and compute instances";
            case "database" -> "Managed database services";
            case "network" -> "Network security and configuration";
            default -> "Cloud resource";
        };
    }
    
    private void sendError(Object id, String message) throws Exception {
        Map<String, Object> response = Map.of(
            "jsonrpc", "2.0",
            "id", id,
            "error", Map.of(
                "code", -1,
                "message", message
            )
        );
        
        System.out.println(objectMapper.writeValueAsString(response));
    }
}