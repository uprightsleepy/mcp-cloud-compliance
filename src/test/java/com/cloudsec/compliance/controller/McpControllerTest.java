package com.cloudsec.compliance.controller;

import com.cloudsec.compliance.dto.response.HealthCheckResponse;
import com.cloudsec.compliance.service.HealthCheckService;
import com.cloudsec.compliance.service.S3ComplianceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

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

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PrintStream originalOut = System.out;
      System.setOut(new PrintStream(out));

      mcpController.handleRequest(requestJson);

      System.setOut(originalOut);
      String output = out.toString();

      assertThat(output).contains("protocolVersion");
      assertThat(output).contains("cloud-compliance-mcp");
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

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PrintStream originalOut = System.out;
      System.setOut(new PrintStream(out));

      mcpController.handleRequest(json);

      System.setOut(originalOut);
      assertThat(out.toString()).contains("Unknown method");
  }

  @Test
  @DisplayName("Should call healthCheck tool")
  void shouldHandleHealthCheckToolCall() throws Exception {
      HealthCheckResponse response = new HealthCheckResponse("ok", null, "test message", null);

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

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PrintStream originalOut = System.out;
      System.setOut(new PrintStream(out));

      mcpController.handleRequest(json);

      System.setOut(originalOut);
      String output = out.toString();
      assertThat(output).contains("status");
      assertThat(output).contains("ok");
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

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PrintStream originalOut = System.out;
      System.setOut(new PrintStream(out));

      mcpController.handleRequest(json);

      System.setOut(originalOut);
      String output = out.toString();

      assertThat(output).contains("health_check");
      assertThat(output).contains("list_s3_buckets");
  }
}
