package com.atmos.weather.dto;
import jakarta.validation.constraints.NotBlank;
public record CityRequest(@NotBlank String city, @NotBlank String country) {}
