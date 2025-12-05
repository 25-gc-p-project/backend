package com.hyodream.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.hyodream.backend.product.service.StreamConsumer;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    // 리스너 컨테이너 설정 (Consumer 등록)
    @Bean
    public org.springframework.data.redis.stream.StreamMessageListenerContainer<String, org.springframework.data.redis.connection.stream.MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory, StreamConsumer streamConsumer) {

        var options = org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(java.time.Duration.ofMillis(100)) // 0.1초마다 확인
                .build();

        var container = org.springframework.data.redis.stream.StreamMessageListenerContainer.create(connectionFactory,
                options);

        // "product-view-stream"을 구독하겠다!
        container.receive(
                org.springframework.data.redis.connection.stream.StreamOffset.create("product-view-stream",
                        org.springframework.data.redis.connection.stream.ReadOffset.latest()),
                streamConsumer);

        container.start(); // 컨테이너 시작
        return container;
    }
}