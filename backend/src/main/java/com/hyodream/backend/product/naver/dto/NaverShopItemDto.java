package com.hyodream.backend.product.naver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverShopItemDto {

    private String title;       // 상품명
    private String link;        // 상세 페이지 링크
    private String image;       // 이미지 URL

    @JsonProperty("lprice")
    private String lprice;      // 판매가 (최저가)

    @JsonProperty("productId")
    private String productId;   // 네이버 상품 ID

    private String mallName;    // 판매자 (seller)

    private String brand;
    private String maker;

    // 카테고리
    @JsonProperty("category1")
    private String category1;
    @JsonProperty("category2")
    private String category2;
    @JsonProperty("category3")
    private String category3;
    @JsonProperty("category4")
    private String category4;

    // --- [New] 확장된 필드 (AI 서버/크롤러 제공) ---
    private int originalPrice;  // 원가
    private int discountRate;   // 할인율
    private long reviewCount;   // 리뷰 수
    private double rating;      // 평점 (0~5.0)
}
