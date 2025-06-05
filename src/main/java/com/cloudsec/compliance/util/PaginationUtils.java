package com.cloudsec.compliance.util;

import com.cloudsec.compliance.model.PaginationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class PaginationUtils {

    public <T> PaginationResult<T> paginateResults(List<T> allItems, int pageSize, String pageToken) {
        int startIndex = parsePageToken(pageToken, allItems.size());
        
        int endIndex = Math.min(startIndex + pageSize, allItems.size());
        
        List<T> pageItems = allItems.subList(startIndex, endIndex);
        
        String nextPageToken = null;
        boolean hasMore = endIndex < allItems.size();
        if (hasMore) {
            nextPageToken = generatePageToken(endIndex);
        }
        
        log.debug("Paginated {} items (start: {}, end: {}, hasMore: {})", 
                  pageItems.size(), startIndex, endIndex, hasMore);
        
        return new PaginationResult<>(pageItems, nextPageToken, hasMore);
    }
    
    private int parsePageToken(String pageToken, int maxSize) {
        if (pageToken == null || pageToken.trim().isEmpty()) {
            return 0;
        }
        
        try {
            String decoded = new String(Base64.getDecoder().decode(pageToken));
            int startIndex = Integer.parseInt(decoded);
            
            if (startIndex < 0 || startIndex >= maxSize) {
                log.warn("Invalid page token start index: {}, resetting to 0", startIndex);
                return 0;
            }
            
            return startIndex;
        } catch (Exception e) {
            log.warn("Invalid page token format: {}, resetting to 0", pageToken);
            return 0;
        }
    }
    
    private String generatePageToken(int index) {
        String indexStr = String.valueOf(index);
        return Base64.getEncoder().encodeToString(indexStr.getBytes());
    }
}