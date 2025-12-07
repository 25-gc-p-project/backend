package com.hyodream.backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hyodream.backend.payment.domain.Payment;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class PaymentResponseDto {
    private Long paymentId;
    private Long orderId;
    private int amount;
    private String paymentMethod;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentDate;
    
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