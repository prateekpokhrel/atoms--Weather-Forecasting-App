package com.atmos.weather.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "weather_cache")
public class WeatherCache {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String city; private double temperature; private int humidity; private int pressure;
    private double windSpeed; private String condition; private String icon; private Instant timestamp;
}
