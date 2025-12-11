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

@Tag(name = "Review API", description = "상품 리뷰 등록, 조회, 수정, 삭제 (내부 회원 + 외부 크롤링 리뷰)")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 작성 (내부 회원용)", description = """
            효드림 쇼핑몰에서 상품을 **직접 구매한 회원**만 작성할 수 있습니다.
            - **필수 조건:** 로그인 상태, 해당 상품 구매 이력 존재(`OrderItem`).
            - **입력:** 평점(score, 1~5)과 내용, 이미지를 입력받습니다.
            - **검증:** 중복 작성 여부 및 구매 여부를 엄격하게 검증합니다.
            """)
    @PostMapping
    public ResponseEntity<String> createReview(
            @RequestBody ReviewRequestDto dto
    ) {
        reviewService.createReview(dto);
        return ResponseEntity.ok("리뷰가 등록되었습니다.");
    }

    @Operation(summary = "외부/크롤링 리뷰 저장 (시스템/AI 전용)", description = """
            네이버 쇼핑 등 외부에서 수집한 리뷰 데이터를 저장하는 엔드포인트입니다.
            **일반 사용자는 호출하지 않습니다.**

            - **특징:** 구매 인증 절차를 건너뛰고(`skip purchase verification`), 외부 ID(`externalReviewId`)를 통해 중복 저장을 방지합니다.
            - **용도:** 상품 상세 조회 시 AI 크롤러가 수집한 최신 리뷰를 DB에 적재할 때 사용됩니다.
            """)
    @PostMapping("/crawled")
    public ResponseEntity<String> saveCrawledReview(
            @RequestBody ReviewRequestDto dto
    ) {
        reviewService.saveCrawledReview(dto);
        return ResponseEntity.ok("외부 리뷰가 저장되었습니다.");
    }

    @Operation(summary = "상품별 리뷰 목록 조회", description = "특정 상품에 달린 모든 리뷰(내부 회원 + 외부 크롤링)를 최신순으로 조회합니다.")
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