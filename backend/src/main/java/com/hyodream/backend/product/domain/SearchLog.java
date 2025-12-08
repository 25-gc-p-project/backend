package com.hyodream.backend.product.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "search_logs")
public class SearchLog {

    @Id
    private String keyword; // 검색어 (PK)

    private LocalDateTime lastUpdatedAt; // 마지막으로 네이버 API에서 갱신한 시간

    // 편의 메서드
    public void updateTimestamp() {
        this.lastUpdatedAt = LocalDateTime.now();
    }
}