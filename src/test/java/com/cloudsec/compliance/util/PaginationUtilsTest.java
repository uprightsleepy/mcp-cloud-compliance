package com.cloudsec.compliance.util;

import com.cloudsec.compliance.model.PaginationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaginationUtils Tests")
class PaginationUtilsTest {

    private PaginationUtils paginationUtils;

    @BeforeEach
    void setUp() {
        paginationUtils = new PaginationUtils();
    }

    @Test
    @DisplayName("Should return first page when no token provided")
    void shouldReturnFirstPageWhenNoToken() {
        // Given
        List<String> items = List.of("item1", "item2", "item3", "item4", "item5");
        
        // When
        PaginationResult<String> result = paginationUtils.paginateResults(items, 3, null);
        
        // Then
        assertThat(result.items()).containsExactly("item1", "item2", "item3");
        assertThat(result.hasMore()).isTrue();
        assertThat(result.nextPageToken()).isNotNull();
    }

    @Test
    @DisplayName("Should return correct second page with valid token")
    void shouldReturnSecondPageWithValidToken() {
        // Given
        List<String> items = List.of("item1", "item2", "item3", "item4", "item5");
        String pageToken = Base64.getEncoder().encodeToString("3".getBytes()); // Start at index 3
        
        // When
        PaginationResult<String> result = paginationUtils.paginateResults(items, 2, pageToken);
        
        // Then
        assertThat(result.items()).containsExactly("item4", "item5");
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextPageToken()).isNull();
    }

    @Test
    @DisplayName("Should handle last page correctly")
    void shouldHandleLastPageCorrectly() {
        // Given
        List<String> items = List.of("item1", "item2", "item3");
        
        // When
        PaginationResult<String> result = paginationUtils.paginateResults(items, 5, null);
        
        // Then
        assertThat(result.items()).containsExactly("item1", "item2", "item3");
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextPageToken()).isNull();
    }

    @Test
    @DisplayName("Should handle empty list")
    void shouldHandleEmptyList() {
        // Given
        List<String> items = List.of();
        
        // When
        PaginationResult<String> result = paginationUtils.paginateResults(items, 5, null);
        
        // Then
        assertThat(result.items()).isEmpty();
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextPageToken()).isNull();
    }

    @Test
    @DisplayName("Should handle invalid page token gracefully")
    void shouldHandleInvalidPageTokenGracefully() {
        // Given
        List<String> items = List.of("item1", "item2", "item3");
        String invalidToken = "invalid-token";
        
        // When
        PaginationResult<String> result = paginationUtils.paginateResults(items, 2, invalidToken);
        
        // Then - Should default to first page
        assertThat(result.items()).containsExactly("item1", "item2");
        assertThat(result.hasMore()).isTrue();
    }

    @Test
    @DisplayName("Should handle page token with invalid index")
    void shouldHandlePageTokenWithInvalidIndex() {
        // Given
        List<String> items = List.of("item1", "item2", "item3");
        String tokenWithHighIndex = Base64.getEncoder().encodeToString("100".getBytes());
        
        // When
        PaginationResult<String> result = paginationUtils.paginateResults(items, 2, tokenWithHighIndex);
        
        // Then - Should default to first page
        assertThat(result.items()).containsExactly("item1", "item2");
        assertThat(result.hasMore()).isTrue();
    }

    @Test
    @DisplayName("Should handle negative index in page token")
    void shouldHandleNegativeIndexInPageToken() {
        // Given
        List<String> items = List.of("item1", "item2", "item3");
        String tokenWithNegativeIndex = Base64.getEncoder().encodeToString("-5".getBytes());
        
        // When
        PaginationResult<String> result = paginationUtils.paginateResults(items, 2, tokenWithNegativeIndex);
        
        // Then - Should default to first page
        assertThat(result.items()).containsExactly("item1", "item2");
        assertThat(result.hasMore()).isTrue();
    }

    @Test
    @DisplayName("Should generate correct page tokens")
    void shouldGenerateCorrectPageTokens() {
        // Given
        List<String> items = IntStream.range(1, 11)
            .mapToObj(i -> "item" + i)
            .toList();
        
        // When - Get first page
        PaginationResult<String> firstPage = paginationUtils.paginateResults(items, 3, null);
        
        // Then
        assertThat(firstPage.nextPageToken()).isNotNull();
        
        // When - Use token for second page
        PaginationResult<String> secondPage = paginationUtils.paginateResults(items, 3, firstPage.nextPageToken());
        
        // Then
        assertThat(secondPage.items()).containsExactly("item4", "item5", "item6");
        assertThat(secondPage.hasMore()).isTrue();
    }

    @Test
    @DisplayName("Should handle page size larger than remaining items")
    void shouldHandlePageSizeLargerThanRemainingItems() {
        // Given
        List<String> items = List.of("item1", "item2", "item3", "item4", "item5");
        String pageToken = Base64.getEncoder().encodeToString("3".getBytes()); // Start at index 3
        
        // When - Request more items than remaining
        PaginationResult<String> result = paginationUtils.paginateResults(items, 10, pageToken);
        
        // Then
        assertThat(result.items()).containsExactly("item4", "item5");
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextPageToken()).isNull();
    }

    @Test
    @DisplayName("Should handle single item pages")
    void shouldHandleSingleItemPages() {
        // Given
        List<String> items = List.of("item1", "item2", "item3");
        
        // When
        PaginationResult<String> result = paginationUtils.paginateResults(items, 1, null);
        
        // Then
        assertThat(result.items()).containsExactly("item1");
        assertThat(result.hasMore()).isTrue();
        assertThat(result.nextPageToken()).isNotNull();
        
        // When - Get next page
        PaginationResult<String> nextPage = paginationUtils.paginateResults(items, 1, result.nextPageToken());
        
        // Then
        assertThat(nextPage.items()).containsExactly("item2");
        assertThat(nextPage.hasMore()).isTrue();
    }
}