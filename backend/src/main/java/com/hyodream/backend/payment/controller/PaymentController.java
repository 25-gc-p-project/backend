package com.hyodream.backend.payment.controller;

import com.hyodream.backend.payment.dto.PaymentResponseDto;
import com.hyodream.backend.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment API", description = "결제 내역 조회 및 관리")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 영수증 조회", description = "특정 주문 번호에 대한 결제 상세 내역을 조회합니다.")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponseDto> getPaymentInfo(
            @Parameter(description = "주문 ID") @PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentInfo(orderId));
    }
}