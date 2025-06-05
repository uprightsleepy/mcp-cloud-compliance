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
        // When
        HealthCheckResponse result = healthCheckService.performHealthCheck(null);
        
        // Then
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("MCP Cloud Compliance Server is running");
        assertThat(result.version()).isEqualTo("0.2.0");
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should echo back provided message")
    void shouldEchoBackProvidedMessage() {
        // Given
        String inputMessage = "test message";
        
        // When
        HealthCheckResponse result = healthCheckService.performHealthCheck(inputMessage);
        
        // Then
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("Echo: test message");
        assertThat(result.version()).isEqualTo("0.2.0");
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty string message")
    void shouldHandleEmptyStringMessage() {
        // When
        HealthCheckResponse result = healthCheckService.performHealthCheck("");
        
        // Then
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("Echo: ");
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle whitespace message")
    void shouldHandleWhitespaceMessage() {
        // When
        HealthCheckResponse result = healthCheckService.performHealthCheck("   ");
        
        // Then
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("Echo:    ");
    }

    @Test
    @DisplayName("Should always return consistent version")
    void shouldAlwaysReturnConsistentVersion() {
        // When
        HealthCheckResponse result1 = healthCheckService.performHealthCheck("message1");
        HealthCheckResponse result2 = healthCheckService.performHealthCheck("message2");
        
        // Then
        assertThat(result1.version()).isEqualTo(result2.version());
        assertThat(result1.version()).isEqualTo("0.2.0");
    }

    @Test
    @DisplayName("Should generate timestamps for each call")
    void shouldGenerateTimestampsForEachCall() throws InterruptedException {
        // When
        HealthCheckResponse result1 = healthCheckService.performHealthCheck("first");
        Thread.sleep(10); // Small delay to ensure different timestamps
        HealthCheckResponse result2 = healthCheckService.performHealthCheck("second");
        
        // Then
        assertThat(result1.timestamp()).isNotEqualTo(result2.timestamp());
    }

    @Test
    @DisplayName("Should handle very long message")
    void shouldHandleVeryLongMessage() {
        // Given
        String longMessage = "a".repeat(1000);
        
        // When
        HealthCheckResponse result = healthCheckService.performHealthCheck(longMessage);
        
        // Then
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).startsWith("Echo: ");
        assertThat(result.message()).contains(longMessage);
    }

    @Test
    @DisplayName("Should handle special characters in message")
    void shouldHandleSpecialCharactersInMessage() {
        // Given
        String specialMessage = "!@#$%^&*(){}[]|\\:;\"'<>,.?/~`";
        
        // When
        HealthCheckResponse result = healthCheckService.performHealthCheck(specialMessage);
        
        // Then
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("Echo: " + specialMessage);
    }

    @Test
    @DisplayName("Should handle unicode characters in message")
    void shouldHandleUnicodeCharactersInMessage() {
        // Given
        String unicodeMessage = "Hello 世界 🌍 émojis";
        
        // When
        HealthCheckResponse result = healthCheckService.performHealthCheck(unicodeMessage);
        
        // Then
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.message()).isEqualTo("Echo: " + unicodeMessage);
    }
}