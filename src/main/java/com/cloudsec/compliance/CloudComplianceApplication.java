package com.cloudsec.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@SpringBootApplication
public class CloudComplianceApplication {
    public static void main(String[] args) {
        McpServer server = new McpServer();
        server.start();
    }
}

class McpServer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HealthCheckService healthService = new HealthCheckService();
    
    public void start() {
        System.err.println("Starting MCP Cloud Compliance Server...");
        
        Scanner scanner = new Scanner(System.in);
        
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) continue;
            
            try {
                handleRequest(line);
            } catch (Exception e) {
                System.err.println("Error handling request: " + e.getMessage());
            }
        }
    }
    
    private void handleRequest(String jsonRequest) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> request = objectMapper.readValue(jsonRequest, Map.class);
        
        String method = (String) request.get("method");
        Object id = request.get("id");
        
        if ("initialize".equals(method)) {
            sendInitializeResponse(id);
        } else if ("tools/list".equals(method)) {
            sendToolsList(id);
        } else if ("tools/call".equals(method)) {
            handleToolCall(request, id);
        } else {
            sendError(id, "Unknown method: " + method);
        }
    }
    
    private void sendInitializeResponse(Object id) throws Exception {
        Map<String, Object> response = Map.of(
            "jsonrpc", "2.0",
            "id", id,
            "result", Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(
                    "tools", Map.of()
                ),
                "serverInfo", Map.of(
                    "name", "cloud-compliance-mcp",
                    "version", "0.1.0"
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
                    Map.of(
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
                    )
                )
            )
        );
        
        System.out.println(objectMapper.writeValueAsString(response));
    }
    
    @SuppressWarnings("unchecked")
    private void handleToolCall(Map<String, Object> request, Object id) throws Exception {
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        if ("health_check".equals(toolName)) {
            String message = arguments != null ? (String) arguments.get("message") : null;
            HealthCheckResult result = healthService.healthCheck(message);
            
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
        } else {
            sendError(id, "Unknown tool: " + toolName);
        }
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

@Service
class HealthCheckService {
    
    public HealthCheckResult healthCheck(String message) {
        return new HealthCheckResult(
            "OK",
            LocalDateTime.now().toString(),
            message != null ? "Echo: " + message : "MCP Cloud Compliance Server is running",
            "0.1.0"
        );
    }
}

record HealthCheckResult(
    String status,
    String timestamp,
    String message,
    String version
) {}