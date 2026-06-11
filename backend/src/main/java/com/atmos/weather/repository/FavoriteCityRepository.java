package com.atmos.weather.repository;
import com.atmos.weather.model.FavoriteCity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FavoriteCityRepository extends JpaRepository<FavoriteCity, Long> {}
