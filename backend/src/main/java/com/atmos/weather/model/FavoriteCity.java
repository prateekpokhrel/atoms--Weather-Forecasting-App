package com.atmos.weather.model;

import jakarta.persistence.*;

@Entity
@Table(name = "favorite_city", uniqueConstraints = @UniqueConstraint(columnNames = {"city", "country"}))
public class FavoriteCity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String city;
    @Column(nullable = false) private String country;
    public FavoriteCity() {}
    public FavoriteCity(String city, String country) { this.city = city; this.country = country; }
    public Long getId() { return id; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
}
