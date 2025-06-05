// File: src/test/java/com/cloudsec/compliance/component/RateLimitingComponentTest.java
package com.cloudsec.compliance.components;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingComponent Tests")
class RateLimitingComponentTest {

    private RateLimitingComponent rateLimitingComponent;

    @BeforeEach
    void setUp() {
        rateLimitingComponent = new RateLimitingComponent();
    }

    @Test
    @DisplayName("Should allow requests within rate limit")
    void shouldAllowRequestsWithinRateLimit() {
        // When/Then - First 10 requests should be allowed
        for (int i = 1; i <= 10; i++) {
            boolean allowed = rateLimitingComponent.checkRateLimit("testOperation");
            assertThat(allowed)
                .as("Request %d should be allowed", i)
                .isTrue();
        }
    }

    @Test
    @DisplayName("Should reject requests exceeding rate limit")
    void shouldRejectRequestsExceedingRateLimit() {
        // Given - Use up the rate limit
        for (int i = 1; i <= 10; i++) {
            rateLimitingComponent.checkRateLimit("testOperation");
        }
        
        // When - 11th request
        boolean allowed = rateLimitingComponent.checkRateLimit("testOperation");
        
        // Then
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Should track different operations separately")
    void shouldTrackDifferentOperationsSeparately() {
        // Given - Use up rate limit for operation1
        for (int i = 1; i <= 10; i++) {
            rateLimitingComponent.checkRateLimit("operation1");
        }
        
        // When - Check operation2
        boolean allowed = rateLimitingComponent.checkRateLimit("operation2");
        
        // Then - operation2 should still be allowed
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Should reset rate limit after time window")
    void shouldResetRateLimitAfterTimeWindow() {
        // Given - Use up the rate limit
        for (int i = 1; i <= 10; i++) {
            rateLimitingComponent.checkRateLimit("testOperation");
        }
        
        // Verify rate limit is exceeded
        assertThat(rateLimitingComponent.checkRateLimit("testOperation")).isFalse();
        
        // When - Wait for rate limit window to reset (61 seconds to be safe)
        // Note: In a real test, you might want to mock time or use a shorter window
        await().atMost(61, TimeUnit.SECONDS).untilAsserted(() -> {
            boolean allowed = rateLimitingComponent.checkRateLimit("testOperation");
            assertThat(allowed).isTrue();
        });
    }

    @Test
    @DisplayName("Should handle null operation gracefully")
    void shouldHandleNullOperationGracefully() {
        // When
        boolean allowed = rateLimitingComponent.checkRateLimit(null);
        
        // Then - Should not throw exception and should have some behavior
        assertThat(allowed).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty operation string")
    void shouldHandleEmptyOperationString() {
        // When
        boolean allowed = rateLimitingComponent.checkRateLimit("");
        
        // Then
        assertThat(allowed).isNotNull();
    }

    @Test
    @DisplayName("Should handle concurrent requests safely")
    void shouldHandleConcurrentRequestsSafely() throws InterruptedException {
        final int threadCount = 5;
        final int requestsPerThread = 3;
        final Thread[] threads = new Thread[threadCount];
        final boolean[] results = new boolean[threadCount * requestsPerThread];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    int resultIndex = threadIndex * requestsPerThread + j;
                    results[resultIndex] = rateLimitingComponent.checkRateLimit("concurrentTest");
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long allowedCount = 0;
        for (boolean result : results) {
            if (result) {
                allowedCount++;
            }
        }
        
        assertThat(allowedCount)
            .as("Some requests should be allowed")
            .isGreaterThan(0);
            
        assertThat(allowedCount)
            .as("Not all requests should be allowed if rate limit works")
            .isLessThanOrEqualTo(10);
    }
}