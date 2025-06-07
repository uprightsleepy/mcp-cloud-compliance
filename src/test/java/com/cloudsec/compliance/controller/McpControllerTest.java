package com.cloudsec.compliance.controller;

import com.cloudsec.compliance.dto.response.HealthCheckResponse;
import com.cloudsec.compliance.service.HealthCheckService;
import com.cloudsec.compliance.service.S3ComplianceService;
import com.cloudsec.compliance.model.ComplianceResult;
import com.cloudsec.compliance.model.ComplianceStandard;
import com.cloudsec.compliance.model.ComplianceStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpController Tests")
class McpControllerTest {
    
    @Mock
    private HealthCheckService healthCheckService;

    @Mock
    private S3ComplianceService s3ComplianceService;

    private ObjectMapper objectMapper;
    private McpController mcpController;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        mcpController = new McpController(objectMapper, healthCheckService, s3ComplianceService);
    }

    private String captureOutput(Runnable action) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));
        
        try {
            action.run();
            return out.toString();
        } finally {
            System.setOut(originalOut);
        }
    }

    @Nested
    @DisplayName("Basic MCP Protocol Tests")
    class BasicMcpProtocolTests {

        @Test
        @DisplayName("Should handle initialize request")
        void shouldHandleInitializeRequest() throws Exception {
            String requestJson = """
                {
                  "jsonrpc": "2.0",
                  "method": "initialize",
                  "id": 1
                }
            """;

            String output = captureOutput(() -> {
                try {
                    mcpController.handleRequest(requestJson);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(output).contains("protocolVersion");
            assertThat(output).contains("cloud-compliance-mcp");
            assertThat(output).contains("\"version\":\"0.2.0\"");
        }

        @Test
        @DisplayName("Should return error on unknown method")
        void shouldReturnErrorOnUnknownMethod() throws Exception {
            String json = """
                {
                  "jsonrpc": "2.0",
                  "method": "nonexistent",
                  "id": 99
                }
            """;

            String output = captureOutput(() -> {
                try {
                    mcpController.handleRequest(json);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(output).contains("Unknown method");
            assertThat(output).contains("\"error\":");
        }

        @Test
        @DisplayName("Should return tools list for tools/list method")
        void shouldReturnToolsList() throws Exception {
            String json = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/list",
                  "id": 2
                }
            """;

            String output = captureOutput(() -> {
                try {
                    mcpController.handleRequest(json);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(output).contains("health_check");
            assertThat(output).contains("list_s3_buckets");
            assertThat(output).contains("check_resource_compliance");
            assertThat(output).contains("list_supported_standards");
            assertThat(output).contains("list_supported_resource_types");
        }
    }

    @Nested
    @DisplayName("Existing Tool Tests")
    class ExistingToolTests {

        @Test
        @DisplayName("Should call healthCheck tool")
        void shouldHandleHealthCheckToolCall() throws Exception {
            HealthCheckResponse response = new HealthCheckResponse("ok", "2024-01-01T12:00:00", "test message", "0.2.0");

            when(healthCheckService.performHealthCheck("hello"))
                .thenReturn(response);

            String json = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/call",
                  "id": 42,
                  "params": {
                    "name": "health_check",
                    "arguments": {
                      "message": "hello"
                    }
                  }
                }
            """;

            String output = captureOutput(() -> {
                try {
                    mcpController.handleRequest(json);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(output).contains("status");
            assertThat(output).contains("ok");
            assertThat(output).contains("test message");
        }

        @Test
        @DisplayName("Should handle health check without message")
        void shouldHandleHealthCheckWithoutMessage() throws Exception {
            HealthCheckResponse response = new HealthCheckResponse("OK", "2024-01-01T12:00:00", "MCP Cloud Compliance Server is running", "0.2.0");

            when(healthCheckService.performHealthCheck(null))
                .thenReturn(response);

            String json = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/call",
                  "id": 43,
                  "params": {
                    "name": "health_check",
                    "arguments": {}
                  }
                }
            """;

            String output = captureOutput(() -> {
                try {
                    mcpController.handleRequest(json);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(output).contains("\\\"status\\\":\\\"OK\\\"");
            verify(healthCheckService).performHealthCheck(null);
        }
    }

    @Nested
    @DisplayName("New Compliance Tool Tests")
    class NewComplianceToolTests {

        @Test
        @DisplayName("Should handle compliance check tool call")
        void shouldHandleComplianceCheckToolCall() throws Exception {
            ComplianceResult mockResult = new ComplianceResult(
                "test-resource",
                "storage",
                ComplianceStandard.SOC2,
                ComplianceStatus.COMPLIANT,
                List.of(),
                "AWS",
                "us-east-1"
            );
            when(s3ComplianceService.checkCompliance("storage", ComplianceStandard.SOC2))
                .thenReturn(mockResult);

            String json = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/call",
                  "id": 5,
                  "params": {
                    "name": "check_resource_compliance",
                    "arguments": {
                      "resourceType": "storage",
                      "standard": "SOC2"
                    }
                  }
                }
            """;

            String output = captureOutput(() -> {
                try {
                    mcpController.handleRequest(json);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(output).contains("\\\"resourceType\\\":\\\"storage\\\"");
            assertThat(output).contains("\\\"status\\\":\\\"COMPLIANT\\\"");
            assertThat(output).contains("\\\"cloudProvider\\\":\\\"AWS\\\"");
            verify(s3ComplianceService).checkCompliance("storage", ComplianceStandard.SOC2);
        }

        @Test
        @DisplayName("Should handle supported standards tool call")
        void shouldHandleSupportedStandardsToolCall() throws Exception {
            when(s3ComplianceService.getSupportedStandards())
                .thenReturn(List.of(ComplianceStandard.SOC2, ComplianceStandard.CIS));

            String json = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/call",
                  "id": 6,
                  "params": {
                    "name": "list_supported_standards",
                    "arguments": {}
                  }
                }
            """;

            String output = captureOutput(() -> {
                try {
                    mcpController.handleRequest(json);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(output).contains("\\\"name\\\":\\\"SOC2\\\"");
            assertThat(output).contains("\\\"shortName\\\":\\\"SOC 2\\\"");
            assertThat(output).contains("\\\"name\\\":\\\"CIS\\\"");
            verify(s3ComplianceService).getSupportedStandards();
        }

        @Test
        @DisplayName("Should handle supported resource types tool call")
        void shouldHandleSupportedResourceTypesToolCall() throws Exception {
            when(s3ComplianceService.getSupportedResourceTypes())
                .thenReturn(List.of("storage"));

            String json = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/call",
                  "id": 7,
                  "params": {
                    "name": "list_supported_resource_types",
                    "arguments": {}
                  }
                }
            """;

            String output = captureOutput(() -> {
                try {
                    mcpController.handleRequest(json);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(output).contains("\\\"type\\\":\\\"storage\\\"");
            assertThat(output).contains("Cloud storage services");
            verify(s3ComplianceService).getSupportedResourceTypes();
        }

        @Test
        @DisplayName("Should handle missing arguments for compliance check")
        void shouldHandleMissingArgumentsForComplianceCheck() throws Exception {
            String json = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/call",
                  "id": 8,
                  "params": {
                    "name": "check_resource_compliance",
                    "arguments": {}
                  }
                }
            """;

            String output = captureOutput(() -> {
                try {
                    mcpController.handleRequest(json);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(output).contains("\"error\":");
            assertThat(output).contains("Both resourceType and standard are required");
        }

        @Test
        @DisplayName("Should handle invalid compliance standard")
        void shouldHandleInvalidComplianceStandard() throws Exception {
            String json = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/call",
                  "id": 9,
                  "params": {
                    "name": "check_resource_compliance",
                    "arguments": {
                      "resourceType": "storage",
                      "standard": "INVALID_STANDARD"
                    }
                  }
                }
            """;

            String output = captureOutput(() -> {
                try {
                    mcpController.handleRequest(json);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(output).contains("\"error\":");
            assertThat(output).contains("Invalid compliance standard: INVALID_STANDARD");
        }

        @Test
        @DisplayName("Should handle missing arguments object")
        void shouldHandleMissingArgumentsObject() throws Exception {
            String json = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/call",
                  "id": 10,
                  "params": {
                    "name": "check_resource_compliance"
                  }
                }
            """;

            String output = captureOutput(() -> {
                try {
                    mcpController.handleRequest(json);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(output).contains("\"error\":");
            assertThat(output).contains("Missing required arguments");
        }

        @Test
        @DisplayName("Should handle unknown tool name")
        void shouldHandleUnknownToolName() throws Exception {
            String json = """
                {
                  "jsonrpc": "2.0",
                  "method": "tools/call",
                  "id": 11,
                  "params": {
                    "name": "unknown_tool",
                    "arguments": {}
                  }
                }
            """;

            String output = captureOutput(() -> {
                try {
                    mcpController.handleRequest(json);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(output).contains("\"error\":");
            assertThat(output).contains("Unknown tool: unknown_tool");
        }
    }
}
