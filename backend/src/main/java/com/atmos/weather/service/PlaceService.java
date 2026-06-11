package com.atmos.weather.service;

import com.atmos.weather.dto.CityRequest;
import com.atmos.weather.model.FavoriteCity;
import com.atmos.weather.model.SearchHistory;
import com.atmos.weather.repository.FavoriteCityRepository;
import com.atmos.weather.repository.SearchHistoryRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PlaceService {
    private final FavoriteCityRepository favorites;
    private final SearchHistoryRepository history;
    public PlaceService(FavoriteCityRepository favorites, SearchHistoryRepository history) {
        this.favorites = favorites; this.history = history;
    }
    public List<FavoriteCity> favorites() { return favorites.findAll(); }
    public FavoriteCity addFavorite(CityRequest r) { return favorites.save(new FavoriteCity(r.city(), r.country())); }
    public void deleteFavorite(Long id) { favorites.deleteById(id); }
    public List<SearchHistory> history() { return history.findTop20ByOrderBySearchedAtDesc(); }
    public SearchHistory addHistory(CityRequest r) { return history.save(new SearchHistory(r.city(), r.country())); }
    public void deleteHistory(Long id) { history.deleteById(id); }
    public void clearHistory() { history.deleteAll(); }
}
