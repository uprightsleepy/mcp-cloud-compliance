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
        for (int i = 1; i <= 10; i++) {
            rateLimitingComponent.checkRateLimit("testOperation");
        }
        
        boolean allowed = rateLimitingComponent.checkRateLimit("testOperation");
        
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Should track different operations separately")
    void shouldTrackDifferentOperationsSeparately() {
        for (int i = 1; i <= 10; i++) {
            rateLimitingComponent.checkRateLimit("operation1");
        }
        
        boolean allowed = rateLimitingComponent.checkRateLimit("operation2");
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Should reset rate limit after time window")
    void shouldResetRateLimitAfterTimeWindow() {
        for (int i = 1; i <= 10; i++) {
            rateLimitingComponent.checkRateLimit("testOperation");
        }
        
        assertThat(rateLimitingComponent.checkRateLimit("testOperation")).isFalse();
        
        await().atMost(61, TimeUnit.SECONDS).untilAsserted(() -> {
            boolean allowed = rateLimitingComponent.checkRateLimit("testOperation");
            assertThat(allowed).isTrue();
        });
    }

    @Test
    @DisplayName("Should handle null operation gracefully")
    void shouldHandleNullOperationGracefully() {
        boolean allowed = rateLimitingComponent.checkRateLimit(null);
        assertThat(allowed).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty operation string")
    void shouldHandleEmptyOperationString() {
        boolean allowed = rateLimitingComponent.checkRateLimit("");
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

    @Test
    @DisplayName("Should clean up old entries when window advances")
    void shouldCleanupOldEntries() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            rateLimitingComponent.checkRateLimit("cleanupTest");
        }

        Thread.sleep(61_000);
        boolean allowed = rateLimitingComponent.checkRateLimit("cleanupTest");
        assertThat(allowed).isTrue();
    }
}