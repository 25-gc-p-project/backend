package com.hyodream.backend.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StreamConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final StringRedisTemplate redisTemplate;

    // 스트림에서 메시지가 오면 실행되는 함수
    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Map<String, String> event = message.getValue();
        String userId = event.get("userId");
        String category = event.get("category"); // 예: "관절염"

        System.out.println("Event Consumed: User " + userId + " clicked " + category);

        // 실시간 분석 로직: 유저별 카테고리 점수 누적 (ZSET 사용)
        // Key: "interest:user:세션ID"
        // Value: 카테고리명, Score: 클릭 횟수
        String key = "interest:user:" + userId;
        redisTemplate.opsForZSet().incrementScore(key, category, 1.0);

        // 데이터는 1시간 뒤 자동 만료 (단기 관심사니까)
        redisTemplate.expire(key, Duration.ofHours(1));
    }
}