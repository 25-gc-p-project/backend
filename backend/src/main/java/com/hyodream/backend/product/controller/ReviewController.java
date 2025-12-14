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

@Tag(name = "Review API", description = "상품 리뷰 등록 및 AI 분석 연동 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 작성 (내부 회원용)", description = """
            효드림 쇼핑몰에서 상품을 **직접 구매한 회원**만 작성할 수 있습니다.
            
            **[작성 조건]**
            - **로그인:** 필수
            - **구매 이력:** 해당 상품의 구매 내역(`OrderItem`)이 존재해야 합니다.
            - **중복 검사:** 동일 상품에 대해 중복으로 리뷰를 작성할 수 없습니다.
            
            **[AI 연동: 자동 재분석 트리거]**
            - 리뷰 작성 시 해당 상품의 통계(리뷰 수, 평점)가 즉시 업데이트됩니다.
            - **분석 조건:** 마지막 AI 분석 이후 **새로운 리뷰가 5개 이상** 쌓이면, 비동기(`Async`)로 AI 서버에 리뷰 감성 재분석 요청을 전송합니다.
            """)
    @PostMapping
    public ResponseEntity<String> createReview(
            @RequestBody ReviewRequestDto dto
    ) {
        reviewService.createReview(dto);
        return ResponseEntity.ok("리뷰가 등록되었습니다.");
    }

    @Operation(summary = "상품별 리뷰 목록 조회", description = """
            특정 상품에 달린 모든 리뷰를 최신순으로 조회합니다.
            
            **[데이터 출처]**
            - **HYODREAM:** 내부 회원이 직접 작성한 리뷰
            - **NAVER:** 네이버 쇼핑에서 크롤링한 외부 리뷰 (작성자 ID 마스킹 처리됨)
            """)
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

    @Operation(summary = "리뷰 수정", description = """
            자신이 작성한 리뷰의 내용을 수정합니다.
            
            **[로직]**
            - 평점(`score`)이 수정된 경우, 상품의 전체 평균 평점을 다시 계산하여 정확도를 유지합니다.
            """)
    @PutMapping("/{reviewId}")
    public ResponseEntity<String> updateReview(
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
            @RequestBody ReviewRequestDto dto) {
        reviewService.updateReview(reviewId, dto);
        return ResponseEntity.ok("리뷰가 수정되었습니다.");
    }

    @Operation(summary = "리뷰 삭제", description = """
            자신이 작성한 리뷰를 삭제합니다.
            
            **[로직]**
            - 리뷰 삭제 시 상품의 리뷰 카운트와 평균 평점이 갱신(감소/재계산)됩니다.
            """)
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<String> deleteReview(
            @Parameter(description = "리뷰 ID") @PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok("리뷰가 삭제되었습니다.");
    }
}