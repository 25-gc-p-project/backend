package com.hyodream.backend.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequestDto {
    @Schema(description = "주문할 상품 ID", example = "10")
    private Long productId;

    @Schema(description = "주문 수량", example = "2")
    private int count;
}