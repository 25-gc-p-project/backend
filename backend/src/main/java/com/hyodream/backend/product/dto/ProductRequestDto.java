package com.hyodream.backend.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ProductRequestDto {

    @Schema(description = "상품명", example = "정관장 홍삼정 에브리타임")
    private String name;

    @Schema(description = "가격", example = "98000")
    private int price;

    @Schema(description = "상품 상세 설명", example = "면역력 증진에 도움을 줄 수 있는 홍삼 제품입니다.")
    private String description;

    @Schema(description = "상품 이미지 URL", example = "https://example.com/images/red_ginseng.jpg")
    private String imageUrl;

    @Schema(description = "용량/수량", example = "10ml x 30포")
    private String volume;

    @Schema(description = "크기/규격 정보", example = "가로 20cm, 세로 10cm")
    private String sizeInfo;

    @Schema(description = "효능 태그 목록", example = "[\"면역력 강화\", \"피로 회복\"]")
    private List<String> healthBenefits;

    @Schema(description = "알레르기 유발 성분 목록", example = "[\"없음\"]")
    private List<String> allergens;
}