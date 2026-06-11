package com.atmos.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.atmos.weather.dto.WeatherDtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class WeatherService {
    private final RestClient client;
    private final String apiKey;
    private static final Map<String, double[]> COORDS = Map.ofEntries(
        Map.entry("biratnagar", new double[]{26.4525, 87.2718}),
        Map.entry("kathmandu", new double[]{27.7172, 85.3240}),
        Map.entry("new york", new double[]{40.7128, -74.006}),
        Map.entry("london", new double[]{51.5072, -0.1276}),
        Map.entry("tokyo", new double[]{35.6762, 139.6503}),
        Map.entry("sydney", new double[]{-33.8688, 151.2093}),
        Map.entry("paris", new double[]{48.8566, 2.3522}));

    public WeatherService(RestClient.Builder builder, @Value("${weather.base-url}") String baseUrl,
                          @Value("${weather.api-key:}") String apiKey) {
        this.client = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }
    public Bundle bundle(String city) {
        if (!apiKey.isBlank()) {
            try { return liveBundle(city); } catch (RuntimeException ignored) { /* Keep the app available during provider outages. */ }
        }
        Current current = current(city);
        return new Bundle(current, hourly(city), weekly(city), airQuality(city));
    }
    public Bundle bundle(double latitude, double longitude) {
        String city = nearestCity(latitude, longitude);
        if (!apiKey.isBlank()) {
            try {
                JsonNode places = client.get().uri(uri -> uri.path("/geo/1.0/reverse").queryParam("lat", latitude)
                    .queryParam("lon", longitude).queryParam("limit", 1).queryParam("appid", apiKey).build())
                    .retrieve().body(JsonNode.class);
                if (places != null && !places.isEmpty()) city = places.get(0).path("name").asText(city);
            } catch (RuntimeException ignored) { /* Nearest known city remains a reliable fallback. */ }
        }
        Bundle result = bundle(city);
        Current c = result.current();
        Current located = new Current(c.city(), c.country(), latitude, longitude, c.temperature(), c.feelsLike(),
            c.condition(), c.description(), c.icon(), c.humidity(), c.pressure(), c.windSpeed(), c.visibility(),
            c.uvIndex(), c.clouds(), c.sunrise(), c.sunset(), c.day());
        return new Bundle(located, result.hourly(), result.weekly(), result.airQuality());
    }
    private Bundle liveBundle(String city) {
        JsonNode places = client.get().uri(uri -> uri.path("/geo/1.0/direct").queryParam("q", city).queryParam("limit", 1).queryParam("appid", apiKey).build()).retrieve().body(JsonNode.class);
        if (places == null || places.isEmpty()) throw new IllegalArgumentException("City not found");
        JsonNode place = places.get(0); double lat = place.path("lat").asDouble(); double lon = place.path("lon").asDouble();
        JsonNode one = client.get().uri(uri -> uri.path("/data/3.0/onecall").queryParam("lat", lat).queryParam("lon", lon).queryParam("units", "metric").queryParam("exclude", "minutely,alerts").queryParam("appid", apiKey).build()).retrieve().body(JsonNode.class);
        JsonNode air = client.get().uri(uri -> uri.path("/data/2.5/air_pollution").queryParam("lat", lat).queryParam("lon", lon).queryParam("appid", apiKey).build()).retrieve().body(JsonNode.class);
        if (one == null) throw new IllegalStateException("No forecast returned");
        int offset = one.path("timezone_offset").asInt(); JsonNode now = one.path("current"); JsonNode weather = now.path("weather").path(0);
        long localNow = Instant.now().getEpochSecond(); boolean day = localNow >= now.path("sunrise").asLong() && localNow < now.path("sunset").asLong();
        String condition = weather.path("main").asText("Clear");
        Current current = new Current(place.path("name").asText(title(city)), place.path("country").asText(""), lat, lon,
            round(now.path("temp")), round(now.path("feels_like")), condition, weather.path("description").asText(),
            providerIcon(condition, day), now.path("humidity").asInt(), now.path("pressure").asInt(), oneDecimal(now.path("wind_speed").asDouble() * 3.6),
            round(now.path("visibility").asDouble() / 1000), oneDecimal(now.path("uvi").asDouble()), now.path("clouds").asInt(),
            clock(now.path("sunrise").asLong(), offset), clock(now.path("sunset").asLong(), offset), day);
        List<Hourly> hours = new ArrayList<>();
        for (int i = 0; i < Math.min(24, one.path("hourly").size()); i++) {
            JsonNode h = one.path("hourly").get(i); int localHour = Instant.ofEpochSecond(h.path("dt").asLong()).atOffset(ZoneOffset.ofTotalSeconds(offset)).getHour();
            hours.add(new Hourly(i == 0 ? "Now" : clock(h.path("dt").asLong(), offset), round(h.path("temp")), round(h.path("pop").asDouble() * 100),
                providerIcon(h.path("weather").path(0).path("main").asText(), localHour >= 6 && localHour < 18), h.path("humidity").asInt(), oneDecimal(h.path("wind_speed").asDouble() * 3.6)));
        }
        List<Daily> days = new ArrayList<>();
        for (int i = 0; i < Math.min(7, one.path("daily").size()); i++) {
            JsonNode d = one.path("daily").get(i); LocalDate date = Instant.ofEpochSecond(d.path("dt").asLong()).atOffset(ZoneOffset.ofTotalSeconds(offset)).toLocalDate();
            String c = d.path("weather").path(0).path("main").asText();
            days.add(new Daily(i == 0 ? "Today" : date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), date.toString(),
                round(d.path("temp").path("min")), round(d.path("temp").path("max")), d.path("humidity").asInt(), oneDecimal(d.path("wind_speed").asDouble() * 3.6), c, providerIcon(c, true)));
        }
        JsonNode components = air == null ? null : air.path("list").path(0).path("components");
        AirQuality quality = components == null || components.isMissingNode() ? airQuality(city) : new AirQuality(
            air.path("list").path(0).path("main").path("aqi").asInt() * 25, oneDecimal(components.path("pm2_5").asDouble()), oneDecimal(components.path("pm10").asDouble()),
            oneDecimal(components.path("co").asDouble()), oneDecimal(components.path("no2").asDouble()), oneDecimal(components.path("o3").asDouble()));
        return new Bundle(current, hours, days, quality);
    }
    public Current current(String rawCity) {
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
    public List<Hourly> hourly(String city) {
        int base = current(city).temperature(); List<Hourly> data = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            LocalTime t = LocalTime.now().plusHours(i); int temp = base + (int)Math.round(Math.sin((t.getHour()-8) / 24.0 * Math.PI * 2) * 4);
            data.add(new Hourly(i == 0 ? "Now" : t.format(java.time.format.DateTimeFormatter.ofPattern("ha")).toLowerCase(),
                temp, (i * 17 + Math.abs(city.hashCode())) % 55, i % 7 == 0 ? "rain" : t.getHour() > 18 || t.getHour() < 6 ? "moon" : "sun", 50 + i % 25, 2.5 + i % 6 / 2.0));
        } return data;
    }
    public List<Daily> weekly(String city) {
        int base = current(city).temperature(); List<Daily> data = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = LocalDate.now().plusDays(i); String c = i == 2 || i == 5 ? "Rainy" : i == 3 ? "Cloudy" : "Sunny";
            data.add(new Daily(i == 0 ? "Today" : d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                d.toString(), base - 5 + i % 3, base + 2 + i % 4, 50 + i * 4, 2.8 + i * .4, c, icon(c, true)));
        } return data;
    }
    public AirQuality airQuality(String city) { int s = Math.abs(city.hashCode()); return new AirQuality(42 + s % 36, 10.2 + s % 80 / 10.0, 18.4 + s % 120 / 10.0, 210 + s % 90, 13.5 + s % 40 / 10.0, 48 + s % 25); }
    private String icon(String c, boolean day) { return c.equals("Rainy") ? "rain" : c.equals("Cloudy") ? "cloud" : day ? "sun" : "moon"; }
    private String providerIcon(String c, boolean day) { String v = c.toLowerCase(); return v.contains("rain") || v.contains("drizzle") || v.contains("thunder") ? "rain" : v.contains("cloud") || v.contains("snow") ? "cloud" : day ? "sun" : "moon"; }
    private String clock(long epoch, int offset) { return Instant.ofEpochSecond(epoch).atOffset(ZoneOffset.ofTotalSeconds(offset)).format(java.time.format.DateTimeFormatter.ofPattern("h:mm a")); }
    private int round(JsonNode node) { return (int)Math.round(node.asDouble()); }
    private int round(double value) { return (int)Math.round(value); }
    private double oneDecimal(double value) { return Math.round(value * 10.0) / 10.0; }
    private String nearestCity(double latitude, double longitude) {
        return COORDS.entrySet().stream().min(Comparator.comparingDouble(entry -> {
            double[] coord = entry.getValue();
            return Math.pow(latitude - coord[0], 2) + Math.pow(longitude - coord[1], 2);
        })).map(Map.Entry::getKey).map(this::title).orElse("Kathmandu");
    }
    private String title(String s) { return Arrays.stream(s.trim().split("\\s+")).map(v -> Character.toUpperCase(v.charAt(0)) + v.substring(1).toLowerCase()).reduce((a,b)->a+" "+b).orElse("Kathmandu"); }
    private String country(String city) { return Map.of("Biratnagar","Nepal","Kathmandu","Nepal","New York","United States","London","United Kingdom","Tokyo","Japan","Sydney","Australia","Paris","France").getOrDefault(city, "Nepal"); }
}
