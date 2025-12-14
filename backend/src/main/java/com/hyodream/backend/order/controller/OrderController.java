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

@Tag(name = "Order API", description = "상품 주문 및 취소 관리 API")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성 (결제)", description = """
            상품을 주문하고 결제를 진행합니다.
            
            **[주문 프로세스]**
            1. **재고/상태 확인:** 상품의 판매 가능 여부를 확인합니다.
            2. **주문서 생성:** `orders`, `order_items` 테이블에 데이터를 생성합니다.
            3. **판매량 집계:** 해당 상품의 `totalSales` 및 `recentSales` 카운트를 즉시 증가시킵니다.
            4. **결제 처리:** 가상의 결제 모듈(`PaymentService`)을 호출하여 결제 승인(`DONE`) 상태를 기록합니다.
            """)
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

    @Operation(summary = "내 주문 내역 조회", description = """
            로그인한 사용자의 모든 주문 이력을 최신순으로 조회합니다.
            
            **[반환 정보]**
            - 주문 번호, 날짜, 상태(`ORDER`/`CANCEL`)
            - 주문 상품 목록 (상품명, 수량, 구매 당시 가격)
            """)
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrders());
    }

    @Operation(summary = "주문 취소 (환불)", description = """
            특정 주문을 취소하고 환불 처리합니다.
            
            **[취소 프로세스]**
            1. **권한 확인:** 본인의 주문인지 확인합니다.
            2. **상태 변경:** 주문 상태를 `CANCEL`로 변경합니다.
            3. **데이터 복구:** 상품의 판매량(`totalSales`)을 차감하여 원상 복구합니다.
            4. **결제 취소:** `payments` 테이블의 상태를 `CANCELED`로 변경합니다.
            """)
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(
            @Parameter(description = "취소할 주문 ID") @PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok("주문이 취소되었습니다.");
    }
}