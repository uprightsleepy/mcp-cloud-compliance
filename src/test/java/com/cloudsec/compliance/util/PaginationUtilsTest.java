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
        List<String> items = List.of("item1", "item2", "item3", "item4", "item5");
        
        PaginationResult<String> result = paginationUtils.paginateResults(items, 3, null);
        
        assertThat(result.items()).containsExactly("item1", "item2", "item3");
        assertThat(result.hasMore()).isTrue();
        assertThat(result.nextPageToken()).isNotNull();
    }

    @Test
    @DisplayName("Should return correct second page with valid token")
    void shouldReturnSecondPageWithValidToken() {
        List<String> items = List.of("item1", "item2", "item3", "item4", "item5");
        String pageToken = Base64.getEncoder().encodeToString("3".getBytes());
        
        PaginationResult<String> result = paginationUtils.paginateResults(items, 2, pageToken);
        
        assertThat(result.items()).containsExactly("item4", "item5");
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextPageToken()).isNull();
    }

    @Test
    @DisplayName("Should handle last page correctly")
    void shouldHandleLastPageCorrectly() {
        List<String> items = List.of("item1", "item2", "item3");
        
        PaginationResult<String> result = paginationUtils.paginateResults(items, 5, null);
        
        assertThat(result.items()).containsExactly("item1", "item2", "item3");
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextPageToken()).isNull();
    }

    @Test
    @DisplayName("Should handle empty list")
    void shouldHandleEmptyList() {
        List<String> items = List.of();
        
        PaginationResult<String> result = paginationUtils.paginateResults(items, 5, null);
        
        assertThat(result.items()).isEmpty();
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextPageToken()).isNull();
    }

    @Test
    @DisplayName("Should handle invalid page token gracefully")
    void shouldHandleInvalidPageTokenGracefully() {
        List<String> items = List.of("item1", "item2", "item3");
        String invalidToken = "invalid-token";
        
        PaginationResult<String> result = paginationUtils.paginateResults(items, 2, invalidToken);
        
        assertThat(result.items()).containsExactly("item1", "item2");
        assertThat(result.hasMore()).isTrue();
    }

    @Test
    @DisplayName("Should handle page token with invalid index")
    void shouldHandlePageTokenWithInvalidIndex() {
        List<String> items = List.of("item1", "item2", "item3");
        String tokenWithHighIndex = Base64.getEncoder().encodeToString("100".getBytes());
        
        PaginationResult<String> result = paginationUtils.paginateResults(items, 2, tokenWithHighIndex);
        
        assertThat(result.items()).containsExactly("item1", "item2");
        assertThat(result.hasMore()).isTrue();
    }

    @Test
    @DisplayName("Should handle negative index in page token")
    void shouldHandleNegativeIndexInPageToken() {
        List<String> items = List.of("item1", "item2", "item3");
        String tokenWithNegativeIndex = Base64.getEncoder().encodeToString("-5".getBytes());
        
        PaginationResult<String> result = paginationUtils.paginateResults(items, 2, tokenWithNegativeIndex);
        
        assertThat(result.items()).containsExactly("item1", "item2");
        assertThat(result.hasMore()).isTrue();
    }

    @Test
    @DisplayName("Should generate correct page tokens")
    void shouldGenerateCorrectPageTokens() {
        List<String> items = IntStream.range(1, 11)
            .mapToObj(i -> "item" + i)
            .toList();
        
        PaginationResult<String> firstPage = paginationUtils.paginateResults(items, 3, null);
        
        assertThat(firstPage.nextPageToken()).isNotNull();
        
        PaginationResult<String> secondPage = paginationUtils.paginateResults(items, 3, firstPage.nextPageToken());
        
        assertThat(secondPage.items()).containsExactly("item4", "item5", "item6");
        assertThat(secondPage.hasMore()).isTrue();
    }

    @Test
    @DisplayName("Should handle page size larger than remaining items")
    void shouldHandlePageSizeLargerThanRemainingItems() {
        List<String> items = List.of("item1", "item2", "item3", "item4", "item5");
        String pageToken = Base64.getEncoder().encodeToString("3".getBytes());
        
        PaginationResult<String> result = paginationUtils.paginateResults(items, 10, pageToken);
        
        assertThat(result.items()).containsExactly("item4", "item5");
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextPageToken()).isNull();
    }

    @Test
    @DisplayName("Should handle single item pages")
    void shouldHandleSingleItemPages() {
        List<String> items = List.of("item1", "item2", "item3");
        
        PaginationResult<String> result = paginationUtils.paginateResults(items, 1, null);
        
        assertThat(result.items()).containsExactly("item1");
        assertThat(result.hasMore()).isTrue();
        assertThat(result.nextPageToken()).isNotNull();
        
        PaginationResult<String> nextPage = paginationUtils.paginateResults(items, 1, result.nextPageToken());
        
        assertThat(nextPage.items()).containsExactly("item2");
        assertThat(nextPage.hasMore()).isTrue();
    }
}