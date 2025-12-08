package com.hyodream.backend.product.repository;

import com.hyodream.backend.product.domain.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchLogRepository extends JpaRepository<SearchLog, String> {
    // 기본 메서드(findById, save)로 충분함
}