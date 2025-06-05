package com.cloudsec.compliance.dto.response;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record S3BucketListResponse(
    @NotBlank(message = "Status cannot be blank")
    String status,
    
    @Min(value = 0, message = "Bucket count cannot be negative")
    int bucketCount,
    
    @Min(value = 0, message = "Total buckets cannot be negative") 
    int totalBuckets,
    
    @NotNull(message = "Buckets list cannot be null")
    @Valid
    List<S3BucketInfo> buckets,
    
    @NotBlank(message = "Timestamp cannot be blank")
    String timestamp,
    
    @NotBlank(message = "Region cannot be blank")
    String region,
    
    String nextPageToken,
    
    @NotNull(message = "HasMore flag cannot be null")
    Boolean hasMore,
    
    String error
) {
    public S3BucketListResponse(String status, int bucketCount, List<S3BucketInfo> buckets, 
                               String timestamp, String region) {
        this(status, bucketCount, bucketCount, buckets, timestamp, region, null, false, null);
    }
    
    public S3BucketListResponse(String status, int bucketCount, int totalBuckets, List<S3BucketInfo> buckets, 
                               String timestamp, String region, String nextPageToken, boolean hasMore) {
        this(status, bucketCount, totalBuckets, buckets, timestamp, region, nextPageToken, hasMore, null);
    }
    
    public S3BucketListResponse {
        if (buckets == null) buckets = List.of();
        if (bucketCount < 0) bucketCount = 0;
        if (totalBuckets < 0) totalBuckets = bucketCount;
        if (status == null) status = "UNKNOWN";
        if (timestamp == null) timestamp = LocalDateTime.now().toString();
        if (region == null) region = "unknown";
        if (hasMore == null) hasMore = false;
    }
}