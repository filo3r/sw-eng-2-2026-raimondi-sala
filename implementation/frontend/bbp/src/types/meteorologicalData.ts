/**
 * Weather conditions from Open-Meteo API.
 * Uses WMO Weather interpretation codes.
 */
export type WeatherCondition =
    | 'CLEAR_SKY' // Clear sky, no clouds
    | 'MAINLY_CLEAR' // Mainly clear sky with few clouds
    | 'PARTLY_CLOUDY' // Partly cloudy sky
    | 'OVERCAST' // Overcast, completely cloudy sky
    | 'FOG' // Fog reducing visibility
    | 'DEPOSITING_RIME_FOG' // Fog with ice crystal deposits
    | 'DRIZZLE_LIGHT' // Light drizzle
    | 'DRIZZLE_MODERATE' // Moderate drizzle
    | 'DRIZZLE_DENSE' // Dense intensity drizzle
    | 'FREEZING_DRIZZLE_LIGHT' // Light freezing drizzle
    | 'FREEZING_DRIZZLE_DENSE' // Dense freezing drizzle
    | 'RAIN_SLIGHT' // Slight rain
    | 'RAIN_MODERATE' // Moderate rain
    | 'RAIN_HEAVY' // Heavy intensity rain
    | 'FREEZING_RAIN_LIGHT' // Light freezing rain
    | 'FREEZING_RAIN_HEAVY' // Heavy freezing rain
    | 'SNOW_FALL_SLIGHT' // Slight snow fall
    | 'SNOW_FALL_MODERATE' // Moderate snow fall
    | 'SNOW_FALL_HEAVY' // Heavy intensity snow fall
    | 'SNOW_GRAINS' // Snow grains
    | 'RAIN_SHOWERS_SLIGHT' // Slight rain showers
    | 'RAIN_SHOWERS_MODERATE' // Moderate rain showers
    | 'RAIN_SHOWERS_VIOLENT' // Violent rain showers
    | 'SNOW_SHOWERS_SLIGHT' // Slight snow showers
    | 'SNOW_SHOWERS_HEAVY' // Heavy snow showers
    | 'THUNDERSTORM' // Thunderstorm
    | 'THUNDERSTORM_SLIGHT_HAIL' // Thunderstorm with slight hail
    | 'THUNDERSTORM_HEAVY_HAIL' // Thunderstorm with heavy hail
    | 'UNKNOWN' // Unknown or unrecognized weather condition

/**
 * Weather information for a trip from Open-Meteo API.
 * Includes temperature, humidity, wind speed, and current conditions.
 */
export interface MeteorologicalDataResponse {
    /** Weather condition enum value based on WMO codes */
    weatherCondition: WeatherCondition
    /** Human-readable weather description */
    weatherDescription: string
    /** Temperature in degrees Celsius (-99.9 to +99.9) */
    temperature: number
    /** Relative humidity percentage (0-100) */
    humidity: number
    /** Wind speed in km/h (0.0-999.9) */
    windSpeed: number
}