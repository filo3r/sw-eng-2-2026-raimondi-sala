/**
 * Weather conditions from Open-Meteo API for trip meteorological data.
 * Uses WMO Weather interpretation codes standard.
 * https://open-meteo.com/
 */
export type WeatherCondition =
/** Clear sky with no clouds (code: 0) */
    | 'CLEAR_SKY'
    /** Sky is mainly clear with minimal clouds (code: 1) */
    | 'MAINLY_CLEAR'
    /** Partly cloudy sky (code: 2) */
    | 'PARTLY_CLOUDY'
    /** Completely overcast sky (code: 3) */
    | 'OVERCAST'
    /** Foggy conditions with reduced visibility (code: 45) */
    | 'FOG'
    /** Fog with rime ice formation (code: 48) */
    | 'DEPOSITING_RIME_FOG'
    /** Light intensity drizzle (code: 51) */
    | 'DRIZZLE_LIGHT'
    /** Moderate intensity drizzle (code: 53) */
    | 'DRIZZLE_MODERATE'
    /** Dense intensity drizzle (code: 55) */
    | 'DRIZZLE_DENSE'
    /** Light intensity freezing drizzle (code: 56) */
    | 'FREEZING_DRIZZLE_LIGHT'
    /** Dense intensity freezing drizzle (code: 57) */
    | 'FREEZING_DRIZZLE_DENSE'
    /** Slight intensity rain (code: 61) */
    | 'RAIN_SLIGHT'
    /** Moderate intensity rain (code: 63) */
    | 'RAIN_MODERATE'
    /** Heavy intensity rain (code: 65) */
    | 'RAIN_HEAVY'
    /** Light intensity freezing rain (code: 66) */
    | 'FREEZING_RAIN_LIGHT'
    /** Heavy intensity freezing rain (code: 67) */
    | 'FREEZING_RAIN_HEAVY'
    /** Slight intensity snowfall (code: 71) */
    | 'SNOW_FALL_SLIGHT'
    /** Moderate intensity snowfall (code: 73) */
    | 'SNOW_FALL_MODERATE'
    /** Heavy intensity snowfall (code: 75) */
    | 'SNOW_FALL_HEAVY'
    /** Snow grains precipitation (code: 77) */
    | 'SNOW_GRAINS'
    /** Slight intensity rain showers (code: 80) */
    | 'RAIN_SHOWERS_SLIGHT'
    /** Moderate intensity rain showers (code: 81) */
    | 'RAIN_SHOWERS_MODERATE'
    /** Violent intensity rain showers (code: 82) */
    | 'RAIN_SHOWERS_VIOLENT'
    /** Slight intensity snow showers (code: 85) */
    | 'SNOW_SHOWERS_SLIGHT'
    /** Heavy intensity snow showers (code: 86) */
    | 'SNOW_SHOWERS_HEAVY'
    /** Slight or moderate thunderstorm (code: 95) */
    | 'THUNDERSTORM'
    /** Thunderstorm with slight hail (code: 96) */
    | 'THUNDERSTORM_SLIGHT_HAIL'
    /** Thunderstorm with heavy hail (code: 99) */
    | 'THUNDERSTORM_HEAVY_HAIL'
    /** Fallback for unrecognized weather codes (code: -1) */
    | 'UNKNOWN'

/**
 * Weather information for a trip from Open-Meteo API.
 * Contains meteorological conditions recorded during the trip.
 */
export interface MeteorologicalDataResponse {
    /**
     * Weather condition enum.
     * Examples: CLEAR_SKY, RAIN, SNOW.
     */
    weatherCondition: WeatherCondition

    /**
     * Human-readable weather description.
     * Example: "Clear sky", "Rain: Moderate intensity"
     */
    weatherDescription: string

    /**
     * Ambient temperature in degrees Celsius.
     * Range: -99.9 to +99.9
     */
    temperature: number

    /**
     * Relative humidity as percentage.
     * Range: 0 to 100
     */
    humidity: number

    /**
     * Wind speed in km/h.
     * Range: 0.0 to 999.9
     */
    windSpeed: number
}