package com.atmos.weather.controller;

import com.atmos.weather.dto.CityRequest;
import com.atmos.weather.model.FavoriteCity;
import com.atmos.weather.model.SearchHistory;
import com.atmos.weather.service.PlaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PlaceController {
    private final PlaceService service;
    public PlaceController(PlaceService service) { this.service = service; }
    @GetMapping("/favorites") public List<FavoriteCity> favorites() { return service.favorites(); }
    @PostMapping("/favorites") @ResponseStatus(HttpStatus.CREATED) public FavoriteCity favorite(@Valid @RequestBody CityRequest r) { return service.addFavorite(r); }
    @DeleteMapping("/favorites/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteFavorite(@PathVariable Long id) { service.deleteFavorite(id); }
    @GetMapping("/history") public List<SearchHistory> history() { return service.history(); }
    @PostMapping("/history") @ResponseStatus(HttpStatus.CREATED) public SearchHistory history(@Valid @RequestBody CityRequest r) { return service.addHistory(r); }
    @DeleteMapping("/history/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteHistory(@PathVariable Long id) { service.deleteHistory(id); }
    @DeleteMapping("/history") @ResponseStatus(HttpStatus.NO_CONTENT) public void clearHistory() { service.clearHistory(); }
}
