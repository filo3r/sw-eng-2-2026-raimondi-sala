package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.MeteorologicalDataResponse;
import it.polimi.se.bbp.entity.MeteorologicalData;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting MeteorologicalData entities to MeteorologicalDataResponse DTOs.
 */
@Component
public class MeteorologicalDataResponseMapper {

    /**
     * Converts a MeteorologicalData entity to a MeteorologicalDataResponse DTO.
     * @param meteorologicalData the meteorological data entity
     * @return the meteorological data response DTO, or null if input is null
     */
    public MeteorologicalDataResponse toResponse(MeteorologicalData meteorologicalData) {
        if (meteorologicalData == null)
            return null;
        return MeteorologicalDataResponse.builder()
                .weatherCondition(meteorologicalData.getWeatherCondition())
                .weatherDescription(meteorologicalData.getWeatherCondition().getWeatherDescription())
                .temperature(meteorologicalData.getTemperature())
                .humidity(meteorologicalData.getHumidity())
                .windSpeed(meteorologicalData.getWindSpeed())
                .build();
    }

}