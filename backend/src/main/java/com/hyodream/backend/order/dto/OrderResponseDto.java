package com.hyodream.backend.order.dto;

import com.hyodream.backend.order.domain.Order;
import com.hyodream.backend.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderResponseDto {
    @Schema(description = "주문 번호 (ID)", example = "1001")
    private Long orderId;

    @Schema(description = "주문 일시", example = "2025-12-10T14:30:00")
    private LocalDateTime orderDate;

    @Schema(description = "주문 상태", example = "ORDER")
    private OrderStatus status;

    @Schema(description = "주문 상품 목록")
    private List<OrderItemResponseDto> orderItems; // 주문한 상품들 목록

    public OrderResponseDto(Order order, List<OrderItemResponseDto> orderItems) {
        this.orderId = order.getId();
        this.orderDate = order.getOrderDate();
        this.status = order.getStatus();
        this.orderItems = orderItems;
    }
}