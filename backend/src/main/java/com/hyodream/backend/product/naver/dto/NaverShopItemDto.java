package com.hyodream.backend.product.naver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverShopItemDto {

    private String title;

    private String link;

    private String image;

    // 최저가
    @JsonProperty("lprice")
    private String lprice;

    // 최고가
    @JsonProperty("hprice")
    private String hprice;

    // 쇼핑몰 이름
    private String mallName;

    // 상품 ID
    @JsonProperty("productId")
    private String productId;

    // 브랜드 / 제조사
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
}