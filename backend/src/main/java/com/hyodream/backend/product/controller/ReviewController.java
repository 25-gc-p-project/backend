package com.hyodream.backend.product.controller;

import com.hyodream.backend.product.dto.ReviewRequestDto;
import com.hyodream.backend.product.dto.ReviewResponseDto;
import com.hyodream.backend.product.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 작성 (로그인 필수)
    // POST http://localhost:8080/api/reviews
    @PostMapping
    public ResponseEntity<String> createReview(
            @RequestBody ReviewRequestDto dto
    ) {
        reviewService.createReview(dto);
        return ResponseEntity.ok("리뷰가 등록되었습니다.");
    }

    // 특정 상품의 리뷰 목록 조회 (누구나 가능)
    // GET http://localhost:8080/api/reviews/products/{productId}
    @GetMapping("/products/{productId}")
    public ResponseEntity<List<ReviewResponseDto>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProductId(productId));
    }

    // 내가 쓴 리뷰 조회
    // GET http://localhost:8080/api/reviews/my
    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponseDto>> getMyReviews() {
        // 서비스 호출
        List<ReviewResponseDto> myReviews = reviewService.getMyReviews();
        return ResponseEntity.ok(myReviews);
    }

    // 리뷰 수정
    // PUT http://localhost:8080/api/reviews/{reviewId}
    @PutMapping("/{reviewId}")
    public ResponseEntity<String> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewRequestDto dto) {
        reviewService.updateReview(reviewId, dto);
        return ResponseEntity.ok("리뷰가 수정되었습니다.");
    }

    // 리뷰 삭제
    // DELETE http://localhost:8080/api/reviews/{reviewId}
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<String> deleteReview(
            @PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok("리뷰가 삭제되었습니다.");
    }
}