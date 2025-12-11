package com.hyodream.backend.global.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hyodream.backend.product.dto.AiProductDetailDto;
import com.hyodream.backend.user.dto.HealthInfoRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "ai-client", url = "${ai.server.url:http://hyodream-ai:8000}")
public interface AiClient {

    // 추천 시스템
    @PostMapping("/recommend-products")
    AiRecommendResponse getRecommendations(@RequestBody HealthInfoRequestDto healthInfo);

    // 크롤링
    @PostMapping("/crawl/product")
    AiProductDetailDto getProductDetail(@RequestBody CrawlRequest request);

    // [New] 리뷰 감성 분석
    @PostMapping("/analyze")
    AiSentimentResponse analyzeReviews(@RequestBody SentimentRequest request);


    // --- DTOs ---
    
    record CrawlRequest(String url) {}

    record AiRecommendResponse(@JsonProperty("product_ids") List<Long> productIds) {}

    // 감성 분석 요청
    record SentimentRequest(List<String> reviews) {}

    // 감성 분석 응답
    record AiSentimentResponse(
        @JsonProperty("total_reviews") int totalReviews,
        @JsonProperty("positive_percent") double positivePercent,
        @JsonProperty("negative_percent") double negativePercent,
        @JsonProperty("positive_count") int positiveCount,
        @JsonProperty("negative_count") int negativeCount
    ) {}
}