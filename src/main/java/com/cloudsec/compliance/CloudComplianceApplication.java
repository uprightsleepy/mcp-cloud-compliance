package com.cloudsec.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

import java.time.LocalDateTime;
import java.util.*;
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
    
    private final Map<String, java.util.concurrent.atomic.AtomicLong> rateLimiters = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long RATE_LIMIT_WINDOW_MS = 60_000; // 1 minute
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    
    private static final Set<String> VALID_REGIONS = new HashSet<>(Arrays.asList(
        "us-east-1", "us-east-2", "us-west-1", "us-west-2",
        "eu-west-1", "eu-west-2", "eu-west-3", "eu-central-1", "eu-north-1",
        "ap-southeast-1", "ap-southeast-2", "ap-northeast-1", "ap-northeast-2",
        "ap-south-1", "ca-central-1", "sa-east-1"
    ));
    
    private static final int MAX_BUCKETS_RETURNED = 100;
    
    public S3BucketListResult listBuckets(String region) {
        try {
            String validatedRegion = validateAndSanitizeRegion(region);
            
            if (!checkRateLimit("listBuckets")) {
                return createErrorResult("Rate limit exceeded. Please try again later.", validatedRegion);
            }
            
            Region awsRegion = Region.of(validatedRegion);
            
            S3Client s3Client = S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(builder -> builder
                    .retryPolicy(retryPolicy -> retryPolicy.numRetries(2))
                )
                .build();
            
            ListBucketsResponse response = s3Client.listBuckets();
            
            List<S3BucketInfo> buckets = response.buckets().stream()
                .limit(MAX_BUCKETS_RETURNED)
                .map(bucket -> new S3BucketInfo(
                    sanitizeBucketName(bucket.name()),
                    bucket.creationDate() != null ? bucket.creationDate().toString() : "unknown",
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
            
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            return handleS3Exception(e, region);
        } catch (software.amazon.awssdk.core.exception.SdkException e) {
            return createErrorResult("AWS service unavailable", region);
        } catch (SecurityException | IllegalArgumentException e) {
            return createErrorResult("Invalid request: " + e.getMessage(), region);
        } catch (Exception e) {
            System.err.println("Unexpected error in listBuckets: " + e.getClass().getSimpleName());
            return createErrorResult("Service temporarily unavailable", region);
        }
    }
    
    private String validateAndSanitizeRegion(String region) {
        if (region == null || region.trim().isEmpty()) {
            return "us-east-1";
        }
        
        String sanitized = region.trim().toLowerCase().replaceAll("[^a-z0-9-]", "");
        
        if (!VALID_REGIONS.contains(sanitized)) {
            throw new IllegalArgumentException("Invalid region specified");
        }
        
        return sanitized;
    }
    
    private boolean checkRateLimit(String operation) {
        String key = operation + "_" + (System.currentTimeMillis() / RATE_LIMIT_WINDOW_MS);
        java.util.concurrent.atomic.AtomicLong counter = rateLimiters.computeIfAbsent(key, k -> new java.util.concurrent.atomic.AtomicLong(0));
        
        // Clean up old entries
        rateLimiters.entrySet().removeIf(entry -> 
            !entry.getKey().endsWith("_" + (System.currentTimeMillis() / RATE_LIMIT_WINDOW_MS)));
        
        return counter.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
    }
    
    private String sanitizeBucketName(String bucketName) {
        if (bucketName == null) return "unknown";
        
        String lowerName = bucketName.toLowerCase();
        if (lowerName.contains("secret") || lowerName.contains("private") || 
            lowerName.contains("internal") || lowerName.contains("confidential")) {
            return bucketName.substring(0, Math.min(3, bucketName.length())) + "***";
        }
        
        return bucketName;
    }
    
    private S3BucketListResult handleS3Exception(software.amazon.awssdk.services.s3.model.S3Exception e, String region) {
        String sanitizedRegion = region != null ? region : "unknown";
        
        switch (e.statusCode()) {
            case 403:
                return createErrorResult("Access denied. Please check AWS permissions.", sanitizedRegion);
            case 404:
                return createErrorResult("Resource not found", sanitizedRegion);
            case 429:
                return createErrorResult("Rate limit exceeded by AWS", sanitizedRegion);
            case 500:
            case 502:
            case 503:
                return createErrorResult("AWS service temporarily unavailable", sanitizedRegion);
            default:
                return createErrorResult("Request failed", sanitizedRegion);
        }
    }
    
    /**
     * Create standardized error result
     */
    private S3BucketListResult createErrorResult(String message, String region) {
        return new S3BucketListResult(
            "ERROR",
            0,
            List.of(),
            LocalDateTime.now().toString(),
            region != null ? region : "unknown",
            message
        );
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
    
    public S3BucketListResult {
        if (buckets == null) buckets = List.of();
        if (bucketCount < 0) bucketCount = 0;
        if (status == null) status = "UNKNOWN";
        if (timestamp == null) timestamp = LocalDateTime.now().toString();
        if (region == null) region = "unknown";
    }
}

record S3BucketInfo(
    String name,
    String creationDate,
    String region
) {
    public S3BucketInfo {
        if (name == null || name.trim().isEmpty()) name = "unknown";
        if (creationDate == null) creationDate = "unknown";
        if (region == null) region = "unknown";
    }
}