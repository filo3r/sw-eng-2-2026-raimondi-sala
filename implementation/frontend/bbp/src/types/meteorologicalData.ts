/**
 * Weather conditions from Open-Meteo API.
 * Uses WMO Weather interpretation codes.
 */
export type WeatherCondition =
    | 'CLEAR_SKY'
    | 'MAINLY_CLEAR'
    | 'PARTLY_CLOUDY'
    | 'OVERCAST'
    | 'FOG'
    | 'DEPOSITING_RIME_FOG'
    | 'DRIZZLE_LIGHT'
    | 'DRIZZLE_MODERATE'
    | 'DRIZZLE_DENSE'
    | 'FREEZING_DRIZZLE_LIGHT'
    | 'FREEZING_DRIZZLE_DENSE'
    | 'RAIN_SLIGHT'
    | 'RAIN_MODERATE'
    | 'RAIN_HEAVY'
    | 'FREEZING_RAIN_LIGHT'
    | 'FREEZING_RAIN_HEAVY'
    | 'SNOW_FALL_SLIGHT'
    | 'SNOW_FALL_MODERATE'
    | 'SNOW_FALL_HEAVY'
    | 'SNOW_GRAINS'
    | 'RAIN_SHOWERS_SLIGHT'
    | 'RAIN_SHOWERS_MODERATE'
    | 'RAIN_SHOWERS_VIOLENT'
    | 'SNOW_SHOWERS_SLIGHT'
    | 'SNOW_SHOWERS_HEAVY'
    | 'THUNDERSTORM'
    | 'THUNDERSTORM_SLIGHT_HAIL'
    | 'THUNDERSTORM_HEAVY_HAIL'
    | 'UNKNOWN'

/**
 * Weather information for a trip from Open-Meteo API.
 */
export interface MeteorologicalDataResponse {
    /** Weather condition enum */
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