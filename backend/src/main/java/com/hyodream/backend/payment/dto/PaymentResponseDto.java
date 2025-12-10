package com.hyodream.backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hyodream.backend.payment.domain.Payment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class PaymentResponseDto {
    @Schema(description = "결제 ID", example = "5001")
    private Long paymentId;

    @Schema(description = "관련 주문 ID", example = "1001")
    private Long orderId;

    @Schema(description = "결제 금액", example = "196000")
    private int amount;

    @Schema(description = "결제 수단", example = "CARD")
    private String paymentMethod;

    @Schema(description = "결제 일시", example = "2025-12-10 14:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentDate;
    
    @Schema(description = "결제 상태", example = "COMPLETED")
    private String status; // 상태 추가

    public PaymentResponseDto(Payment payment) {
        this.paymentId = payment.getId();
        this.orderId = payment.getOrderId();
        this.amount = payment.getAmount();
        this.paymentMethod = payment.getPaymentMethod();
        this.paymentDate = payment.getPaymentDate();
        this.status = payment.getStatus().name();
    }
}