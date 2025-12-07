package it.polimi.se.bbp.config;

import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for JTS Topology Suite integration.
 * Provides beans for geometric operations on bike paths and obstacles.
 */
@Configuration
public class JtsConfig {

    /**
     * Creates a GeometryFactory for building JTS geometric objects.
     * Used to create Points, LineStrings, and Polygons for spatial operations
     * such as validating obstacle proximity to routes using buffer operations.
     * @return configured GeometryFactory instance
     */
    @Bean
    public GeometryFactory geometryFactory() {
        return new GeometryFactory();
    }

}