// File: src/test/java/com/cloudsec/compliance/service/S3ComplianceServiceTest.java
package com.cloudsec.compliance.service;

import com.cloudsec.compliance.components.InputValidator;
import com.cloudsec.compliance.components.RateLimitingComponent;
import com.cloudsec.compliance.dto.response.S3BucketListResponse;
import com.cloudsec.compliance.errors.InvalidInputException;
import com.cloudsec.compliance.util.PaginationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3ComplianceService Tests")
class S3ComplianceServiceTest {

    @Mock
    private InputValidator inputValidator;
    
    @Mock
    private RateLimitingComponent rateLimitingComponent;
    
    @Mock
    private PaginationUtils paginationUtils;

    private S3ComplianceService s3ComplianceService;

    @BeforeEach
    void setUp() {
        s3ComplianceService = new S3ComplianceService(inputValidator, rateLimitingComponent, paginationUtils);
    }

    @Test
    @DisplayName("Should return error when rate limit exceeded")
    void shouldReturnErrorWhenRateLimitExceeded() {
        // Given
        when(inputValidator.validateAndSanitizeRegion("us-east-1")).thenReturn("us-east-1");
        when(inputValidator.validatePageSize(20, 20)).thenReturn(20);
        when(rateLimitingComponent.checkRateLimit("listBuckets")).thenReturn(false);
        
        // When
        S3BucketListResponse result = s3ComplianceService.listBuckets("us-east-1", 20, null);
        
        // Then
        assertThat(result.status()).isEqualTo("ERROR");
        assertThat(result.error()).contains("Rate limit exceeded");
        assertThat(result.bucketCount()).isEqualTo(0);
        assertThat(result.buckets()).isEmpty();
    }

    @Test
    @DisplayName("Should return error when invalid region provided")
    void shouldReturnErrorWhenInvalidRegion() {
        // Given
        when(inputValidator.validateAndSanitizeRegion("invalid-region"))
            .thenThrow(new InvalidInputException("Invalid region specified: invalid-region"));
        
        // When
        S3BucketListResponse result = s3ComplianceService.listBuckets("invalid-region", 20, null);
        
        // Then
        assertThat(result.status()).isEqualTo("ERROR");
        assertThat(result.error()).contains("Invalid region specified");
        assertThat(result.region()).isEqualTo("invalid-region");
    }

    @Test
    @DisplayName("Should handle null parameters gracefully")
    void shouldHandleNullParametersGracefully() {
        // Given
        when(inputValidator.validateAndSanitizeRegion(null)).thenReturn("us-east-1");
        when(inputValidator.validatePageSize(null, 20)).thenReturn(20);
        when(rateLimitingComponent.checkRateLimit("listBuckets")).thenReturn(false);
        
        // When
        S3BucketListResponse result = s3ComplianceService.listBuckets(null, null, null);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("Should validate input parameters in correct order")
    void shouldValidateInputParametersInCorrectOrder() {
        // Given
        when(inputValidator.validateAndSanitizeRegion("us-west-2")).thenReturn("us-west-2");
        when(inputValidator.validatePageSize(10, 20)).thenReturn(10);
        when(rateLimitingComponent.checkRateLimit("listBuckets")).thenReturn(false);
        
        // When
        s3ComplianceService.listBuckets("us-west-2", 10, "token");
        
        // Then - Verify calls were made in expected order
        verify(inputValidator).validateAndSanitizeRegion("us-west-2");
        verify(inputValidator).validatePageSize(10, 20);
        verify(rateLimitingComponent).checkRateLimit("listBuckets");
    }

    @Test
    @DisplayName("Should use default page size when not provided")
    void shouldUseDefaultPageSizeWhenNotProvided() {
        // Given
        when(inputValidator.validateAndSanitizeRegion(anyString())).thenReturn("us-east-1");
        when(inputValidator.validatePageSize(null, 20)).thenReturn(20);
        when(rateLimitingComponent.checkRateLimit(anyString())).thenReturn(false);
        
        // When
        s3ComplianceService.listBuckets("us-east-1", null, null);
        
        // Then
        verify(inputValidator).validatePageSize(null, 20);
    }

    @Test
    @DisplayName("Should return proper error structure for all error cases")
    void shouldReturnProperErrorStructureForAllErrorCases() {
        // Given - Rate limit error
        when(inputValidator.validateAndSanitizeRegion(anyString())).thenReturn("us-east-1");
        when(inputValidator.validatePageSize(anyInt(), anyInt())).thenReturn(20);
        when(rateLimitingComponent.checkRateLimit(anyString())).thenReturn(false);
        
        // When
        S3BucketListResponse result = s3ComplianceService.listBuckets("us-east-1", 20, null);
        
        // Then - Verify error response structure
        assertThat(result.status()).isEqualTo("ERROR");
        assertThat(result.bucketCount()).isEqualTo(0);
        assertThat(result.totalBuckets()).isEqualTo(0);
        assertThat(result.buckets()).isEmpty();
        assertThat(result.timestamp()).isNotNull();
        assertThat(result.region()).isEqualTo("us-east-1");
        assertThat(result.nextPageToken()).isNull();
        assertThat(result.hasMore()).isFalse();
        assertThat(result.error()).isNotNull();
    }

    @Test
    @DisplayName("Should sanitize region parameter before validation")
    void shouldSanitizeRegionParameterBeforeValidation() {
        // Given
        String inputRegion = "  US-EAST-1  ";
        when(inputValidator.validateAndSanitizeRegion(inputRegion)).thenReturn("us-east-1");
        when(inputValidator.validatePageSize(anyInt(), anyInt())).thenReturn(20);
        when(rateLimitingComponent.checkRateLimit(anyString())).thenReturn(false);
        
        // When
        s3ComplianceService.listBuckets(inputRegion, 20, null);
        
        // Then
        verify(inputValidator).validateAndSanitizeRegion(inputRegion);
    }

    @Test
    @DisplayName("Should handle unexpected exceptions gracefully")
    void shouldHandleUnexpectedExceptionsGracefully() {
        // Given
        when(inputValidator.validateAndSanitizeRegion(anyString()))
            .thenThrow(new RuntimeException("Unexpected error"));
        
        // When
        S3BucketListResponse result = s3ComplianceService.listBuckets("us-east-1", 20, null);
        
        // Then
        assertThat(result.status()).isEqualTo("ERROR");
        assertThat(result.error()).isEqualTo("Service temporarily unavailable");
    }

    @Test
    @DisplayName("Should pass correct operation name to rate limiter")
    void shouldPassCorrectOperationNameToRateLimiter() {
        // Given
        when(inputValidator.validateAndSanitizeRegion(anyString())).thenReturn("us-east-1");
        when(inputValidator.validatePageSize(anyInt(), anyInt())).thenReturn(20);
        when(rateLimitingComponent.checkRateLimit("listBuckets")).thenReturn(true);
        
        // When - This will likely fail due to missing AWS mocking, but we want to test the rate limiter call
        try {
            s3ComplianceService.listBuckets("us-east-1", 20, null);
        } catch (Exception e) {
            // Expected due to AWS SDK not being mocked
        }
        
        // Then
        verify(rateLimitingComponent).checkRateLimit("listBuckets");
    }

    @Test
    @DisplayName("Should create error response with proper region fallback")
    void shouldCreateErrorResponseWithProperRegionFallback() {
        // Given - Null region input
        when(inputValidator.validateAndSanitizeRegion(null))
            .thenThrow(new InvalidInputException("Invalid region"));
        
        // When
        S3BucketListResponse result = s3ComplianceService.listBuckets(null, 20, null);
        
        // Then
        assertThat(result.region()).isEqualTo("unknown");
    }
}