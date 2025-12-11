package com.hyodream.backend.product.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 내부 회원은 필수, 외부 리뷰(네이버)는 Null 가능
    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    // 리뷰 출처 (기본값: HYODREAM)
    @Enumerated(EnumType.STRING)
    private ReviewSource source = ReviewSource.HYODREAM;

    // 외부 리뷰 식별자 (네이버 리뷰 ID 등, 중복 수집 방지용)
    @Column(name = "external_review_id", unique = true)
    private String externalReviewId;

    // 작성자 이름 (내부: 닉네임, 외부: 마스킹된 ID)
    private String authorName;

    // 구매 옵션 (예: "색상: 블랙 / 사이즈: L")
    private String productOption;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String content;

    // 숫자 평점 (1~5점) - 평균 평점 계산용
    private int score;

    // 기존 감성 평점 (GOOD/BAD) - UI 표시용 (score 기반 자동 설정 가능)
    @Enumerated(EnumType.STRING)
    private ReviewRating rating;

    // 리뷰 첨부 이미지 URL 리스트
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "review_images", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url")
    private List<String> images = new ArrayList<>();

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        // score가 있는데 rating이 없으면 자동 설정
        if (this.rating == null && this.score > 0) {
            if (this.score >= 4) {
                this.rating = ReviewRating.GOOD;
            } else if (this.score == 3) {
                this.rating = ReviewRating.AVERAGE;
            } else {
                this.rating = ReviewRating.BAD;
            }
        }
    }
}
