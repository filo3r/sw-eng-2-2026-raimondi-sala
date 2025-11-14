package it.polimi.se.bbp.enums.openmeteo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum representing weather conditions retrieved from the Open-Meteo API for trip meteorological data.
 * Weather codes follow the WMO Weather interpretation codes standard used by Open-Meteo.
 * This data is used to enrich trip information when available from external weather services.
 * https://open-meteo.com/
 */
@Getter
@RequiredArgsConstructor
public enum WeatherCondition {
    /** Clear sky with no clouds. */
    CLEAR_SKY(0, "Clear sky"),
    /** Sky is mainly clear with minimal clouds. */
    MAINLY_CLEAR(1, "Mainly clear"),
    /** Partly cloudy sky. */
    PARTLY_CLOUDY(2, "Partly cloudy"),
    /** Completely overcast sky. */
    OVERCAST(3, "Overcast"),
    /** Foggy conditions with reduced visibility. */
    FOG(45, "Fog"),
    /** Fog with rime ice formation. */
    DEPOSITING_RIME_FOG(48, "Depositing rime fog"),
    /** Light intensity drizzle. */
    DRIZZLE_LIGHT(51, "Drizzle: Light intensity"),
    /** Moderate intensity drizzle. */
    DRIZZLE_MODERATE(53, "Drizzle: Moderate intensity"),
    /** Dense intensity drizzle. */
    DRIZZLE_DENSE(55, "Drizzle: Dense intensity"),
    /** Light intensity freezing drizzle. */
    FREEZING_DRIZZLE_LIGHT(56, "Freezing Drizzle: Light intensity"),
    /** Dense intensity freezing drizzle. */
    FREEZING_DRIZZLE_DENSE(57, "Freezing Drizzle: Dense intensity"),
    /** Slight intensity rain. */
    RAIN_SLIGHT(61, "Rain: Slight intensity"),
    /** Moderate intensity rain. */
    RAIN_MODERATE(63, "Rain: Moderate intensity"),
    /** Heavy intensity rain. */
    RAIN_HEAVY(65, "Rain: Heavy intensity"),
    /** Light intensity freezing rain. */
    FREEZING_RAIN_LIGHT(66, "Freezing Rain: Light intensity"),
    /** Heavy intensity freezing rain. */
    FREEZING_RAIN_HEAVY(67, "Freezing Rain: Heavy intensity"),
    /** Slight intensity snowfall. */
    SNOW_FALL_SLIGHT(71, "Snow fall: Slight intensity"),
    /** Moderate intensity snowfall. */
    SNOW_FALL_MODERATE(73, "Snow fall: Moderate intensity"),
    /** Heavy intensity snowfall. */
    SNOW_FALL_HEAVY(75, "Snow fall: Heavy intensity"),
    /** Snow grains precipitation. */
    SNOW_GRAINS(77, "Snow grains"),
    /** Slight intensity rain showers. */
    RAIN_SHOWERS_SLIGHT(80, "Rain showers: Slight intensity"),
    /** Moderate intensity rain showers. */
    RAIN_SHOWERS_MODERATE(81, "Rain showers: Moderate intensity"),
    /** Violent intensity rain showers. */
    RAIN_SHOWERS_VIOLENT(82, "Rain showers: Violent intensity"),
    /** Slight intensity snow showers. */
    SNOW_SHOWERS_SLIGHT(85, "Snow showers: Slight intensity"),
    /** Heavy intensity snow showers. */
    SNOW_SHOWERS_HEAVY(86, "Snow showers: Heavy intensity"),
    /** Slight or moderate thunderstorm. */
    THUNDERSTORM(95, "Thunderstorm: Slight or moderate"),
    /** Thunderstorm with slight hail. */
    THUNDERSTORM_SLIGHT_HAIL(96, "Thunderstorm with slight hail"),
    /** Thunderstorm with heavy hail. */
    THUNDERSTORM_HEAVY_HAIL(99, "Thunderstorm with heavy hail");

    /**
     * The WMO weather code from Open-Meteo API.
     * Used to map API responses to weather conditions.
     */
    private final int weatherCode;

    /**
     * The human-readable description of this weather condition.
     */
    private final String weatherDescription;

    /**
     * Immutable map for quick lookup of weather condition by code.
     */
    private static final Map<Integer, WeatherCondition> CODE_MAP =
            Stream.of(values()).collect(Collectors.toUnmodifiableMap(WeatherCondition::getWeatherCode, condition -> condition));

    /**
     * Retrieves the weather condition corresponding to the given WMO weather code.
     * @param code the WMO weather code from Open-Meteo API
     * @return the WeatherCondition enum constant matching the code
     * @throws IllegalArgumentException if no weather condition exists for the given code
     */
    public static WeatherCondition fromWeatherCode(int code) {
        WeatherCondition condition = CODE_MAP.get(code);
        if (condition == null)
            throw new IllegalArgumentException("Unknown weather code: " + code);
        return condition;
    }

}