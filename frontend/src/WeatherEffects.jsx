import React, { useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Cloud } from 'lucide-react';

export default function WeatherEffects({ condition, isNight }) {
  // Use useMemo so we don't recalculate math random on every render
  const elements = useMemo(() => {
    const isRain = condition === 'rainy' || condition === 'rain';
    const isSnow = condition === 'snowy' || condition === 'snow';
    const isCloudy = condition === 'cloudy';
    const isSunny = condition === 'sunny' || condition === 'clear';

    // Generate random properties for immersive effects
    const generateParticles = (count, isFlake = false) => {
      return Array.from({ length: count }).map((_, i) => ({
        id: i,
        left: `${Math.random() * 100}%`,
        animationDuration: `${(Math.random() * 2 + (isFlake ? 4 : 0.8)).toFixed(2)}s`,
        animationDelay: `${-(Math.random() * 5).toFixed(2)}s`,
        opacity: Math.random() * 0.6 + 0.3,
        size: isFlake ? Math.random() * 4 + 2 : undefined,
        height: !isFlake ? Math.random() * 20 + 20 : undefined,
      }));
    };

    const generateStars = (count) => {
      return Array.from({ length: count }).map((_, i) => ({
        id: i,
        left: `${Math.random() * 100}%`,
        top: `${Math.random() * 100}%`,
        animationDuration: `${Math.random() * 3 + 2}s`,
        animationDelay: `${-(Math.random() * 5).toFixed(2)}s`,
        size: Math.random() * 2 + 1,
      }));
    };

    const clouds = Array.from({ length: 4 }).map((_, i) => ({
      id: i,
      top: `${Math.random() * 30 + (i * 15)}%`,
      animationDuration: `${Math.random() * 40 + 60}s`,
      animationDelay: `${-(Math.random() * 100)}s`,
      opacity: Math.random() * 0.15 + 0.05,
      scale: Math.random() * 2 + 1,
    }));

    return {
      rainDrops: isRain ? generateParticles(60, false) : [],
      snowFlakes: isSnow ? generateParticles(60, true) : [],
      stars: isNight ? generateStars(100) : [],
      clouds: isCloudy || isRain || isSnow ? clouds : [],
      isSunny,
      isNight
    };
  }, [condition, isNight]);

  return (
    <div className="weather-effects-container">
      <AnimatePresence mode="popLayout">
        <motion.div
          key={`${condition}-${isNight}`}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 1.5 }}
          className="absolute inset-0 pointer-events-none overflow-hidden z-[-1]"
        >
          {/* Ambient Glows */}
          <div className="ambient-sky">
            <i />
            <i />
            <i />
          </div>

          {/* Sun Rays */}
          {elements.isSunny && (
            <div className="sun-rays">
              <div className="ray" />
              <div className="ray" />
              <div className="ray" />
            </div>
          )}

          {/* Twinkling Stars */}
          {elements.isNight && elements.stars.map((star) => (
            <div
              key={`star-${star.id}`}
              className="star"
              style={{
                left: star.left,
                top: star.top,
                width: `${star.size}px`,
                height: `${star.size}px`,
                animationDuration: star.animationDuration,
                animationDelay: star.animationDelay,
              }}
            />
          ))}

          {/* Floating Clouds */}
          {elements.clouds.map((cloud) => (
            <div
              key={`cloud-${cloud.id}`}
              className="drifting-cloud"
              style={{
                top: cloud.top,
                opacity: cloud.opacity,
                transform: `scale(${cloud.scale})`,
                animationDuration: cloud.animationDuration,
                animationDelay: cloud.animationDelay,
              }}
            >
              <Cloud size={100} fill="currentColor" className="text-white" />
            </div>
          ))}

          {/* Falling Rain */}
          {elements.rainDrops.map((drop) => (
            <div
              key={`rain-${drop.id}`}
              className="raindrop"
              style={{
                left: drop.left,
                height: `${drop.height}px`,
                opacity: drop.opacity,
                animationDuration: drop.animationDuration,
                animationDelay: drop.animationDelay,
              }}
            />
          ))}

          {/* Falling Snow */}
          {elements.snowFlakes.map((flake) => (
            <div
              key={`snow-${flake.id}`}
              className="snowflake"
              style={{
                left: flake.left,
                width: `${flake.size}px`,
                height: `${flake.size}px`,
                opacity: flake.opacity,
                animationDuration: flake.animationDuration,
                animationDelay: flake.animationDelay,
              }}
            />
          ))}
        </motion.div>
      </AnimatePresence>
    </div>
  );
}
