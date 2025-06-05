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
        String result = inputValidator.validateAndSanitizeRegion(null);
        assertThat(result).isEqualTo("us-east-1");
    }

    @Test
    @DisplayName("Should return default region when input is empty")
    void shouldReturnDefaultRegionWhenEmpty() {
        String result = inputValidator.validateAndSanitizeRegion("");
        assertThat(result).isEqualTo("us-east-1");
    }

    @Test
    @DisplayName("Should return default region when input is whitespace")
    void shouldReturnDefaultRegionWhenWhitespace() {
        String result = inputValidator.validateAndSanitizeRegion("   ");
        assertThat(result).isEqualTo("us-east-1");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1", "ca-central-1"
    })
    @DisplayName("Should accept valid AWS regions")
    void shouldAcceptValidRegions(String validRegion) {
        String result = inputValidator.validateAndSanitizeRegion(validRegion);
        assertThat(result).isEqualTo(validRegion);
    }

    @Test
    @DisplayName("Should sanitize region input by trimming and lowercasing")
    void shouldSanitizeRegionInput() {
        String result = inputValidator.validateAndSanitizeRegion("  US-EAST-1  ");
        assertThat(result).isEqualTo("us-east-1");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-region", "hacker-region", "us-east-99", "fake-region", "../../etc/passwd"
    })
    @DisplayName("Should reject invalid regions")
    void shouldRejectInvalidRegions(String invalidRegion) {
        assertThatThrownBy(() -> inputValidator.validateAndSanitizeRegion(invalidRegion))
            .isInstanceOf(InvalidInputException.class)
            .hasMessageContaining("Invalid region specified");
    }

    @Test
    @DisplayName("Should sanitize malicious characters from region input")
    void shouldSanitizeMaliciousCharacters() {
        assertThatThrownBy(() -> inputValidator.validateAndSanitizeRegion("us-east-1; rm -rf /"))
            .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("Should return default page size when input is null")
    void shouldReturnDefaultPageSizeWhenNull() {
        int result = inputValidator.validatePageSize(null, 20);
        assertThat(result).isEqualTo(20);
    }

    @Test
    @DisplayName("Should return minimum page size when input is too small")
    void shouldReturnMinimumPageSizeWhenTooSmall() {
        int result = inputValidator.validatePageSize(-5, 20);
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return maximum page size when input is too large")
    void shouldReturnMaximumPageSizeWhenTooLarge() {
        int result = inputValidator.validatePageSize(1000, 20);
        assertThat(result).isEqualTo(100);
    }

    @Test
    @DisplayName("Should return input when page size is valid")
    void shouldReturnInputWhenPageSizeValid() {
        int result = inputValidator.validatePageSize(25, 20);
        assertThat(result).isEqualTo(25);
    }

    @Test
    @DisplayName("Should return bucket name unchanged when not sensitive")
    void shouldReturnBucketNameUnchangedWhenNotSensitive() {
        String result = inputValidator.sanitizeBucketName("my-app-bucket");
        assertThat(result).isEqualTo("my-app-bucket");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "secret-bucket", "private-data", "internal-files", "confidential-backup"
    })
    @DisplayName("Should mask sensitive bucket names")
    void shouldMaskSensitiveBucketNames(String sensitiveName) {
        String result = inputValidator.sanitizeBucketName(sensitiveName);
        assertThat(result).endsWith("***");
        assertThat(result.length()).isLessThanOrEqualTo(sensitiveName.length());
    }

    @Test
    @DisplayName("Should handle null bucket name")
    void shouldHandleNullBucketName() {
        String result = inputValidator.sanitizeBucketName(null);
        assertThat(result).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Should handle short bucket name not matching sensitive pattern")
    void shouldHandleShortNonSensitiveBucket() {
        String result = inputValidator.sanitizeBucketName("se");
        assertThat(result).isEqualTo("se");
    }

    @Test
    @DisplayName("Should handle short bucket name that partially matches sensitive pattern")
    void shouldHandleShortBucketThatLooksSensitive() {
        String result = inputValidator.sanitizeBucketName("sec");
        assertThat(result).isEqualTo("sec");
    }

    @Test
    @DisplayName("Should mask 'secret' bucket name")
    void shouldMaskExactlySecretBucketName() {
        String result = inputValidator.sanitizeBucketName("secret");
        assertThat(result).isEqualTo("sec***");
    }
}
