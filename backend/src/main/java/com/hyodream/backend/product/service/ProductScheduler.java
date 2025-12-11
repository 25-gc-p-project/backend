package com.hyodream.backend.product.service;

import com.hyodream.backend.order.repository.OrderItemRepository;
import com.hyodream.backend.product.domain.Product;
import com.hyodream.backend.product.domain.ProductStatus;
import com.hyodream.backend.product.domain.SearchLog;
import com.hyodream.backend.product.naver.service.NaverShoppingService;
import com.hyodream.backend.product.repository.ProductRepository;
import com.hyodream.backend.product.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductScheduler {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final SearchLogRepository searchLogRepository;
    private final NaverShoppingService naverShoppingService;

    // 매일 자정: 최근 판매량 집계
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void updateRecentSales() {
        log.info("🔄 [스케줄러] 최근 한 달 판매량 업데이트 시작...");

        List<Product> allProducts = productRepository.findAll();
        for (Product p : allProducts) {
            p.setRecentSales(0);
        }

        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);
        List<Object[]> salesData = orderItemRepository.countSalesByProductSince(oneMonthAgo);

        for (Object[] row : salesData) {
            Long productId = (Long) row[0];
            Long countLong = (Long) row[1];
            int count = countLong.intValue();

            productRepository.findById(productId).ifPresent(product -> {
                product.setRecentSales(count);
            });
        }
        log.info("✅ [스케줄러] 판매량 업데이트 완료!");
    }

    // 매일 새벽 3시: 오래된 검색어 재검색 (데이터 최신화)
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void refreshOldKeywords() {
        log.info("🔄 [스케줄러] 오래된 검색어 데이터 최신화 시작...");
        
        // API 호출한 지 3일 지난 검색어 조회
        LocalDateTime threshold = LocalDateTime.now().minusDays(3);
        List<SearchLog> oldLogs = searchLogRepository.findByLastApiCallAtBefore(threshold);

        int updatedCount = 0;
        for (SearchLog logEntry : oldLogs) {
            try {
                // API 호출 및 DB 갱신
                naverShoppingService.importNaverProducts(logEntry.getKeyword());
                
                // 시간 갱신
                logEntry.recordApiCall();
                updatedCount++;
                
                // API 호출 제한 고려하여 약간의 딜레이 (선택사항)
                Thread.sleep(100); 
                
            } catch (Exception e) {
                log.error("Failed to refresh keyword: {}", logEntry.getKeyword(), e);
            }
        }
        log.info("✅ [스케줄러] {}개 키워드 최신화 완료!", updatedCount);
    }

    // 매일 새벽 4시: 오랫동안 업데이트 안 된 상품 정리 (Garbage Collection)
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupOldProducts() {
        log.info("🧹 [스케줄러] 오래된 상품 정리 시작...");
        
        // 30일 이상 업데이트 안 된 상품 조회
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        List<Product> oldProducts = productRepository.findByUpdatedAtBefore(threshold);

        int deletedCount = 0;
        int stoppedCount = 0;

        for (Product p : oldProducts) {
            if (p.getTotalSales() > 0) {
                if (p.getStatus() != ProductStatus.STOP_SELLING) {
                    p.setStatus(ProductStatus.STOP_SELLING);
                    stoppedCount++;
                }
            } else {
                productRepository.delete(p);
                deletedCount++;
            }
        }
        log.info("✅ [스케줄러] 정리 완료! (삭제: {}건, 판매중지: {}건)", deletedCount, stoppedCount);
    }
}
