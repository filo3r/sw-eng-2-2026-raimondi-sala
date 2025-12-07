package it.polimi.se.bbp.mapper.response;

import it.polimi.se.bbp.dto.response.BikePathResponse;
import it.polimi.se.bbp.dto.response.PagedBikePathResponse;
import it.polimi.se.bbp.entity.BikePath;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Mapper for converting paginated BikePath results to PagedBikePathResponse DTOs.
 * Extracts pagination metadata and maps content list.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {BikePathResponseMapper.class})
public interface PagedBikePathResponseMapper {

    /**
     * Converts Spring Data Page of bike paths to PagedBikePathResponse DTO.
     * Extracts pagination metadata and delegates content mapping to BikePathResponseMapper.
     * @param bikePathPage Spring Data Page containing bike path entities and pagination info
     * @return paged response DTO with mapped content and pagination metadata
     */
    default PagedBikePathResponse toPagedResponse(Page<BikePath> bikePathPage) {
        if (bikePathPage == null)
            return null;
        List<BikePathResponse> content = mapContent(bikePathPage.getContent());
        return new PagedBikePathResponse(
                content,
                bikePathPage.getNumber(),
                bikePathPage.getSize(),
                bikePathPage.getTotalElements(),
                bikePathPage.getTotalPages(),
                bikePathPage.hasNext(),
                bikePathPage.hasPrevious(),
                bikePathPage.isFirst(),
                bikePathPage.isLast()
        );
    }

    /**
     * Maps list of BikePath entities to BikePathResponse DTOs.
     * MapStruct generates implementation using BikePathResponseMapper.
     * @param bikePaths list of bike path entities
     * @return list of bike path response DTOs
     */
    List<BikePathResponse> mapContent(List<BikePath> bikePaths);

}