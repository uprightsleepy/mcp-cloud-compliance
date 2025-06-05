package com.cloudsec.compliance.components;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class RateLimitingComponent {
    
    private final Map<String, AtomicLong> rateLimiters = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_WINDOW_MS = 60_000; // 1 minute
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    
    public boolean checkRateLimit(String operation) {
        String key = operation + "_" + (System.currentTimeMillis() / RATE_LIMIT_WINDOW_MS);
        AtomicLong counter = rateLimiters.computeIfAbsent(key, k -> new AtomicLong(0));
        
        cleanupOldEntries();
        
        long currentCount = counter.incrementAndGet();
        
        if (currentCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for operation: {} (count: {})", operation, currentCount);
            return false;
        }
        
        log.debug("Rate limit check passed for operation: {} (count: {})", operation, currentCount);
        return true;
    }
    
    private void cleanupOldEntries() {
        long currentWindow = System.currentTimeMillis() / RATE_LIMIT_WINDOW_MS;
        rateLimiters.entrySet().removeIf(entry -> 
            !entry.getKey().endsWith("_" + currentWindow));
    }
}
