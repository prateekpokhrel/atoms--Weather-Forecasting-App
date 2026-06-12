import { createContext, useContext, useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { BrowserRouter, NavLink, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import axios from 'axios'
import {
  AirVent, CalendarDays, Cloud, CloudRain, Droplets, Eye, Gauge,
  Heart, History, LocateFixed, Map, MapPin, Menu, Moon, Navigation, Search,
  Sparkles, Sun, Sunrise, Sunset, Trash2, Wind, X, Zap
} from 'lucide-react'
import { Area, AreaChart, Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { MapContainer, TileLayer } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import WeatherEffects from './WeatherEffects'

const API = import.meta.env.VITE_API_URL || 'http://localhost:8087/api'
const WeatherContext = createContext()
const popular = ['Biratnagar', 'Kathmandu', 'New York', 'London', 'Tokyo', 'Sydney', 'Paris']

const iconMap = {
  sun: Sun, moon: Moon, rain: CloudRain, cloud: Cloud,
}

function WeatherIcon({ type = 'sun', size = 34, className = '' }) {
  const Icon = iconMap[type] || Sun
  return <motion.div className={`weather-icon ${type} ${className}`} animate={type === 'sun' ? { rotate: 360 } : { y: [0, -4, 0] }} transition={{ duration: type === 'sun' ? 20 : 3, repeat: Infinity, ease: 'linear' }}><Icon size={size} strokeWidth={1.7} /></motion.div>
}

function Provider({ children }) {
  const [city, setCity] = useState(localStorage.getItem('atmos-city') || 'Kathmandu')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [favorites, setFavorites] = useState([])
  const [history, setHistory] = useState([])
  const [locating, setLocating] = useState(false)
  const [locationMessage, setLocationMessage] = useState('')

  const fetchPlaces = async () => {
    try {
      const [f, h] = await Promise.all([axios.get(`${API}/favorites`), axios.get(`${API}/history`)])
      setFavorites(f.data); setHistory(h.data)
    } catch { /* App remains useful while backend is starting. */ }
  }
  const load = async (nextCity = city, record = false) => {
    setLoading(true); setError('')
    try {
      const res = await axios.get(`${API}/weather/bundle/${encodeURIComponent(nextCity)}`)
      setData(res.data); setCity(nextCity); localStorage.setItem('atmos-city', nextCity)
      if (record) await axios.post(`${API}/history`, { city: res.data.current.city, country: res.data.current.country })
      fetchPlaces()
    } catch {
      setError('We could not reach the weather service. Check that the API is running and try again.')
    } finally { setLoading(false) }
  }
  const loadCoordinates = async (latitude, longitude, record = false) => {
    setLocating(true); setError(''); setLocationMessage('Pinpointing your location...')
    if (!data) setLoading(true)
    try {
      const res = await axios.get(`${API}/weather/coordinates`, { params: { lat: latitude, lon: longitude } })
      setData(res.data); setCity(res.data.current.city); localStorage.setItem('atmos-city', res.data.current.city)
      setLocationMessage(`Using your precise location near ${res.data.current.city}`)
      if (record) await axios.post(`${API}/history`, { city: res.data.current.city, country: res.data.current.country })
      fetchPlaces()
    } catch {
      setLocationMessage('Location weather is unavailable. Showing your last city.')
      if (!data) await load(city)
    } finally { setLoading(false); setLocating(false) }
  }
  const locateMe = (initial = false) => {
    if (!navigator.geolocation) {
      setLocationMessage('Location services are not supported by this browser.')
      if (initial) load(city)
      return
    }
    setLocating(true); setLocationMessage('Requesting precise location...')
    navigator.geolocation.getCurrentPosition(
      position => loadCoordinates(position.coords.latitude, position.coords.longitude, !initial),
      locationError => {
        setLocating(false)
        setLocationMessage(locationError.code === 1 ? 'Location permission was denied.' : 'Could not determine your precise location.')
        if (initial) load(city)
      },
      { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 },
    )
  }
  useEffect(() => {
    const initial = setTimeout(() => { locateMe(true); fetchPlaces() }, 0)
    const id = setInterval(() => load(localStorage.getItem('atmos-city') || city), 600000)
    return () => { clearTimeout(initial); clearInterval(id) }
    // The initial city is intentionally captured once; later refreshes use persisted state.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])
  const isNight = data ? !data.current.day : false
  const condition = data?.current.condition?.toLowerCase() || 'sunny'

  const addFavorite = async () => {
    if (!data || favorites.some(f => f.city === data.current.city)) return
    await axios.post(`${API}/favorites`, { city: data.current.city, country: data.current.country }); fetchPlaces()
  }
  return <WeatherContext.Provider value={{ city, data, loading, error, favorites, history, load, locateMe, locating, locationMessage, fetchPlaces, addFavorite }}>
    <div className={`app-shell weather-${condition} ${isNight ? 'night' : ''}`}><WeatherEffects condition={condition} isNight={isNight} />{children}</div>
  </WeatherContext.Provider>
}

const useWeather = () => useContext(WeatherContext)

function Sidebar({ open, close }) {
  const links = [
    ['/', Sparkles, 'Overview'], ['/forecast', CalendarDays, 'Forecast'], ['/air-quality', AirVent, 'Air quality'],
    ['/maps', Map, 'Weather maps'], ['/favorites', Heart, 'Favorites'], ['/history', History, 'Search history'],
  ]
  return <><AnimatePresence>{open && <motion.div className="mobile-scrim" onClick={close} initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} />}</AnimatePresence>
    <aside className={`sidebar ${open ? 'open' : ''}`}>
      <div className="brand"><span><CloudRain size={22} /></span> ATMOS</div>
      <button className="mobile-close" onClick={close}><X /></button>
      <nav>{links.map(([to, Icon, label]) => <NavLink key={to} to={to} onClick={close} end={to === '/'}><Icon size={19} /><span>{label}</span></NavLink>)}</nav>
      <div className="sidebar-foot"><div className="mini-orb"><Sun size={24} /></div><div><b>Atmos Pro</b><small>Hyperlocal insights</small></div><Zap size={15} /></div>
    </aside></>
}

function SearchBox() {
  const { load } = useWeather(); const [term, setTerm] = useState(''); const [open, setOpen] = useState(false)
  const matches = popular.filter(x => x.toLowerCase().includes(term.toLowerCase()))
  const submit = city => { if (city.trim()) load(city.trim(), true); setTerm(''); setOpen(false) }
  return <div className="search-wrap"><Search size={18} /><input value={term} onFocus={() => setOpen(true)} onChange={e => { setTerm(e.target.value); setOpen(true) }} onKeyDown={e => e.key === 'Enter' && submit(term)} placeholder="Search any city..." />
    <AnimatePresence>{open && <motion.div className="suggestions" initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
      <small>{term ? 'SUGGESTIONS' : 'POPULAR CITIES'}</small>
      {(matches.length ? matches : [term]).map(x => <button key={x} onClick={() => submit(x)}><MapPin size={16} />{x}<span>Weather</span></button>)}
    </motion.div>}</AnimatePresence>
  </div>
}

function Header({ toggleMenu }) {
  const { data, locateMe, locating } = useWeather()
  return <header><button className="menu-btn" onClick={toggleMenu}><Menu /></button><SearchBox />
    <div className="header-actions"><button className={`icon-btn locate ${locating ? 'is-locating' : ''}`} onClick={() => locateMe(false)} title="Use precise current location"><LocateFixed size={18} /></button>
      <div className="current-place"><span><MapPin size={15} /></span><div><b>{data?.current.city || 'Locating...'}</b><small>{data?.current.country || 'Your weather'}</small></div></div></div>
  </header>
}

function Layout() {
  const [menu, setMenu] = useState(false); const { data, loading, error, load } = useWeather(); const location = useLocation()
  return <><Sidebar open={menu} close={() => setMenu(false)} /><main><Header toggleMenu={() => setMenu(true)} />
    <AnimatePresence mode="wait">{loading ? <Loading key="loading" /> : error ? <ErrorState key="error" message={error} retry={load} /> :
      <motion.div key={location.pathname + data?.current.city} className="page" initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} transition={{ duration: .4 }}>
        <Routes><Route path="/" element={<Overview />} /><Route path="/forecast" element={<Forecast />} /><Route path="/air-quality" element={<AirQuality />} /><Route path="/maps" element={<Maps />} /><Route path="/favorites" element={<Favorites />} /><Route path="/history" element={<HistoryPage />} /><Route path="*" element={<Overview />} /></Routes>
      </motion.div>}</AnimatePresence></main></>
}

function Greeting() {
  const { data, locationMessage, locating } = useWeather(); const greeting = new Date().getHours() < 12 ? 'Good morning' : new Date().getHours() < 18 ? 'Good afternoon' : 'Good evening'
  return <div className="page-title"><div><span className="eyebrow"><Sparkles size={13} /> LIVE WEATHER INTELLIGENCE</span><h1>{greeting}, explorer.</h1><p>Here is what the sky has planned for {data.current.city}.</p><div className={`location-status ${locating ? 'working' : ''}`}><LocateFixed size={13} />{locationMessage || `Forecast centered on ${data.current.city}`}</div></div><div className="updated"><span></span> Updated just now</div></div>
}

function Hero() {
  const { data, addFavorite, favorites } = useWeather(); const c = data.current
  return <motion.section className="hero-card glass" whileHover={{ y: -3 }}>
    <div className="hero-top"><div><span className="pill"><Navigation size={13} /> {c.city}, {c.country}</span><span className="hero-kicker">LIVE CONDITIONS</span></div><button className="round-btn" onClick={addFavorite}><Heart size={18} fill={favorites.some(f => f.city === c.city) ? 'currentColor' : 'none'} /></button></div>
    <div className="hero-main"><div><div className="temperature">{c.temperature}<sup>°</sup></div><div className="condition">{c.condition}<span>Feels like {c.feelsLike}°</span></div></div><div className="hero-weather"><WeatherIcon type={c.icon} size={116} /><div className="float-cloud one"><Cloud size={30} /></div><div className="float-cloud two"><Cloud size={20} /></div></div></div>
    <div className="hero-bottom"><div><Sunrise /><span><small>Sunrise</small>{c.sunrise}</span></div><div><Sunset /><span><small>Sunset</small>{c.sunset}</span></div><div className="hero-note"><Sparkles size={16} /> {c.description}. Forecast centered at {c.latitude.toFixed(2)}, {c.longitude.toFixed(2)}.</div></div>
  </motion.section>
}

const metrics = [
  ['humidity', 'Humidity', Droplets, '%', 'Comfortable'],
  ['windSpeed', 'Wind speed', Wind, ' km/h', 'Light breeze'],
  ['pressure', 'Pressure', Gauge, ' hPa', 'Stable'],
  ['visibility', 'Visibility', Eye, ' km', 'Crystal clear'],
  ['uvIndex', 'UV index', Sun, '', 'Moderate'],
  ['clouds', 'Cloud cover', Cloud, '%', 'Mostly clear'],
]
function Highlights() {
  const { data } = useWeather()
  return <section><SectionTitle eyebrow="TODAY AT A GLANCE" title="Weather highlights" link="/forecast" />
    <div className="metric-grid">{metrics.map(([key, label, Icon, unit, note], i) => <motion.article className="metric-card glass" key={key} whileHover={{ scale: 1.03, y: -4 }} transition={{ type: 'spring', stiffness: 400, damping: 25 }}>
      <div className={`metric-icon tint-${i}`}><Icon size={20} /></div><span>{label}</span><strong>{data.current[key]}<small>{unit}</small></strong><div className="meter"><i style={{ width: `${Math.min(100, Number(data.current[key]) * (key === 'pressure' ? .075 : key === 'windSpeed' ? 8 : key === 'visibility' ? 8 : 1))}%` }} /></div><small>{note}</small>
    </motion.article>)}</div></section>
}

function SectionTitle({ eyebrow, title, link }) {
  const navigate = useNavigate()
  return <div className="section-title"><div><span>{eyebrow}</span><h2>{title}</h2></div>{link && <button onClick={() => navigate(link)}>View details <span>→</span></button>}</div>
}

function Hourly({ full = false }) {
  const { data } = useWeather()
  return <section><SectionTitle eyebrow="NEXT 24 HOURS" title="Hourly forecast" />
    <div className={`hourly glass ${full ? 'full' : ''}`}>{data.hourly.map((h, i) => <motion.div className={`hour ${i === 0 ? 'active' : ''}`} key={h.time} whileHover={{ scale: 1.05, y: -4 }} transition={{ type: 'spring', stiffness: 400, damping: 25 }}><span>{h.time}</span><WeatherIcon type={h.icon} size={28} /><strong>{h.temperature}°</strong><small><Droplets size={12} />{h.rainProbability}%</small></motion.div>)}</div></section>
}

function Weekly({ compact = false }) {
  const { data } = useWeather()
  return <section className={compact ? 'weekly-side' : ''}><SectionTitle eyebrow="WEEK AHEAD" title="7-day forecast" />
    <div className="weekly glass">{data.weekly.map((d, i) => <motion.div className="day-row" key={d.date} whileHover={{ scale: 1.02, x: 5 }} transition={{ type: 'spring', stiffness: 400, damping: 25 }}><b>{d.day}<small>{new Date(d.date + 'T00:00').toLocaleDateString('en', { month: 'short', day: 'numeric' })}</small></b><WeatherIcon type={d.icon} size={25} /><span>{d.condition}</span><div className="rain"><Droplets size={13} />{d.humidity}%</div><div className="temp-range"><b>{d.max}°</b><i><em style={{ left: `${i * 4}%`, right: `${20 - i * 2}%` }} /></i><small>{d.min}°</small></div></motion.div>)}</div></section>
}

function Overview() {
  return <><Greeting /><div className="overview-grid"><div><Hero /><Highlights /></div><div><Weekly compact /><AirCard /></div></div><Hourly /><Charts /></>
}

function Forecast() { return <><PageHeading title="Forecast" text="A closer look at the rhythm of the week." icon={CalendarDays} /><Hourly full /><div className="forecast-grid"><Weekly /><Charts /></div></> }

function AirCard() {
  const { data } = useWeather(); const a = data.airQuality
  return <section><SectionTitle eyebrow="BREATHE EASY" title="Air quality" link="/air-quality" /><div className="air-card glass"><div className="aq-ring"><span>{a.aqi}</span><small>US AQI</small></div><div><span className="good-tag">GOOD</span><h3>Air quality is excellent</h3><p>Ideal for outdoor activities. Enjoy the fresh air.</p></div></div></section>
}

function AirQuality() {
  const { data } = useWeather(); const a = data.airQuality; const pollutants = [['PM2.5', a.pm25, 'Fine particles'], ['PM10', a.pm10, 'Coarse particles'], ['CO', a.co, 'Carbon monoxide'], ['NO₂', a.no2, 'Nitrogen dioxide'], ['O₃', a.o3, 'Ground ozone']]
  return <><PageHeading title="Air quality" text={`The air in ${data.current.city} is looking beautifully clear.`} icon={AirVent} /><div className="aq-hero glass"><div className="big-aq"><div>{a.aqi}</div><span>US AQI</span></div><div><span className="good-tag">HEALTHY AIR</span><h2>Go outside and breathe deeply.</h2><p>Air pollution poses little or no risk today. Conditions are ideal for everyone.</p></div><AirVent size={100} /></div>
    <SectionTitle eyebrow="LIVE READINGS" title="Pollutant breakdown" /><div className="pollutant-grid">{pollutants.map((p, i) => <motion.div className="pollutant glass" key={p[0]} whileHover={{ scale: 1.04, y: -5 }} transition={{ type: 'spring', stiffness: 400, damping: 25 }}><span>{p[0]}</span><strong>{p[1]}<small>{i === 2 ? ' μg/m³' : ' μg/m³'}</small></strong><div className="meter"><i style={{ width: `${Math.min(95, p[1] / (i === 2 ? 4 : 1))}%` }} /></div><small>{p[2]}</small></motion.div>)}</div></>
}

function Charts() {
  const { data } = useWeather(); const chart = data.hourly.slice(0, 12)
  return <section><SectionTitle eyebrow="WEATHER ANALYTICS" title="Today's trends" /><div className="chart-grid"><Chart title="Temperature" value={`${data.current.temperature}°`} color="#ffb84d"><LineChart data={chart}><Line type="monotone" dataKey="temperature" stroke="#ffb84d" strokeWidth={3} dot={false} /><ChartBase /></LineChart></Chart><Chart title="Humidity" value={`${data.current.humidity}%`} color="#69c6ff"><BarChart data={chart}><Bar dataKey="humidity" fill="#69c6ff" radius={[8,8,0,0]} /><ChartBase /></BarChart></Chart><Chart title="Wind speed" value={`${data.current.windSpeed} km/h`} color="#a78bfa"><AreaChart data={chart}><defs><linearGradient id="wind" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#a78bfa" stopOpacity=".5"/><stop offset="1" stopColor="#a78bfa" stopOpacity="0"/></linearGradient></defs><Area type="monotone" dataKey="windSpeed" stroke="#a78bfa" fill="url(#wind)" strokeWidth={3}/><ChartBase /></AreaChart></Chart></div></section>
}
function Chart({ title, value, children }) { return <div className="chart-card glass"><span>{title}</span><strong>{value}</strong><ResponsiveContainer width="100%" height={145}>{children}</ResponsiveContainer></div> }
function ChartBase() { return <><CartesianGrid stroke="rgba(255,255,255,.08)" vertical={false}/><XAxis dataKey="time" tick={{ fill: 'currentColor', fontSize: 10 }} axisLine={false} tickLine={false}/><YAxis hide/><Tooltip contentStyle={{ background: '#171d2c', border: 0, borderRadius: 12 }}/></> }

function Maps() {
  const { data } = useWeather(); const [layer, setLayer] = useState('temp_new')
  return <><PageHeading title="Weather maps" text="Explore atmospheric patterns moving across the region." icon={Map} /><div className="map-shell glass"><div className="map-tabs">{[['temp_new','Temperature'],['precipitation_new','Rain'],['wind_new','Wind']].map(x => <button className={layer === x[0] ? 'active' : ''} onClick={() => setLayer(x[0])} key={x[0]}>{x[1]}</button>)}</div><MapContainer key={`${data.current.latitude}-${data.current.longitude}`} center={[data.current.latitude, data.current.longitude]} zoom={6} scrollWheelZoom className="map"><TileLayer attribution="© OpenStreetMap" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" /><TileLayer key={layer} opacity={.55} url={`https://tile.openweathermap.org/map/${layer}/{z}/{x}/{y}.png?appid=${import.meta.env.VITE_OPENWEATHER_API_KEY || ''}`} /></MapContainer><div className="map-legend"><span>Low</span><i></i><span>High</span></div></div></>
}

function Favorites() {
  const { favorites, load, fetchPlaces } = useWeather()
  const remove = async id => { await axios.delete(`${API}/favorites/${id}`); fetchPlaces() }
  return <><PageHeading title="Favorite cities" text="Your personal collection of skies around the world." icon={Heart} />{favorites.length ? <div className="place-grid">{favorites.map((f, i) => <motion.div className="place-card glass" key={f.id} whileHover={{ scale: 1.03, y: -5 }} transition={{ type: 'spring', stiffness: 400, damping: 25 }} onClick={() => load(f.city)}><WeatherIcon type={i % 3 === 1 ? 'rain' : 'sun'} size={44}/><div><h3>{f.city}</h3><p>{f.country}</p></div><strong>{22 + i * 2}°</strong><button onClick={e => { e.stopPropagation(); remove(f.id) }}><Trash2 size={16}/></button></motion.div>)}</div> : <Empty icon={Heart} title="No favorite cities yet" text="Tap the heart on the overview to keep a city close." />}</>
}

function HistoryPage() {
  const { history, load, fetchPlaces } = useWeather()
  const clear = async () => { await axios.delete(`${API}/history`); fetchPlaces() }
  return <><PageHeading title="Search history" text="Quickly return to the places you have explored." icon={History} action={history.length ? <button className="outline-btn" onClick={clear}><Trash2 size={15}/> Clear all</button> : null} />{history.length ? <div className="history-list glass">{history.map(h => <button key={h.id} onClick={() => load(h.city)}><span><History size={17}/></span><div><b>{h.city}, {h.country}</b><small>{new Date(h.searchedAt).toLocaleString()}</small></div><span>→</span></button>)}</div> : <Empty icon={Search} title="Your history is clear" text="Cities you search for will appear here." />}</>
}

function PageHeading({ title, text, icon: Icon, action }) { return <div className="page-heading"><div className="heading-icon"><Icon /></div><div><h1>{title}</h1><p>{text}</p></div>{action}</div> }
function Empty({ icon: Icon, title, text }) { return <div className="empty glass"><div><Icon /></div><h2>{title}</h2><p>{text}</p></div> }
function Loading() { return <div className="loading-page"><div className="loading-orb"><Sun /></div><h2>Reading the sky</h2><p>Gathering your latest forecast...</p><div className="skeleton-grid">{[1,2,3,4].map(x => <i key={x}/>)}</div></div> }
function ErrorState({ message, retry }) { return <div className="empty error glass"><div><CloudRain /></div><h2>Forecast unavailable</h2><p>{message}</p><button className="outline-btn" onClick={() => retry()}>Try again</button></div> }

export default function App() { return <BrowserRouter><Provider><Layout /></Provider></BrowserRouter> }
