package com.hyodream.backend.product.service;

import com.hyodream.backend.order.repository.OrderItemRepository;
import com.hyodream.backend.product.domain.*;
import com.hyodream.backend.product.dto.ReviewRequestDto;
import com.hyodream.backend.product.dto.ReviewResponseDto;
import com.hyodream.backend.product.repository.ProductRepository;
import com.hyodream.backend.product.repository.ReviewRepository;
import com.hyodream.backend.user.domain.User;
import com.hyodream.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserService userService;
    private final ProductRepository productRepository;
    private final ProductSyncService productSyncService;

    // 1. [내부] 리뷰 작성 (구매 인증 필요)
    @Transactional
    public void createReview(ReviewRequestDto dto) {
        User user = userService.getCurrentUser();

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 상품입니다."));

        // 구매 여부 확인
        if (!orderItemRepository.existsByUserIdAndProductId(user.getId(), dto.getProductId())) {
            throw new RuntimeException("상품을 구매한 사용자만 리뷰를 작성할 수 있습니다.");
        }

        // 중복 방지
        if (reviewRepository.existsByUserIdAndProductId(user.getId(), dto.getProductId())) {
            throw new RuntimeException("이미 리뷰를 작성하셨습니다.");
        }

        Review review = new Review();
        review.setUserId(user.getId());
        review.setProductId(dto.getProductId());
        review.setSource(ReviewSource.HYODREAM);
        review.setAuthorName(user.getUsername());
        review.setContent(dto.getContent());
        
        // 평점 처리
        int newScore = 5;
        if (dto.getScore() > 0) {
            newScore = dto.getScore();
        } else if (dto.getRating() != null) {
            newScore = (dto.getRating() == ReviewRating.GOOD ? 5 : (dto.getRating() == ReviewRating.AVERAGE ? 3 : 1));
            review.setRating(dto.getRating());
        }
        review.setScore(newScore);

        reviewRepository.save(review);
        
        // 통계 업데이트 & AI 분석 트리거
        updateProductStatsAndTriggerAnalysis(product, 1, newScore);
    }

    // 상품별 리뷰 조회
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByProductId(Long productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);

        String productName = productRepository.findById(productId)
                .map(Product::getName)
                .orElse("알 수 없는 상품");

        List<ReviewResponseDto> dtos = new ArrayList<>();
        for (Review review : reviews) {
            dtos.add(new ReviewResponseDto(review, productName));
        }
        return dtos;
    }

    // 리뷰 수정 (내부 회원만)
    @Transactional
    public void updateReview(Long reviewId, ReviewRequestDto dto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰가 없습니다."));
        User user = userService.getCurrentUser();

        // 본인 확인
        if (review.getUserId() == null || !review.getUserId().equals(user.getId())) {
            throw new RuntimeException("작성자만 수정할 수 있습니다.");
        }

        int oldScore = review.getScore();
        review.setContent(dto.getContent());
        
        if (dto.getScore() > 0) {
            review.setScore(dto.getScore());
        }
        
        // 점수가 바뀌었다면 통계 재계산 필요
        if (oldScore != review.getScore()) {
            Product product = productRepository.findById(review.getProductId()).orElse(null);
            if (product != null) {
                // 전체 재계산이 안전함 (증분 업데이트는 오차 누적 가능성 있음)
                recalculateProductStats(product);
            }
        }
    }

    // 리뷰 삭제
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰가 없습니다."));
        User user = userService.getCurrentUser();

        if (review.getUserId() == null || !review.getUserId().equals(user.getId())) {
            throw new RuntimeException("작성자만 삭제할 수 있습니다.");
        }

        reviewRepository.delete(review);
        
        Product product = productRepository.findById(review.getProductId()).orElse(null);
        if (product != null) {
             // 통계 업데이트 (감소)
             updateProductStatsAndTriggerAnalysis(product, -1, -review.getScore());
             // 혹은 정확성을 위해 전체 재계산
             recalculateProductStats(product);
        }
    }

    // 내가 쓴 리뷰 조회
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getMyReviews() {
        User user = userService.getCurrentUser();
        List<Review> reviews = reviewRepository.findByUserId(user.getId());

        List<ReviewResponseDto> dtos = new ArrayList<>();
        for (Review review : reviews) {
            String productName = productRepository.findById(review.getProductId())
                    .map(Product::getName)
                    .orElse("삭제된 상품");

            dtos.add(new ReviewResponseDto(review, productName));
        }
        return dtos;
    }

    // --- Private Methods ---

    private void updateProductStatsAndTriggerAnalysis(Product product, int countDelta, int scoreDelta) {
        long currentCount = product.getReviewCount() + countDelta;
        // countDelta가 양수일 때만 점수 합산, 삭제 시는 재계산 권장되지만 여기선 약식 구현 가능
        // 하지만 평균 평점 관리가 복잡하므로, 안전하게 DB에서 다시 집계하는 방식을 추천.
        // 여기서는 하이브리드로 처리: 카운트는 증감, 평점은 전체 재계산(정확도 위함)
        
        product.setReviewCount(Math.max(0, currentCount));
        recalculateProductStats(product); // 평점 및 카운트 정확도 보정

        // AI 재분석 트리거 (신규 리뷰가 5개 이상 쌓였을 때)
        ReviewAnalysis analysis = product.getAnalysis();
        int analyzedCount = (analysis != null) ? analysis.getAnalyzedReviewCount() : 0;
        
        if (product.getReviewCount() - analyzedCount >= 5 || analysis == null) {
            productSyncService.analyzeProductReviews(product.getId());
        }
    }
    
    private void recalculateProductStats(Product product) {
        // DB에서 전체 리뷰 수와 평점 평균을 다시 계산
        // (JPA 쿼리로 하거나 Java stream 처리)
        List<Review> allReviews = reviewRepository.findByProductId(product.getId());
        
        long count = allReviews.size();
        double avg = 0.0;
        if (count > 0) {
            double sum = allReviews.stream().mapToInt(Review::getScore).sum();
            avg = Math.round((sum / count) * 10.0) / 10.0; // 소수점 첫째자리 반올림
        }
        
        product.setReviewCount(count);
        product.setAverageRating(avg);
        productRepository.save(product);
    }
}
