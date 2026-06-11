# Atmos Weather

Atmos is a premium full-stack weather dashboard built with React, Spring Boot, and PostgreSQL. It includes animated condition-aware themes, hourly and weekly forecasts, air quality, analytics, map layers, favorites, history, responsive layouts, offline caching, and a PWA manifest.

## Run locally

The API connects by default to the existing local PostgreSQL `atoms` database on port `5433`.

```powershell
cd backend
mvn spring-boot:run
```

In a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

## PostgreSQL

Your existing PostgreSQL connection is configured as:

```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5433/atoms"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="kiit"
```

These values are also the backend defaults. Do not run `docker compose up` while the existing PostgreSQL server is using port `5433`; the compose file is only an optional replacement database.

## Configuration

- `VITE_API_URL`: backend base URL, default `http://localhost:8087/api`
- `VITE_OPENWEATHER_API_KEY`: enables OpenWeather map overlays
- `OPENWEATHER_API_KEY`: enables live server-side current, hourly, weekly, and air-quality data

Without API keys, Atmos uses deterministic demo forecast data so every screen remains fully usable. It also falls back gracefully during provider outages. Favorites and search history are always persisted through JPA.

## Verification

```powershell
cd frontend
npm run lint
npm run build

cd ../backend
mvn test
```
