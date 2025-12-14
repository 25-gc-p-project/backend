package com.hyodream.backend.product.controller;

import com.hyodream.backend.global.util.JwtUtil;
import com.hyodream.backend.product.domain.EventType;
import com.hyodream.backend.product.naver.service.NaverShoppingService; // 서비스 Import
import com.hyodream.backend.product.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Event API", description = "사용자 행동 기반 이벤트 수집 및 실시간 관심사 분석")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    private final ProductRepository productRepository;
    // NaverShoppingService 내부에 static 메서드가 있으므로 주입이 필수는 아니지만,
    // static import를 쓰거나 클래스명으로 바로 접근합니다.

    @Operation(summary = "상품 클릭/조회 이벤트 수집", description = """
            사용자가 상품을 조회하거나 장바구니에 담는 등의 행동을 수집합니다.
            수집된 데이터는 Redis Stream으로 전송되어 실시간 관심사 분석(Real-time Recommendation)에 사용됩니다.
            
            **[관심사 키워드 추출 로직 (우선순위)]**
            1. **매칭 성공 (정확도 최상):** 상품의 카테고리명에서 유추한 효능이 실제 상품의 `healthBenefits` 태그에 포함된 경우.
               - 예: 카테고리 '루테인' -> 유추 '눈 건강' -> 상품 태그에 '눈 건강' 있음 -> **'눈 건강'** 추출
            2. **태그 존재 시:** 매칭되는 게 없으면, 상품의 첫 번째 효능 태그를 사용.
               - 예: 태그 `['기억력 개선', '눈 건강']` -> **'기억력 개선'** 추출
            3. **Fallback:** 위 경우에 해당하지 않으면, 가장 구체적인 **카테고리명**을 그대로 사용.
            
            **[Redis 저장 및 점수]**
            - 추출된 키워드는 Redis ZSet(`interest:user:{id}`)에 점수로 누적됩니다.
            - 이벤트 타입별 가중치: `CLICK(1.0)`, `LONG_VIEW(2.0)`, `CART(5.0)`, `ORDER(10.0)`
            """)
    @PostMapping("/view")
    public void logProductView(
            @Parameter(description = "상품 ID") @RequestParam Long productId,
            @Parameter(description = "이벤트 타입 (CLICK, CART, PURCHASE)") @RequestParam(defaultValue = "CLICK") EventType type,
            @Parameter(description = "비로그인 유저 세션 ID") @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Parameter(description = "로그인 유저 토큰") @RequestHeader(value = "Authorization", required = false) String token) {
        String targetCategory = "기타"; // 기본값
        var productOpt = productRepository.findById(productId);

        if (productOpt.isPresent()) {
            var p = productOpt.get();

            // 0. 가장 구체적인 카테고리 확보
            String categoryToAnalyze = null;
            if (p.getCategory4() != null && !p.getCategory4().isEmpty())
                categoryToAnalyze = p.getCategory4();
            else if (p.getCategory3() != null && !p.getCategory3().isEmpty())
                categoryToAnalyze = p.getCategory3();
            else if (p.getCategory2() != null && !p.getCategory2().isEmpty())
                categoryToAnalyze = p.getCategory2();
            else if (p.getCategory1() != null && !p.getCategory1().isEmpty())
                categoryToAnalyze = p.getCategory1();

            // 1순위: 카테고리에서 유추한 효능이 실제 상품 효능 목록에 포함되어 있는지 확인 (가장 정확)
            // BenefitUtils 대신 NaverShoppingService의 static 메서드 사용
            String deducedBenefit = (categoryToAnalyze != null)
                    ? NaverShoppingService.findPrimaryBenefit(categoryToAnalyze)
                    : null;

            if (deducedBenefit != null && p.getHealthBenefits() != null
                    && p.getHealthBenefits().contains(deducedBenefit)) {
                targetCategory = deducedBenefit;
            }
            // 2순위: 매칭 실패 시, 상품의 첫 번째 효능 사용
            else if (p.getHealthBenefits() != null && !p.getHealthBenefits().isEmpty()) {
                targetCategory = p.getHealthBenefits().get(0);
            }
            // 3순위 (단순 유추) 및 4순위 (카테고리명 그대로 사용) 로직
            // 요청하신대로 3순위(deducedBenefit만으로 결정)는 제거하고,
            // 효능을 찾지 못했다면 최후의 수단으로 카테고리 이름 자체를 사용
            else if (categoryToAnalyze != null) {
                targetCategory = categoryToAnalyze;
            }
        }

        String userId = sessionId;

        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);
            if (jwtUtil.validateToken(jwt)) {
                userId = jwtUtil.getUsername(jwt);
            }
        }

        if (userId == null)
            userId = "unknown";

        // Redis Stream에 이벤트 발행
        Map<String, String> fields = new HashMap<>();
        fields.put("userId", userId);
        fields.put("productId", productId.toString());
        fields.put("category", targetCategory);
        fields.put("type", type.name());
        fields.put("timestamp", String.valueOf(System.currentTimeMillis()));

        redisTemplate.opsForStream().add("product-view-stream", fields);

        System.out.println("Event [" + type + "] Published for: " + userId + ", Category: " + targetCategory);
    }
}