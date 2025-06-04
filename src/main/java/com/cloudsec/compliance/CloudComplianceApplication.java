package com.cloudsec.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

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
    private final S3Service s3Service = new S3Service();
    
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
                    ),
                    Map.of(
                        "name", "list_s3_buckets",
                        "description", "List all S3 buckets in the AWS account",
                        "inputSchema", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "region", Map.of(
                                    "type", "string",
                                    "description", "AWS region (optional, defaults to us-east-1)"
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
        
        Object result;
        
        if ("health_check".equals(toolName)) {
            String message = arguments != null ? (String) arguments.get("message") : null;
            result = healthService.healthCheck(message);
        } else if ("list_s3_buckets".equals(toolName)) {
            String region = arguments != null ? (String) arguments.get("region") : null;
            result = s3Service.listBuckets(region);
        } else {
            sendError(id, "Unknown tool: " + toolName);
            return;
        }
        
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

@Service 
class S3Service {
    
    public S3BucketListResult listBuckets(String region) {
        try {
            Region awsRegion = region != null ? Region.of(region) : Region.US_EAST_1;
            
            S3Client s3Client = S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
            
            ListBucketsResponse response = s3Client.listBuckets();
            
            List<S3BucketInfo> buckets = response.buckets().stream()
                .map(bucket -> new S3BucketInfo(
                    bucket.name(),
                    bucket.creationDate().toString(),
                    awsRegion.toString()
                ))
                .collect(Collectors.toList());
            
            return new S3BucketListResult(
                "SUCCESS",
                buckets.size(),
                buckets,
                LocalDateTime.now().toString(),
                awsRegion.toString()
            );
            
        } catch (Exception e) {
            return new S3BucketListResult(
                "ERROR",
                0,
                List.of(),
                LocalDateTime.now().toString(),
                region != null ? region : "us-east-1",
                "Failed to list buckets: " + e.getMessage()
            );
        }
    }
}

record HealthCheckResult(
    String status,
    String timestamp,
    String message,
    String version
) {}

record S3BucketListResult(
    String status,
    int bucketCount,
    List<S3BucketInfo> buckets,
    String timestamp,
    String region,
    String error
) {
    public S3BucketListResult(String status, int bucketCount, List<S3BucketInfo> buckets, 
                             String timestamp, String region) {
        this(status, bucketCount, buckets, timestamp, region, null);
    }
}

record S3BucketInfo(
    String name,
    String creationDate,
    String region
) {}