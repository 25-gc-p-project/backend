package com.hyodream.backend.order.controller;

import com.hyodream.backend.order.dto.OrderRequestDto;
import com.hyodream.backend.order.dto.OrderResponseDto;
import com.hyodream.backend.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 주문하기 API
    // POST http://localhost:8080/api/orders
    @PostMapping
    public ResponseEntity<String> createOrder(
            @RequestBody List<OrderRequestDto> requestDtos) {
        orderService.order(requestDtos);
        return ResponseEntity.ok("주문이 완료되었습니다.");
    }

    // 내 주문 내역 조회
    // GET http://localhost:8080/api/orders
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrders());
    }

    // 주문 취소
    // POST http://localhost:8080/api/orders/{orderId}/cancel
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(
            @PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok("주문이 취소되었습니다.");
    }
}