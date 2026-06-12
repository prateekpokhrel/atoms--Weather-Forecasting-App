# Atmos Weather 🌤️

Atmos is a premium, state-of-the-art full-stack weather dashboard. It delivers hyper-local weather intelligence wrapped in an immersive, physics-driven, and highly aesthetic user interface. 

Built with **React (Vite)** on the frontend and **Spring Boot (Java)** with **PostgreSQL** on the backend, Atmos requires **no API keys** to run, relying on the powerful open-source **Open-Meteo** API for exact live weather data.

## 🚀 Key Features

### 🎨 Premium "Out of this League" UI & UX
* **Immersive Particle Engine**: The background comes alive based on the live weather. Watch clouds drift, rain fall, snow flurry, and stars twinkle in real-time.
* **Fluid Spring Physics**: Powered by `framer-motion`, every card, button, and metric interacts with tactile, realistic spring animations on hover.
* **Hyper-Realistic Glassmorphism**: Cards feature deep drop shadows, dynamic frosted-glass blurs, and crisp gradient borders to perfectly separate content from the animated backgrounds.
* **Automated Day/Night Cycle**: Dark mode is seamlessly integrated. The app automatically transitions to a dark starry aesthetic if the sun has set in the specific city you are viewing.
* **Bespoke Scrollbars & Components**: Custom floating map controls and sleek, minimalistic hidden scrollbars ensure a clean visual hierarchy.

### 🌩️ Live Weather Intelligence (Open-Meteo)
* **No API Keys Required**: The backend fetches precise data directly from the Open-Meteo API.
* **Current Conditions**: Live temperature, "feels like", humidity, wind speed, pressure, visibility, UV index, and cloud cover.
* **Hourly & 7-Day Forecasts**: Highly accurate temperature ranges and precipitation probabilities.
* **Air Quality**: Real-time pollutant breakdowns including US AQI, PM2.5, PM10, CO, NO₂, and O₃.
* **Robust Geocoding**: Automatically resolves city names to precise latitude/longitude coordinates.

### 💾 Backend & Data Persistence
* **Favorites & History**: Save your favorite cities or view your search history. All data is persisted to a local PostgreSQL database via Spring Data JPA.
* **Fallback Resilience**: In the event of an API outage, Atmos gracefully degrades to mathematically generated mock data, ensuring the app remains fully functional and testable at all times.
* **Geopolitical Accuracy**: Includes custom geographic mappings to ensure accurate country tagging for major cities (e.g., Bhubaneswar perfectly mapping to India).

---

## 🛠️ Getting Started

The project connects to a local PostgreSQL `atoms` database on port `5433`. 

### 1. Database Configuration
Your PostgreSQL connection is configured as:
```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5433/atoms"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="kiit"
```
*(These values are also the backend defaults.)*

### 2. Start the Backend (Spring Boot)
Open a terminal and run:
```powershell
cd backend
mvn spring-boot:run
```
*The API will start on `http://localhost:8087/api`.*

### 3. Start the Frontend (React / Vite)
Open a second terminal and run:
```powershell
cd frontend
npm install
npm run dev
```
*The application will be available at `http://localhost:5173`.*

---

## ⚙️ Configuration

- `VITE_API_URL`: Backend base URL (default `http://localhost:8087/api`).
- `VITE_OPENWEATHER_API_KEY`: Only required if you wish to enable the live radar map overlays in the "Weather Maps" tab. All other core weather features (Current, Hourly, Weekly, Air Quality) are fully powered by Open-Meteo and require no keys.

---

## ✅ Verification & Testing

To run the automated tests and linting:

**Frontend**:
```powershell
cd frontend
npm run lint
npm run build
```

**Backend**:
```powershell
cd backend
mvn test
```
