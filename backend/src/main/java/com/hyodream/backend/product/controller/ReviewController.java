package com.hyodream.backend.product.controller;

import com.hyodream.backend.product.dto.ReviewRequestDto;
import com.hyodream.backend.product.dto.ReviewResponseDto;
import com.hyodream.backend.product.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Review API", description = "상품 리뷰 등록, 조회, 수정, 삭제")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 작성", description = "구매한 상품에 대해 별점과 내용을 남깁니다. (로그인 필수)")
    @PostMapping
    public ResponseEntity<String> createReview(
            @RequestBody ReviewRequestDto dto
    ) {
        reviewService.createReview(dto);
        return ResponseEntity.ok("리뷰가 등록되었습니다.");
    }

    @Operation(summary = "상품별 리뷰 목록 조회", description = "특정 상품에 달린 모든 리뷰를 조회합니다.")
    @GetMapping("/products/{productId}")
    public ResponseEntity<List<ReviewResponseDto>> getProductReviews(
            @Parameter(description = "상품 ID") @PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProductId(productId));
    }

    @Operation(summary = "내가 쓴 리뷰 조회", description = "현재 로그인한 사용자가 작성한 모든 리뷰를 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponseDto>> getMyReviews() {
        // 서비스 호출
        List<ReviewResponseDto> myReviews = reviewService.getMyReviews();
        return ResponseEntity.ok(myReviews);
    }

    @Operation(summary = "리뷰 수정", description = "자신이 작성한 리뷰의 내용을 수정합니다.")
    @PutMapping("/{reviewId}")
    public ResponseEntity<String> updateReview(
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
            @RequestBody ReviewRequestDto dto) {
        reviewService.updateReview(reviewId, dto);
        return ResponseEntity.ok("리뷰가 수정되었습니다.");
    }

    @Operation(summary = "리뷰 삭제", description = "자신이 작성한 리뷰를 삭제합니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<String> deleteReview(
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok("리뷰가 삭제되었습니다.");
    }
}