package com.hyodream.backend.product.controller;

import com.hyodream.backend.global.util.JwtUtil;
import com.hyodream.backend.product.domain.EventType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Event API", description = "사용자 행동(클릭, 장바구니 등) 이벤트 수집")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    @Operation(summary = "상품 클릭/조회 이벤트 수집", description = "사용자가 상품을 클릭하거나 장바구니에 담을 때 이벤트를 전송하여 실시간 추천 점수에 반영합니다.")
    @PostMapping("/view")
    public void logProductView(
            @Parameter(description = "상품 ID") @RequestParam Long productId,
            @Parameter(description = "상품 카테고리/태그 (예: 관절, 당뇨)") @RequestParam String category,
            @Parameter(description = "이벤트 타입 (CLICK, CART, PURCHASE)") @RequestParam(defaultValue = "CLICK") EventType type,
            @Parameter(description = "비로그인 유저 세션 ID") @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Parameter(description = "로그인 유저 토큰") @RequestHeader(value = "Authorization", required = false) String token
    ) {
        String userId = sessionId; // 기본값은 세션ID

        // 토큰이 있으면 진짜 username을 꺼냄
        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);
            if (jwtUtil.validateToken(jwt)) {
                userId = jwtUtil.getUsername(jwt); // "test_user"가 나옴
            }
        }

        if (userId == null)
            userId = "unknown";

        // Redis Stream에 이벤트 발행 (Producer)
        Map<String, String> fields = new HashMap<>();
        fields.put("userId", userId);
        fields.put("productId", productId.toString());
        fields.put("category", category);
        fields.put("type", type.name()); // 이벤트 타입 저장 (CLICK, CART 등)
        fields.put("timestamp", String.valueOf(System.currentTimeMillis()));

        redisTemplate.opsForStream().add("product-view-stream", fields);

        System.out.println("Event [" + type + "] Published for: " + userId);
    }
}