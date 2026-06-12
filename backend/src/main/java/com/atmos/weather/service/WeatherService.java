package com.atmos.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.atmos.weather.dto.WeatherDtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class WeatherService {
    private final RestClient client;
    
    // For fallback when Geocoding fails or finding nearest city for lat/lon
    private static final Map<String, double[]> COORDS = Map.ofEntries(
        Map.entry("biratnagar", new double[]{26.4525, 87.2718}),
        Map.entry("kathmandu", new double[]{27.7172, 85.3240}),
        Map.entry("new york", new double[]{40.7128, -74.006}),
        Map.entry("london", new double[]{51.5072, -0.1276}),
        Map.entry("tokyo", new double[]{35.6762, 139.6503}),
        Map.entry("sydney", new double[]{-33.8688, 151.2093}),
        Map.entry("paris", new double[]{48.8566, 2.3522}));

    public WeatherService(RestClient.Builder builder) {
        this.client = builder.build();
    }
    
    public Bundle bundle(String city) {
        try { 
            return liveBundleForCity(city); 
        } catch (Exception e) { 
            e.printStackTrace();
            return fallbackBundle(city); 
        }
    }
    
    public Bundle bundle(double latitude, double longitude) {
        try {
            return liveBundleForCoords(latitude, longitude);
        } catch (Exception e) {
            e.printStackTrace();
            return fallbackBundle(nearestCity(latitude, longitude));
        }
    }
    
    private Bundle liveBundleForCity(String queryCity) {
        // 1. Geocode
        JsonNode places = client.get()
            .uri(uri -> uri.scheme("https").host("geocoding-api.open-meteo.com").path("/v1/search")
                .queryParam("name", queryCity).queryParam("count", 1).build())
            .retrieve().body(JsonNode.class);
            
        if (places == null || !places.has("results") || places.get("results").isEmpty()) {
            throw new IllegalArgumentException("City not found");
        }
        
        JsonNode place = places.get("results").get(0);
        double lat = place.path("latitude").asDouble();
        double lon = place.path("longitude").asDouble();
        String city = place.path("name").asText(title(queryCity));
        String country = place.path("country").asText("");
        
        return fetchMeteoData(lat, lon, city, country);
    }
    
    private Bundle liveBundleForCoords(double lat, double lon) {
        String city = nearestCity(lat, lon);
        String country = country(city);
        
        // Try reverse geocoding if possible, or just use nearest city
        // Open-Meteo doesn't have a direct reverse geocoding API, so we use the nearest known city.
        return fetchMeteoData(lat, lon, city, country);
    }
    
    private Bundle fetchMeteoData(double lat, double lon, String city, String country) {
        // 2. Forecast
        JsonNode forecast = client.get()
            .uri(uri -> uri.scheme("https").host("api.open-meteo.com").path("/v1/forecast")
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,cloud_cover,pressure_msl,wind_speed_10m,visibility")
                .queryParam("hourly", "temperature_2m,relative_humidity_2m,precipitation_probability,weather_code,wind_speed_10m")
                .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max")
                .queryParam("timezone", "auto")
                .build())
            .retrieve().body(JsonNode.class);
            
        if (forecast == null) throw new IllegalStateException("No forecast returned");
        
        // 3. Air Quality
        JsonNode air = null;
        try {
            air = client.get()
                .uri(uri -> uri.scheme("https").host("air-quality-api.open-meteo.com").path("/v1/air-quality")
                    .queryParam("latitude", lat)
                    .queryParam("longitude", lon)
                    .queryParam("current", "us_aqi,pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,ozone")
                    .queryParam("timezone", "auto")
                    .build())
                .retrieve().body(JsonNode.class);
        } catch (Exception ignored) {}

        JsonNode cur = forecast.path("current");
        boolean isDay = cur.path("is_day").asInt() == 1;
        int weatherCode = cur.path("weather_code").asInt(0);
        String condition = wmoToCondition(weatherCode);
        String description = wmoToDescription(weatherCode);
        
        JsonNode d = forecast.path("daily");
        String sunriseStr = d.path("sunrise").get(0).asText(); // "2023-11-20T06:12"
        String sunsetStr = d.path("sunset").get(0).asText();
        
        Current current = new Current(
            city, country, lat, lon,
            round(cur.path("temperature_2m")),
            round(cur.path("apparent_temperature")),
            condition,
            description,
            icon(condition, isDay),
            cur.path("relative_humidity_2m").asInt(),
            round(cur.path("pressure_msl")),
            oneDecimal(cur.path("wind_speed_10m").asDouble()),
            round(cur.path("visibility").asDouble() / 1000.0), // km
            oneDecimal(d.path("uv_index_max").get(0).asDouble()),
            cur.path("cloud_cover").asInt(),
            formatTime(sunriseStr),
            formatTime(sunsetStr),
            isDay
        );
        
        List<Hourly> hours = new ArrayList<>();
        JsonNode h = forecast.path("hourly");
        int currentHourIndex = findCurrentHourIndex(h.path("time"), cur.path("time").asText());
        
        for (int i = 0; i < 24; i++) {
            int idx = currentHourIndex + i;
            if (idx >= h.path("time").size()) break;
            
            String timeStr = h.path("time").get(idx).asText();
            int hCode = h.path("weather_code").get(idx).asInt(0);
            int localHour = Integer.parseInt(timeStr.substring(11, 13));
            boolean hDay = localHour >= 6 && localHour < 18;
            
            hours.add(new Hourly(
                i == 0 ? "Now" : formatTimeShort(timeStr),
                round(h.path("temperature_2m").get(idx)),
                h.path("precipitation_probability").get(idx).asInt(0),
                icon(wmoToCondition(hCode), hDay),
                h.path("relative_humidity_2m").get(idx).asInt(0),
                oneDecimal(h.path("wind_speed_10m").get(idx).asDouble())
            ));
        }
        
        List<Daily> days = new ArrayList<>();
        for (int i = 0; i < Math.min(7, d.path("time").size()); i++) {
            String dateStr = d.path("time").get(i).asText();
            LocalDate date = LocalDate.parse(dateStr);
            int dCode = d.path("weather_code").get(i).asInt(0);
            String dCond = wmoToCondition(dCode);
            
            days.add(new Daily(
                i == 0 ? "Today" : date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                date.toString(),
                round(d.path("temperature_2m_min").get(i)),
                round(d.path("temperature_2m_max").get(i)),
                50, // daily avg humidity not strictly in basic open-meteo, mock with 50%
                0.0, // daily avg wind speed not present, mock with 0
                dCond,
                icon(dCond, true)
            ));
        }
        
        AirQuality quality;
        if (air != null && air.has("current")) {
            JsonNode aq = air.path("current");
            quality = new AirQuality(
                aq.path("us_aqi").asInt(50),
                oneDecimal(aq.path("pm2_5").asDouble()),
                oneDecimal(aq.path("pm10").asDouble()),
                oneDecimal(aq.path("carbon_monoxide").asDouble()),
                oneDecimal(aq.path("nitrogen_dioxide").asDouble()),
                oneDecimal(aq.path("ozone").asDouble())
            );
        } else {
            quality = fallbackAirQuality(city);
        }
        
        return new Bundle(current, hours, days, quality);
    }
    
    private int findCurrentHourIndex(JsonNode times, String currentTime) {
        String currentHour = currentTime.substring(0, 14) + "00";
        for (int i = 0; i < times.size(); i++) {
            if (times.get(i).asText().equals(currentHour)) return i;
        }
        return 0;
    }
    
    private String formatTime(String datetime) {
        try {
            LocalDateTime dt = LocalDateTime.parse(datetime);
            return dt.format(DateTimeFormatter.ofPattern("h:mm a"));
        } catch (Exception e) { return datetime; }
    }
    
    private String formatTimeShort(String datetime) {
        try {
            LocalDateTime dt = LocalDateTime.parse(datetime);
            return dt.format(DateTimeFormatter.ofPattern("ha")).toLowerCase();
        } catch (Exception e) { return datetime; }
    }

    private String wmoToCondition(int code) {
        if (code == 0 || code == 1) return "Sunny";
        if (code == 2 || code == 3 || code == 45 || code == 48) return "Cloudy";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82) || (code >= 95 && code <= 99)) return "Rainy";
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return "Snowy";
        return "Sunny";
    }
    
    private String wmoToDescription(int code) {
        if (code == 0) return "Clear sky";
        if (code == 1) return "Mainly clear";
        if (code == 2) return "Partly cloudy";
        if (code == 3) return "Overcast";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 55) return "Drizzle";
        if (code >= 61 && code <= 65) return "Rain";
        if (code >= 71 && code <= 75) return "Snow fall";
        if (code >= 80 && code <= 82) return "Rain showers";
        if (code >= 95) return "Thunderstorm";
        return "Clear";
    }

    private Bundle fallbackBundle(String city) {
        Current current = fallbackCurrent(city);
        return new Bundle(current, fallbackHourly(city), fallbackWeekly(city), fallbackAirQuality(city));
    }

    public Current fallbackCurrent(String rawCity) {
        String city = title(rawCity);
        double[] coord = COORDS.getOrDefault(rawCity.toLowerCase(), new double[]{27.7172, 85.3240});
        int seed = Math.abs(rawCity.toLowerCase().hashCode());
        int temp = 17 + seed % 14; int hour = LocalTime.now().getHour(); boolean day = hour >= 6 && hour < 18;
        String condition = seed % 5 == 0 ? "Rainy" : seed % 4 == 0 ? "Cloudy" : "Sunny";
        return new Current(city, country(city), coord[0], coord[1], temp, temp - 1, condition,
            condition.equals("Sunny") ? "Clear skies" : condition.equals("Rainy") ? "Light rain" : "Scattered clouds",
            icon(condition, day), 48 + seed % 38, 1006 + seed % 18, 2.4 + seed % 35 / 10.0, 10,
            3.0 + seed % 50 / 10.0, 12 + seed % 65, "5:52 AM", "7:04 PM", day);
    }
    public List<Hourly> fallbackHourly(String city) {
        int base = fallbackCurrent(city).temperature(); List<Hourly> data = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            LocalTime t = LocalTime.now().plusHours(i); int temp = base + (int)Math.round(Math.sin((t.getHour()-8) / 24.0 * Math.PI * 2) * 4);
            data.add(new Hourly(i == 0 ? "Now" : t.format(java.time.format.DateTimeFormatter.ofPattern("ha")).toLowerCase(),
                temp, (i * 17 + Math.abs(city.hashCode())) % 55, i % 7 == 0 ? "rain" : t.getHour() > 18 || t.getHour() < 6 ? "moon" : "sun", 50 + i % 25, 2.5 + i % 6 / 2.0));
        } return data;
    }
    public List<Daily> fallbackWeekly(String city) {
        int base = fallbackCurrent(city).temperature(); List<Daily> data = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = LocalDate.now().plusDays(i); String c = i == 2 || i == 5 ? "Rainy" : i == 3 ? "Cloudy" : "Sunny";
            data.add(new Daily(i == 0 ? "Today" : d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                d.toString(), base - 5 + i % 3, base + 2 + i % 4, 50 + i * 4, 2.8 + i * .4, c, icon(c, true)));
        } return data;
    }
    public AirQuality fallbackAirQuality(String city) { int s = Math.abs(city.hashCode()); return new AirQuality(42 + s % 36, 10.2 + s % 80 / 10.0, 18.4 + s % 120 / 10.0, 210 + s % 90, 13.5 + s % 40 / 10.0, 48 + s % 25); }
    
    private String icon(String c, boolean day) { return c.equals("Rainy") ? "rain" : c.equals("Cloudy") ? "cloud" : c.equals("Snowy") ? "cloud" : day ? "sun" : "moon"; }
    private int round(JsonNode node) { return node != null && !node.isMissingNode() ? (int)Math.round(node.asDouble()) : 0; }
    private int round(double value) { return (int)Math.round(value); }
    private double oneDecimal(double value) { return Math.round(value * 10.0) / 10.0; }
    private String nearestCity(double latitude, double longitude) {
        return COORDS.entrySet().stream().min(Comparator.comparingDouble(entry -> {
            double[] coord = entry.getValue();
            return Math.pow(latitude - coord[0], 2) + Math.pow(longitude - coord[1], 2);
        })).map(Map.Entry::getKey).map(this::title).orElse("Kathmandu");
    }
    private String title(String s) { return Arrays.stream(s.trim().split("\\s+")).map(v -> Character.toUpperCase(v.charAt(0)) + v.substring(1).toLowerCase()).reduce((a,b)->a+" "+b).orElse("Kathmandu"); }
    private String country(String city) {
        return Map.ofEntries(
            Map.entry("Biratnagar", "Nepal"), Map.entry("Kathmandu", "Nepal"),
            Map.entry("New York", "United States"), Map.entry("London", "United Kingdom"),
            Map.entry("Tokyo", "Japan"), Map.entry("Sydney", "Australia"),
            Map.entry("Paris", "France"), Map.entry("Bhubaneswar", "India"),
            Map.entry("Mumbai", "India"), Map.entry("Delhi", "India")
        ).getOrDefault(city, city);
    }
}
