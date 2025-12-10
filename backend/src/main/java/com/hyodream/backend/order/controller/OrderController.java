package com.hyodream.backend.order.controller;

import com.hyodream.backend.order.dto.OrderRequestDto;
import com.hyodream.backend.order.dto.OrderResponseDto;
import com.hyodream.backend.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Order API", description = "주문 생성, 조회, 취소 관리")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성", description = "장바구니 또는 바로 구매를 통해 상품을 주문합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 완료"),
            @ApiResponse(responseCode = "400", description = "재고 부족 또는 잘못된 요청")
    })
    @PostMapping
    public ResponseEntity<String> createOrder(
            @RequestBody List<OrderRequestDto> requestDtos) {
        orderService.order(requestDtos);
        return ResponseEntity.ok("주문이 완료되었습니다.");
    }

    @Operation(summary = "내 주문 내역 조회", description = "로그인한 사용자의 모든 주문 내역을 최신순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrders());
    }

    @Operation(summary = "주문 취소", description = "주문 ID를 받아 해당 주문을 취소하고 결제를 환불합니다.")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(
            @Parameter(description = "취소할 주문 ID") @PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok("주문이 취소되었습니다.");
    }
}