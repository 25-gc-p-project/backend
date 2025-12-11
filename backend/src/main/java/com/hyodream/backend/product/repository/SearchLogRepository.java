package com.hyodream.backend.product.repository;

import com.hyodream.backend.product.domain.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchLogRepository extends JpaRepository<SearchLog, String> {
    
    // API 호출한 지 오래된 키워드 찾기 (배치 작업용)
    @Query("SELECT s FROM SearchLog s WHERE s.lastApiCallAt < :cutoffDate")
    List<SearchLog> findByLastApiCallAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
