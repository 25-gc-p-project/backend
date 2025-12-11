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

    // 사용자가 이 키워드를 검색한 마지막 시간 (인기도/최근검색용)
    private LocalDateTime lastSearchedAt;

    // 네이버 API를 호출하여 데이터를 갱신한 마지막 시간 (데이터 최신성용)
    private LocalDateTime lastApiCallAt;

    // 사용자가 검색했을 때 호출
    public void recordSearch() {
        this.lastSearchedAt = LocalDateTime.now();
    }
    
    // API 호출했을 때 호출
    public void recordApiCall() {
        this.lastApiCallAt = LocalDateTime.now();
    }
}
