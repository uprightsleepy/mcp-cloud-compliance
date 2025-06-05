package com.cloudsec.compliance.service;

import com.cloudsec.compliance.dto.response.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthCheckService Tests")
class HealthCheckServiceTest {

    private HealthCheckService healthCheckService;

    @BeforeEach
    void setUp() {
        healthCheckService = new HealthCheckService();
    }

    @Test
    @DisplayName("Should return OK status with default message when no message provided")
    void shouldReturnOkStatusWithDefaultMessage() {
        HealthCheckResponse result = healthCheckService.performHealthCheck(null);
        
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("MCP Cloud Compliance Server is running");
        assertThat(result.version()).isEqualTo("0.2.0");
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should echo back provided message")
    void shouldEchoBackProvidedMessage() {
        String inputMessage = "test message";
        
        HealthCheckResponse result = healthCheckService.performHealthCheck(inputMessage);
        
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("Echo: test message");
        assertThat(result.version()).isEqualTo("0.2.0");
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty string message")
    void shouldHandleEmptyStringMessage() {
        HealthCheckResponse result = healthCheckService.performHealthCheck("");
        
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("Echo: ");
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle whitespace message")
    void shouldHandleWhitespaceMessage() {
        HealthCheckResponse result = healthCheckService.performHealthCheck("   ");
        
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("Echo:    ");
    }

    @Test
    @DisplayName("Should always return consistent version")
    void shouldAlwaysReturnConsistentVersion() {
        HealthCheckResponse result1 = healthCheckService.performHealthCheck("message1");
        HealthCheckResponse result2 = healthCheckService.performHealthCheck("message2");
        
        assertThat(result1.version()).isEqualTo(result2.version());
        assertThat(result1.version()).isEqualTo("0.2.0");
    }

    @Test
    @DisplayName("Should generate timestamps for each call")
    void shouldGenerateTimestampsForEachCall() throws InterruptedException {
        HealthCheckResponse result1 = healthCheckService.performHealthCheck("first");
        Thread.sleep(10);
        HealthCheckResponse result2 = healthCheckService.performHealthCheck("second");
        
        assertThat(result1.timestamp()).isNotEqualTo(result2.timestamp());
    }

    @Test
    @DisplayName("Should handle very long message")
    void shouldHandleVeryLongMessage() {
        String longMessage = "a".repeat(1000);
        
        HealthCheckResponse result = healthCheckService.performHealthCheck(longMessage);
        
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).startsWith("Echo: ");
        assertThat(result.message()).contains(longMessage);
    }

    @Test
    @DisplayName("Should handle special characters in message")
    void shouldHandleSpecialCharactersInMessage() {
        String specialMessage = "!@#$%^&*(){}[]|\\:;\"'<>,.?/~`";
        
        HealthCheckResponse result = healthCheckService.performHealthCheck(specialMessage);
        
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("Echo: " + specialMessage);
    }

    @Test
    @DisplayName("Should handle unicode characters in message")
    void shouldHandleUnicodeCharactersInMessage() {
        String unicodeMessage = "Hello 世界 🌍 émojis";
        
        HealthCheckResponse result = healthCheckService.performHealthCheck(unicodeMessage);
        
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("Echo: " + unicodeMessage);
    }
}