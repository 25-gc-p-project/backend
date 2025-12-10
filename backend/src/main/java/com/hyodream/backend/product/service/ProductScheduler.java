package com.hyodream.backend.product.service;

import com.hyodream.backend.order.repository.OrderItemRepository;
import com.hyodream.backend.product.domain.Product;
import com.hyodream.backend.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import com.hyodream.backend.product.domain.ProductStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductScheduler {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    // 매일 자정(0시 0분 0초)에 실행
    // cron = "초 분 시 일 월 요일"
    // 테스트용: @Scheduled(cron = "0/10 * * * * *") - 10초에 한번
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    @Transactional
    public void updateRecentSales() {
        System.out.println("🔄 [스케줄러] 최근 한 달 판매량 업데이트 시작...");

        // 모든 상품의 recentSales를 일단 0으로 초기화 (안 팔린 건 0이어야 하니까)
        List<Product> allProducts = productRepository.findAll();
        for (Product p : allProducts) {
            p.setRecentSales(0);
        }

        // 최근 30일간 판매 데이터 집계
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);
        List<Object[]> salesData = orderItemRepository.countSalesByProductSince(oneMonthAgo);

        // 상품 정보 업데이트
        for (Object[] row : salesData) {
            Long productId = (Long) row[0];
            Long countLong = (Long) row[1]; // DB 결과는 Long으로 나옴
            int count = countLong.intValue();

            productRepository.findById(productId).ifPresent(product -> {
                product.setRecentSales(count);
            });
        }

        System.out.println("✅ [스케줄러] 업데이트 완료!");
    }

    // [New] 매일 새벽 4시에 오래된 상품 정리 (Garbage Collection)
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupOldProducts() {
        System.out.println("🧹 [스케줄러] 오래된 상품 정리 시작...");
        
        // 30일 이상 업데이트 안 된 상품 조회
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        List<Product> oldProducts = productRepository.findByUpdatedAtBefore(threshold);

        int deletedCount = 0;
        int stoppedCount = 0;

        for (Product p : oldProducts) {
            if (p.getTotalSales() > 0) {
                // 판매 이력이 있으면 지우지 않고 '판매 중지' 처리 (주문 내역 보존)
                if (p.getStatus() != ProductStatus.STOP_SELLING) {
                    p.setStatus(ProductStatus.STOP_SELLING);
                    stoppedCount++;
                }
            } else {
                // 판매 이력이 없으면 과감하게 삭제 (DB 용량 확보)
                productRepository.delete(p);
                deletedCount++;
            }
        }

        System.out.printf("✅ [스케줄러] 정리 완료! (삭제: %d건, 판매중지: %d건)%n", deletedCount, stoppedCount);
    }
}