package com.hyodream.backend.product.dto;

import com.hyodream.backend.product.domain.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import java.util.List;

@Getter
public class ProductResponseDto {

    @Schema(description = "상품 ID", example = "1")
    private Long id;

    @Schema(description = "상품명", example = "종근당 락토핏 골드")
    private String name;

    @Schema(description = "가격", example = "15900")
    private int price;

    @Schema(description = "상품 이미지 URL", example = "https://shopping-phinf.pstatic.net/...")
    private String imageUrl;

    @Schema(description = "효능 태그 (자동 분석)", example = "[\"장 건강\", \"면역력 강화\"]")
    private List<String> healthBenefits;

    @Schema(description = "포함된 알레르기 성분 (로그인 유저의 경우 필터링됨)", example = "[\"우유\", \"대두\"]")
    private List<String> allergens;

    // 엔티티 -> DTO 변환 생성자
    public ProductResponseDto(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.price = product.getPrice();
        this.imageUrl = product.getImageUrl();
        this.healthBenefits = product.getHealthBenefits();
        this.allergens = product.getAllergens();
    }
}