/*
 * Meteorological data related constants.
 */

import type { WeatherCondition } from '@/types/meteorologicalData'

// ============================================================================
// WEATHER CONDITION OPTIONS
// ============================================================================

/**
 * Weather condition option for UI selection.
 */
export interface WeatherConditionOption {
    /** Weather condition enum value */
    value: WeatherCondition
    /** Human-readable label */
    label: string
    /** WMO weather code from Open-Meteo API */
    code: number
}

/**
 * Available weather condition options.
 * Based on WMO Weather interpretation codes from Open-Meteo API.
 * Ordered by weather code.
 */
export const WEATHER_CONDITION_OPTIONS: readonly WeatherConditionOption[] = [
    { value: 'CLEAR_SKY', label: 'Clear sky', code: 0 },
    { value: 'MAINLY_CLEAR', label: 'Mainly clear', code: 1 },
    { value: 'PARTLY_CLOUDY', label: 'Partly cloudy', code: 2 },
    { value: 'OVERCAST', label: 'Overcast', code: 3 },
    { value: 'FOG', label: 'Fog', code: 45 },
    { value: 'DEPOSITING_RIME_FOG', label: 'Depositing rime fog', code: 48 },
    { value: 'DRIZZLE_LIGHT', label: 'Drizzle: Light intensity', code: 51 },
    { value: 'DRIZZLE_MODERATE', label: 'Drizzle: Moderate intensity', code: 53 },
    { value: 'DRIZZLE_DENSE', label: 'Drizzle: Dense intensity', code: 55 },
    { value: 'FREEZING_DRIZZLE_LIGHT', label: 'Freezing Drizzle: Light intensity', code: 56 },
    { value: 'FREEZING_DRIZZLE_DENSE', label: 'Freezing Drizzle: Dense intensity', code: 57 },
    { value: 'RAIN_SLIGHT', label: 'Rain: Slight intensity', code: 61 },
    { value: 'RAIN_MODERATE', label: 'Rain: Moderate intensity', code: 63 },
    { value: 'RAIN_HEAVY', label: 'Rain: Heavy intensity', code: 65 },
    { value: 'FREEZING_RAIN_LIGHT', label: 'Freezing Rain: Light intensity', code: 66 },
    { value: 'FREEZING_RAIN_HEAVY', label: 'Freezing Rain: Heavy intensity', code: 67 },
    { value: 'SNOW_FALL_SLIGHT', label: 'Snow fall: Slight intensity', code: 71 },
    { value: 'SNOW_FALL_MODERATE', label: 'Snow fall: Moderate intensity', code: 73 },
    { value: 'SNOW_FALL_HEAVY', label: 'Snow fall: Heavy intensity', code: 75 },
    { value: 'SNOW_GRAINS', label: 'Snow grains', code: 77 },
    { value: 'RAIN_SHOWERS_SLIGHT', label: 'Rain showers: Slight intensity', code: 80 },
    { value: 'RAIN_SHOWERS_MODERATE', label: 'Rain showers: Moderate intensity', code: 81 },
    { value: 'RAIN_SHOWERS_VIOLENT', label: 'Rain showers: Violent intensity', code: 82 },
    { value: 'SNOW_SHOWERS_SLIGHT', label: 'Snow showers: Slight intensity', code: 85 },
    { value: 'SNOW_SHOWERS_HEAVY', label: 'Snow showers: Heavy intensity', code: 86 },
    { value: 'THUNDERSTORM', label: 'Thunderstorm: Slight or moderate', code: 95 },
    { value: 'THUNDERSTORM_SLIGHT_HAIL', label: 'Thunderstorm with slight hail', code: 96 },
    { value: 'THUNDERSTORM_HEAVY_HAIL', label: 'Thunderstorm with heavy hail', code: 99 },
    { value: 'UNKNOWN', label: 'Unknown weather condition', code: -1 }
] as const

/**
 * Map weather condition to human-readable label.
 */
export const WEATHER_CONDITION_LABELS: Record<WeatherCondition, string> = {
    CLEAR_SKY: 'Clear sky',
    MAINLY_CLEAR: 'Mainly clear',
    PARTLY_CLOUDY: 'Partly cloudy',
    OVERCAST: 'Overcast',
    FOG: 'Fog',
    DEPOSITING_RIME_FOG: 'Depositing rime fog',
    DRIZZLE_LIGHT: 'Drizzle: Light intensity',
    DRIZZLE_MODERATE: 'Drizzle: Moderate intensity',
    DRIZZLE_DENSE: 'Drizzle: Dense intensity',
    FREEZING_DRIZZLE_LIGHT: 'Freezing Drizzle: Light intensity',
    FREEZING_DRIZZLE_DENSE: 'Freezing Drizzle: Dense intensity',
    RAIN_SLIGHT: 'Rain: Slight intensity',
    RAIN_MODERATE: 'Rain: Moderate intensity',
    RAIN_HEAVY: 'Rain: Heavy intensity',
    FREEZING_RAIN_LIGHT: 'Freezing Rain: Light intensity',
    FREEZING_RAIN_HEAVY: 'Freezing Rain: Heavy intensity',
    SNOW_FALL_SLIGHT: 'Snow fall: Slight intensity',
    SNOW_FALL_MODERATE: 'Snow fall: Moderate intensity',
    SNOW_FALL_HEAVY: 'Snow fall: Heavy intensity',
    SNOW_GRAINS: 'Snow grains',
    RAIN_SHOWERS_SLIGHT: 'Rain showers: Slight intensity',
    RAIN_SHOWERS_MODERATE: 'Rain showers: Moderate intensity',
    RAIN_SHOWERS_VIOLENT: 'Rain showers: Violent intensity',
    SNOW_SHOWERS_SLIGHT: 'Snow showers: Slight intensity',
    SNOW_SHOWERS_HEAVY: 'Snow showers: Heavy intensity',
    THUNDERSTORM: 'Thunderstorm: Slight or moderate',
    THUNDERSTORM_SLIGHT_HAIL: 'Thunderstorm with slight hail',
    THUNDERSTORM_HEAVY_HAIL: 'Thunderstorm with heavy hail',
    UNKNOWN: 'Unknown weather condition'
}

/**
 * Get weather condition label by WMO code.
 * Returns 'Unknown weather condition' if code is not found.
 * @param code - WMO weather code
 * @returns Human-readable weather condition label
 */
export function getWeatherConditionByCode(code: number): string {
    const condition = WEATHER_CONDITION_OPTIONS.find(option => option.code === code)
    return condition?.label ?? WEATHER_CONDITION_LABELS.UNKNOWN
}