package com.hyodream.backend.product.service;

import com.hyodream.backend.order.repository.OrderItemRepository;
import com.hyodream.backend.product.domain.Product;
import com.hyodream.backend.product.domain.Review;
import com.hyodream.backend.product.domain.ReviewRating;
import com.hyodream.backend.product.domain.ReviewSource;
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

    // 1. [내부] 리뷰 작성 (구매 인증 필요)
    @Transactional
    public void createReview(ReviewRequestDto dto) {
        User user = userService.getCurrentUser();

        // 상품 존재 확인
        if (!productRepository.existsById(dto.getProductId())) {
            throw new RuntimeException("존재하지 않는 상품입니다.");
        }

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
        
        // 아이디를 작성자 이름으로 저장
        review.setAuthorName(user.getUsername());

        review.setContent(dto.getContent());
        
        // 평점 처리 (Score 우선)
        if (dto.getScore() > 0) {
            review.setScore(dto.getScore());
            // rating은 prePersist에서 자동 설정됨
        } else if (dto.getRating() != null) {
            // 구버전 호환: rating만 들어오면 임의의 점수 할당
            review.setRating(dto.getRating());
            review.setScore(dto.getRating() == ReviewRating.GOOD ? 5 : (dto.getRating() == ReviewRating.AVERAGE ? 3 : 1));
        } else {
            // 기본값
            review.setScore(5);
        }

        reviewRepository.save(review);
    }

    // 2. [외부/크롤러] 리뷰 저장 (구매 인증 Skip)
    @Transactional
    public void saveCrawledReview(ReviewRequestDto dto) {
        // 상품 존재 확인 (없으면 저장 불가)
        if (!productRepository.existsById(dto.getProductId())) {
            log.warn("Cannot save review for non-existent product ID: {}", dto.getProductId());
            return;
        }

        // 중복 수집 방지
        if (dto.getExternalReviewId() != null && 
            reviewRepository.existsByExternalReviewId(dto.getExternalReviewId())) {
            return; // 이미 저장된 리뷰
        }

        Review review = new Review();
        review.setProductId(dto.getProductId());
        review.setSource(ReviewSource.NAVER);
        review.setUserId(null); // 외부 리뷰는 회원 ID 없음

        review.setExternalReviewId(dto.getExternalReviewId());
        review.setAuthorName(dto.getAuthorName()); // 마스킹된 이름
        review.setProductOption(dto.getProductOption());
        review.setContent(dto.getContent());
        review.setImages(dto.getImages());
        
        // 점수 저장
        review.setScore(dto.getScore());
        // rating은 prePersist에서 자동 계산

        reviewRepository.save(review);
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

        review.setContent(dto.getContent());
        if (dto.getScore() > 0) {
            review.setScore(dto.getScore());
            // rating은 자동 업데이트가 안되므로 수동 갱신 필요할 수 있음.
            // 여기서는 단순화를 위해 생략하거나 로직 추가 가능.
        }
    }

    // 리뷰 삭제
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰가 없습니다."));
        User user = userService.getCurrentUser();

        // 본인 확인
        if (review.getUserId() == null || !review.getUserId().equals(user.getId())) {
            throw new RuntimeException("작성자만 삭제할 수 있습니다.");
        }

        reviewRepository.delete(review);
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
}
