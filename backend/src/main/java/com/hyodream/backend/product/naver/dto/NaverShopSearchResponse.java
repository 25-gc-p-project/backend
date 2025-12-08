package com.hyodream.backend.product.naver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverShopSearchResponse {

    // 총 검색 결과 수
    private int total;

    // 시작 인덱스
    private int start;

    // 한 번에 보여준 개수
    private int display;

    // 실제 상품 리스트
    private List<NaverShopItemDto> items;
}