package com.hyodream.backend.payment.controller;

import com.hyodream.backend.payment.dto.PaymentResponseDto;
import com.hyodream.backend.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment API", description = "결제 내역 및 영수증 조회 API")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 영수증 조회", description = """
            특정 주문 번호(`orderId`)에 대한 상세 결제 내역을 조회합니다.
            
            **[반환 데이터]**
            - 결제 금액, 수단(CARD/CASH), 결제 일시
            - 결제 상태 (`DONE`: 결제 완료, `CANCELED`: 취소/환불)
            """)
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponseDto> getPaymentInfo(
            @Parameter(description = "주문 ID") @PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentInfo(orderId));
    }
}