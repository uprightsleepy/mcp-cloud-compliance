package com.cloudsec.compliance.service;

import com.cloudsec.compliance.dto.response.S3BucketListResponse;
import com.cloudsec.compliance.errors.InvalidInputException;
import com.cloudsec.compliance.errors.RateLimitExceededException;
import com.cloudsec.compliance.components.InputValidator;
import com.cloudsec.compliance.components.RateLimitingComponent;
import com.cloudsec.compliance.dto.response.S3BucketInfo;
import com.cloudsec.compliance.util.PaginationUtils;
import com.cloudsec.compliance.model.PaginationResult;
import com.cloudsec.compliance.model.ComplianceResult;
import com.cloudsec.compliance.model.ComplianceStandard;
import com.cloudsec.compliance.model.ComplianceStatus;
import com.cloudsec.compliance.model.Finding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ComplianceService implements CloudComplianceService {
    
    private final InputValidator inputValidator;
    private final RateLimitingComponent rateLimitingComponent;
    private final PaginationUtils paginationUtils;
    
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_BUCKETS_RETURNED = 1000;
    private static final String CLOUD_PROVIDER = "AWS";
    private static final String STORAGE_RESOURCE_TYPE = "storage";
    
    @Override
    public ComplianceResult checkCompliance(String resourceType, ComplianceStandard standard) {
        log.info("Checking compliance for resourceType: {}, standard: {}", resourceType, standard);
        
        if (!STORAGE_RESOURCE_TYPE.equals(resourceType)) {
            return new ComplianceResult(
                "unknown",
                resourceType,
                standard,
                ComplianceStatus.NOT_APPLICABLE,
                List.of(),
                CLOUD_PROVIDER,
                "unknown"
            );
        }
        
        try {
            return new ComplianceResult(
                "s3-service",
                STORAGE_RESOURCE_TYPE,
                standard,
                ComplianceStatus.COMPLIANT,
                List.of(),
                CLOUD_PROVIDER,
                "global"
            );
        } catch (Exception e) {
            log.error("Error checking compliance for S3", e);
            return new ComplianceResult(
                "s3-service",
                STORAGE_RESOURCE_TYPE,
                standard,
                ComplianceStatus.ERROR,
                List.of(new Finding(
                    "S3-ERROR-001",
                    Finding.Severity.HIGH,
                    "SYSTEM",
                    "Unable to check S3 compliance: " + e.getMessage(),
                    "Check AWS credentials and permissions",
                    "Exception: " + e.getClass().getSimpleName()
                )),
                CLOUD_PROVIDER,
                "unknown"
            );
        }
    }
    
    @Override
    public List<String> getSupportedResourceTypes() {
        return List.of(STORAGE_RESOURCE_TYPE);
    }
    
    @Override
    public String getCloudProvider() {
        return CLOUD_PROVIDER;
    }
    
    @Override
    public List<ComplianceStandard> getSupportedStandards() {
        return List.of(
            ComplianceStandard.SOC2,
            ComplianceStandard.CIS,
            ComplianceStandard.NIST
        );
    }
    
    public S3BucketListResponse listBuckets(String region, Integer pageSize, String pageToken) {
        log.info("Listing S3 buckets for region: {}, pageSize: {}", region, pageSize);
        
        try {
            String validatedRegion = inputValidator.validateAndSanitizeRegion(region);
            int validatedPageSize = inputValidator.validatePageSize(pageSize, DEFAULT_PAGE_SIZE);
            
            if (!rateLimitingComponent.checkRateLimit("listBuckets")) {
                throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
            }
            
            S3Client s3Client = createS3Client(validatedRegion);
            
            ListBucketsResponse response = s3Client.listBuckets();
            
            List<S3BucketInfo> allBuckets = response.buckets().stream()
                .limit(MAX_BUCKETS_RETURNED)
                .map(bucket -> new S3BucketInfo(
                    inputValidator.sanitizeBucketName(bucket.name()),
                    Optional.ofNullable(bucket.creationDate())
                        .map(Object::toString)
                        .orElse("unknown"),
                    validatedRegion
                ))
                .collect(Collectors.toList());
            
            PaginationResult<S3BucketInfo> paginatedResult = 
                paginationUtils.paginateResults(allBuckets, validatedPageSize, pageToken);
            
            return new S3BucketListResponse(
                "SUCCESS",
                paginatedResult.items().size(),
                response.buckets().size(),
                paginatedResult.items(),
                LocalDateTime.now().toString(),
                validatedRegion,
                paginatedResult.nextPageToken(),
                paginatedResult.hasMore(),
                null
            );
            
        } catch (RateLimitExceededException | InvalidInputException e) {
            log.warn("Client error in listBuckets: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), region);
        } catch (S3Exception e) {
            log.error("S3 error in listBuckets: {}", e.getMessage());
            return createErrorResponse(mapS3Error(e), region);
        } catch (SdkException e) {
            log.error("AWS SDK error in listBuckets: {}", e.getMessage());
            return createErrorResponse("AWS service unavailable", region);
        } catch (Exception e) {
            log.error("Unexpected error in listBuckets", e);
            return createErrorResponse("Service temporarily unavailable", region);
        }
    }
    
    private S3Client createS3Client(String region) {
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .overrideConfiguration(builder -> builder
                .retryPolicy(retryPolicy -> retryPolicy.numRetries(2))
            )
            .build();
    }
    
    private String mapS3Error(S3Exception e) {
        return switch (e.statusCode()) {
            case 403 -> "Access denied. Please check AWS permissions.";
            case 404 -> "Resource not found";
            case 429 -> "Rate limit exceeded by AWS";
            case 500, 502, 503 -> "AWS service temporarily unavailable";
            default -> "Request failed";
        };
    }
    
    private S3BucketListResponse createErrorResponse(String message, String region) {
        return new S3BucketListResponse(
            "ERROR",
            0,
            0,
            List.of(),
            LocalDateTime.now().toString(),
            region != null ? region : "unknown",
            null,
            false,
            message
        );
    }
}