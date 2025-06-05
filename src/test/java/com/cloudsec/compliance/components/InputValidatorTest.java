package com.cloudsec.compliance.components;

import com.cloudsec.compliance.errors.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InputValidator Tests")
class InputValidatorTest {

    private InputValidator inputValidator;

    @BeforeEach
    void setUp() {
        inputValidator = new InputValidator();
    }

    @Test
    @DisplayName("Should return default region when input is null")
    void shouldReturnDefaultRegionWhenNull() {
        // When
        String result = inputValidator.validateAndSanitizeRegion(null);
        
        // Then
        assertThat(result).isEqualTo("us-east-1");
    }

    @Test
    @DisplayName("Should return default region when input is empty")
    void shouldReturnDefaultRegionWhenEmpty() {
        // When
        String result = inputValidator.validateAndSanitizeRegion("");
        
        // Then
        assertThat(result).isEqualTo("us-east-1");
    }

    @Test
    @DisplayName("Should return default region when input is whitespace")
    void shouldReturnDefaultRegionWhenWhitespace() {
        // When
        String result = inputValidator.validateAndSanitizeRegion("   ");
        
        // Then
        assertThat(result).isEqualTo("us-east-1");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1", "ca-central-1"
    })
    @DisplayName("Should accept valid AWS regions")
    void shouldAcceptValidRegions(String validRegion) {
        // When
        String result = inputValidator.validateAndSanitizeRegion(validRegion);
        
        // Then
        assertThat(result).isEqualTo(validRegion);
    }

    @Test
    @DisplayName("Should sanitize region input by trimming and lowercasing")
    void shouldSanitizeRegionInput() {
        // When
        String result = inputValidator.validateAndSanitizeRegion("  US-EAST-1  ");
        
        // Then
        assertThat(result).isEqualTo("us-east-1");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-region", "hacker-region", "us-east-99", "fake-region", "../../etc/passwd"
    })
    @DisplayName("Should reject invalid regions")
    void shouldRejectInvalidRegions(String invalidRegion) {
        // When/Then
        assertThatThrownBy(() -> inputValidator.validateAndSanitizeRegion(invalidRegion))
            .isInstanceOf(InvalidInputException.class)
            .hasMessageContaining("Invalid region specified");
    }

    @Test
    @DisplayName("Should sanitize malicious characters from region input")
    void shouldSanitizeMaliciousCharacters() {
        // When/Then
        assertThatThrownBy(() -> inputValidator.validateAndSanitizeRegion("us-east-1; rm -rf /"))
            .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("Should return default page size when input is null")
    void shouldReturnDefaultPageSizeWhenNull() {
        // When
        int result = inputValidator.validatePageSize(null, 20);
        
        // Then
        assertThat(result).isEqualTo(20);
    }

    @Test
    @DisplayName("Should return minimum page size when input is too small")
    void shouldReturnMinimumPageSizeWhenTooSmall() {
        // When
        int result = inputValidator.validatePageSize(-5, 20);
        
        // Then
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return maximum page size when input is too large")
    void shouldReturnMaximumPageSizeWhenTooLarge() {
        // When
        int result = inputValidator.validatePageSize(1000, 20);
        
        // Then
        assertThat(result).isEqualTo(100);
    }

    @Test
    @DisplayName("Should return input when page size is valid")
    void shouldReturnInputWhenPageSizeValid() {
        // When
        int result = inputValidator.validatePageSize(25, 20);
        
        // Then
        assertThat(result).isEqualTo(25);
    }

    @Test
    @DisplayName("Should return bucket name unchanged when not sensitive")
    void shouldReturnBucketNameUnchangedWhenNotSensitive() {
        // When
        String result = inputValidator.sanitizeBucketName("my-app-bucket");
        
        // Then
        assertThat(result).isEqualTo("my-app-bucket");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "secret-bucket", "private-data", "internal-files", "confidential-backup"
    })
    @DisplayName("Should mask sensitive bucket names")
    void shouldMaskSensitiveBucketNames(String sensitiveName) {
        // When
        String result = inputValidator.sanitizeBucketName(sensitiveName);
        
        // Then
        assertThat(result).endsWith("***");
        assertThat(result).hasSizeLessThanOrEqualTo(6); // 3 chars + "***"
    }

    @Test
    @DisplayName("Should handle null bucket name")
    void shouldHandleNullBucketName() {
        // When
        String result = inputValidator.sanitizeBucketName(null);
        
        // Then
        assertThat(result).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Should handle very short sensitive bucket names")
    void shouldHandleShortSensitiveBucketNames() {
        // When
        String result = inputValidator.sanitizeBucketName("se");
        
        // Then - "se" doesn't contain sensitive patterns, so it shouldn't be masked
        assertThat(result).isEqualTo("se");
    }
    
    @Test
    @DisplayName("Should handle short bucket name containing sensitive pattern")
    void shouldHandleShortBucketNameWithSensitivePattern() {
        // When
        String result = inputValidator.sanitizeBucketName("sec");
        
        // Then - "sec" doesn't fully contain "secret", so shouldn't be masked
        assertThat(result).isEqualTo("sec");
        
        // But if it contains the full pattern:
        String secretResult = inputValidator.sanitizeBucketName("secret");
        assertThat(secretResult).isEqualTo("sec***");
    }
}