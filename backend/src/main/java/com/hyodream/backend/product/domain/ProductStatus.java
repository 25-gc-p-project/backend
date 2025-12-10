package com.hyodream.backend.product.domain;

public enum ProductStatus {
    ON_SALE,     // 판매 중
    SOLD_OUT,    // 품절
    STOP_SELLING // 판매 중지 (장기 미업데이트 등)
}