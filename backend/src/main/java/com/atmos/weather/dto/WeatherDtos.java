package com.atmos.weather.dto;

import java.util.List;

public final class WeatherDtos {
    private WeatherDtos() {}
    public record Current(String city, String country, double latitude, double longitude, int temperature,
        int feelsLike, String condition, String description, String icon, int humidity, int pressure,
        double windSpeed, int visibility, double uvIndex, int clouds, String sunrise, String sunset, boolean day) {}
    public record Hourly(String time, int temperature, int rainProbability, String icon, int humidity, double windSpeed) {}
    public record Daily(String day, String date, int min, int max, int humidity, double windSpeed, String condition, String icon) {}
    public record AirQuality(int aqi, double pm25, double pm10, double co, double no2, double o3) {}
    public record Bundle(Current current, List<Hourly> hourly, List<Daily> weekly, AirQuality airQuality) {}
}
