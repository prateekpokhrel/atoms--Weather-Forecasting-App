package com.atmos.weather.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "search_history")
public class SearchHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String city;
    @Column(nullable = false) private String country;
    @Column(nullable = false) private Instant searchedAt;
    public SearchHistory() {}
    public SearchHistory(String city, String country) { this.city = city; this.country = country; this.searchedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public Instant getSearchedAt() { return searchedAt; }
}
