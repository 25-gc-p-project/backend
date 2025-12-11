package com.hyodream.backend.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiProductDetailDto {
    // 상품 상세 정보
    private int originalPrice;
    private int discountRate;
    private String seller;
    private long reviewCount;
    private double rating;

    // 크롤링된 리뷰 목록
    private List<CrawledReviewDto> reviews;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CrawledReviewDto {
        private String externalReviewId; // 리뷰 고유 ID
        private String authorName;       // 마스킹된 작성자
        private String content;          // 내용
        private int score;               // 평점 (1~5)
        private String productOption;    // 옵션 (색상/사이즈)
        private List<String> images;     // 첨부 이미지 URL
        private String createDate;       // 작성일
    }
}
