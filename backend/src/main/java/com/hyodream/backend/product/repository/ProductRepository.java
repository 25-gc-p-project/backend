package com.hyodream.backend.product.repository;

import com.hyodream.backend.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // 나중에 "당뇨" 태그 가진 상품 찾을 때 씀
    // (JPA가 알아서 만들어줌)
    List<Product> findByHealthBenefitsContaining(String benefit);

    // 상품명 검색(페이징)
    Page<Product> findByNameContaining(String keyword, Pageable pageable);

    // 커스텀 정렬 & 필터링 쿼리
    // 1. Filtering: 내 알레르기 리스트(:userAllergies)에 포함된 성분이 하나라도 있으면 제외
    // 2. Sorting: 내 관심사(:interest)가 healthBenefits에 포함되면 우선순위 0 (상단), 아니면 1 (하단) ->
    // 그 뒤엔 ID 최신순
    @Query("SELECT DISTINCT p FROM Product p " +
            "WHERE (:isLogin = false OR NOT EXISTS (SELECT 1 FROM p.allergens a WHERE a IN :userAllergies)) " +
            "ORDER BY CASE WHEN :interest MEMBER OF p.healthBenefits THEN 0 ELSE 1 END, p.id DESC")
    Page<Product> findAllWithPersonalization(
            @Param("isLogin") boolean isLogin,
            @Param("userAllergies") List<String> userAllergies,
            @Param("interest") String interest,
            Pageable pageable);
}