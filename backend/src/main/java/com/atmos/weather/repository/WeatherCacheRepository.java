package com.atmos.weather.repository;
import com.atmos.weather.model.WeatherCache;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WeatherCacheRepository extends JpaRepository<WeatherCache, Long> {}
