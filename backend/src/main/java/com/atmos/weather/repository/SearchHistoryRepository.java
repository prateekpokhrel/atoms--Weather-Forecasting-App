package com.atmos.weather.repository;
import com.atmos.weather.model.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    List<SearchHistory> findTop20ByOrderBySearchedAtDesc();
}
