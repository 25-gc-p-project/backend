package com.hyodream.backend.product.dto;

import com.hyodream.backend.product.domain.Review;
import com.hyodream.backend.product.domain.ReviewSource;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ReviewResponseDto {
    private Long id;
    private Long productId;
    private String productName;
    private Long userId; // null일 수 있음 (외부 리뷰)
    
    private String authorName; // 작성자 이름 (닉네임 or 마스킹 ID)
    private ReviewSource source; // 출처 (HYODREAM, NAVER)
    private String productOption; // 구매 옵션
    
    private String content;
    private int score; // 1~5점
    private String rating; // "좋아요" (한글)
    private String ratingCode; // "GOOD" (코드)
    
    private List<String> images; // 첨부 이미지
    
    private LocalDateTime createdAt;

    public ReviewResponseDto(Review review, String productName) {
        this.id = review.getId();
        this.productId = review.getProductId();
        this.productName = productName;
        this.userId = review.getUserId();
        
        this.authorName = review.getAuthorName();
        this.source = review.getSource();
        this.productOption = review.getProductOption();
        
        this.content = review.getContent();
        this.score = review.getScore();
        
        // Null Safety
        if (review.getRating() != null) {
            this.rating = review.getRating().getDescription();
            this.ratingCode = review.getRating().name();
        } else {
            this.rating = "평가 없음";
            this.ratingCode = "NONE";
        }
        
        this.images = review.getImages();
        this.createdAt = review.getCreatedAt();
    }
}
