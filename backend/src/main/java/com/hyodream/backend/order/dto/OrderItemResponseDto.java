package com.hyodream.backend.order.dto;

import com.hyodream.backend.order.domain.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class OrderItemResponseDto {
    @Schema(description = "상품 ID", example = "10")
    private Long productId;

    @Schema(description = "상품명", example = "정관장 홍삼정")
    private String productName; // 상품 이름 (DB 조회해서 채움)

    @Schema(description = "주문 수량", example = "2")
    private int count;

    @Schema(description = "주문 당시 가격 (단가)", example = "98000")
    private int orderPrice; // 구매 당시 가격

    public OrderItemResponseDto(OrderItem orderItem, String productName) {
        this.productId = orderItem.getProductId();
        this.productName = productName;
        this.count = orderItem.getCount();
        this.orderPrice = orderItem.getOrderPrice();
    }
}