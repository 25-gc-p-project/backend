package com.hyodream.backend.product.controller;

import com.hyodream.backend.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    // 상품 클릭 이벤트 수집
    // POST http://localhost:8080/api/events/view
    @PostMapping("/view")
    public void logProductView(
            @RequestParam Long productId,
            @RequestParam String category, // 예: "관절염", "당뇨" (상품의 핵심 태그)
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId, // 비로그인용 식별자
            @RequestHeader(value = "Authorization", required = false) String token // 로그인용
    ) {
        String userId = sessionId; // 기본값은 세션ID

        // 👇👇 [수정] 토큰이 있으면 진짜 username을 꺼낸다!
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
        fields.put("timestamp", String.valueOf(System.currentTimeMillis()));

        RecordId id = redisTemplate.opsForStream()
                .add("product-view-stream", fields);

        System.out.println("Event Published for User: " + userId);
    }
}