package com.hyodream.backend.product.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "review_analysis")
public class ReviewAnalysis {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    // 분석 상태
    @Enumerated(EnumType.STRING)
    private AnalysisStatus status = AnalysisStatus.NONE;

    // 분석 결과
    private int positiveCount = 0;
    private int negativeCount = 0;
    
    private double positiveRatio = 0.0;
    private double negativeRatio = 0.0;

    // 재분석 트리거용 통계
    private int analyzedReviewCount = 0; // 마지막 분석 당시의 리뷰 총 개수

    private LocalDateTime lastAnalyzedAt;

    public ReviewAnalysis(Product product) {
        this.product = product;
        this.productId = product.getId();
    }

    public void updateResult(int positiveCount, int negativeCount, double positiveRatio, double negativeRatio, int totalReviews) {
        this.positiveCount = positiveCount;
        this.negativeCount = negativeCount;
        this.positiveRatio = positiveRatio;
        this.negativeRatio = negativeRatio;
        this.analyzedReviewCount = totalReviews;
        this.lastAnalyzedAt = LocalDateTime.now();
        this.status = AnalysisStatus.COMPLETED;
    }
    
    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }
}
