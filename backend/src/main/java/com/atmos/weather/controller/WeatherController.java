package com.atmos.weather.controller;

import com.atmos.weather.dto.WeatherDtos.*;
import com.atmos.weather.service.WeatherService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    private final WeatherService service;
    public WeatherController(WeatherService service) { this.service = service; }
    @GetMapping("/bundle/{city}") public Bundle bundle(@PathVariable String city) { return service.bundle(city); }
    @GetMapping("/coordinates") public Bundle coordinates(@RequestParam double lat, @RequestParam double lon) { return service.bundle(lat, lon); }
    @GetMapping("/current/{city}") public Current current(@PathVariable String city) { return service.bundle(city).current(); }
    @GetMapping("/hourly/{city}") public List<Hourly> hourly(@PathVariable String city) { return service.bundle(city).hourly(); }
    @GetMapping("/weekly/{city}") public List<Daily> weekly(@PathVariable String city) { return service.bundle(city).weekly(); }
    @GetMapping("/highlights/{city}") public Current highlights(@PathVariable String city) { return service.bundle(city).current(); }
    @GetMapping("/air-quality/{city}") public AirQuality air(@PathVariable String city) { return service.bundle(city).airQuality(); }
}
