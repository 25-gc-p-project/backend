package com.hyodream.backend.product.service;

import com.hyodream.backend.global.client.review.AiReviewClient;
import com.hyodream.backend.global.client.review.dto.ReviewAnalysisRequestDto;
import com.hyodream.backend.global.client.review.dto.ReviewAnalysisResponseDto;
import com.hyodream.backend.product.domain.AnalysisStatus;
import com.hyodream.backend.product.domain.Product;
import com.hyodream.backend.product.domain.Review;
import com.hyodream.backend.product.domain.ReviewAnalysis;
import com.hyodream.backend.product.repository.ProductRepository;
import com.hyodream.backend.product.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final AiReviewClient aiReviewClient;
    private final PlatformTransactionManager transactionManager;

    /**
     * [비동기] 상품 리뷰 AI 분석 수행
     * - DB에 저장된 리뷰를 가져와 AI 서버로 전송
     * - 분석 결과를 ReviewAnalysis 테이블에 저장
     */
    @Async
    public void analyzeProductReviews(Long productId) {
        log.info("🧠 [Async] Starting AI analysis for product ID: {}", productId);

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // 1. [Native Query] DB 레벨에서 원자적으로 분석 상태 선점 (status='PROGRESS')
        try {
            int updatedRows = txTemplate.execute(status -> productRepository.startSyncNative(productId));
            
            if (updatedRows > 0) {
                log.info("🏁 [Async] Acquired analysis lock for ID: {}", productId);
            } else {
                log.info("✋ [Async] Analysis already in progress for ID: {}. Skipping.", productId);
                return;
            }
        } catch (Exception e) {
            log.error("⚠️ [Async] DB Error during analysis setup: {}", e.getMessage());
            return;
        }

        try {
            // 2. [No Transaction] 리뷰 데이터 조회 및 AI 요청
            // 트랜잭션을 길게 잡지 않기 위해 여기서 수행
            List<Review> reviews = reviewRepository.findByProductId(productId);
            List<String> reviewContents = reviews.stream()
                    .map(Review::getContent)
                    .filter(c -> c != null && !c.isBlank())
                    .collect(Collectors.toList());

            if (reviewContents.isEmpty()) {
                log.info("ℹ️ [Async] No reviews to analyze for ID: {}", productId);
                txTemplate.execute(status -> {
                    completeSyncLogic(productId, null); // 빈 결과로 완료 처리
                    return null;
                });
                return;
            }

            // AI 서버 호출
            ReviewAnalysisResponseDto analysisResult = aiReviewClient.analyzeReviews(
                    new ReviewAnalysisRequestDto(reviewContents));

            // 3. [Transaction] 결과 저장
            txTemplate.execute(status -> {
                completeSyncLogic(productId, analysisResult);
                return null;
            });

        } catch (Exception e) {
            log.error("⚠️ [Async] Error during AI analysis: {}", e.getMessage());
            txTemplate.execute(status -> {
                failSyncLogic(productId);
                return null;
            });
        }
    }

    private void completeSyncLogic(Long productId, ReviewAnalysisResponseDto result) {
        // [Critical] 종료 시에도 비관적 락 사용 (데이터 정합성 보장)
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new RuntimeException("Product not found during completeSync"));

        ReviewAnalysis analysis = product.getAnalysis();
        if (analysis == null) {
            analysis = new ReviewAnalysis(product);
            product.setAnalysis(analysis);
        }

        if (result != null) {
            analysis.updateResult(
                    result.getPositiveCount(),
                    result.getNegativeCount(),
                    result.getPositivePercent(),
                    result.getNegativePercent(),
                    result.getTotalReviews()
            );
            log.info("✅ [Async] Analysis completed for ID: {} (Pos: {}%, Neg: {}%)", 
                    productId, result.getPositivePercent(), result.getNegativePercent());
        } else {
            // 리뷰가 없어서 분석 못한 경우도 COMPLETED로 처리하되 값은 0
            analysis.setStatus(AnalysisStatus.COMPLETED);
            analysis.setAnalyzedReviewCount(0);
            log.info("✅ [Async] Analysis completed (Empty) for ID: {}", productId);
        }

        productRepository.saveAndFlush(product);
    }

    private void failSyncLogic(Long productId) {
        try {
            Product product = productRepository.findByIdWithLock(productId).orElse(null);
            if (product != null) {
                ReviewAnalysis analysis = product.getAnalysis();
                if (analysis == null) {
                    analysis = new ReviewAnalysis(product);
                    product.setAnalysis(analysis);
                }
                
                // 이미 완료된 상태라면 덮어쓰지 않음
                if (analysis.getStatus() == AnalysisStatus.COMPLETED) return;
                
                analysis.setStatus(AnalysisStatus.FAILED);
                productRepository.saveAndFlush(product);
            }
        } catch (Exception e) {
            log.error("Failed to mark as FAILED: {}", e.getMessage());
        }
    }
}
