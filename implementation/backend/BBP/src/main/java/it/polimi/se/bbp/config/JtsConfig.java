package it.polimi.se.bbp.config;

import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for JTS Topology Suite integration.
 * Provides beans for geometric operations used in bike path route validation.
 * JTS (Java Topology Suite) is used to validate that obstacles are within
 * a reasonable distance from bike path routes using geometric buffer operations.
 */
@Configuration
public class JtsConfig {

    /**
     * Creates a GeometryFactory bean for creating JTS geometric objects.
     * GeometryFactory is thread-safe and can be shared across the application.
     * It is used to create Points, LineStrings, and Polygons for spatial operations.
     * This factory is primarily used in ObstacleService to:
     * - Create Point geometries from obstacle coordinates
     * - Validate obstacle proximity to bike path routes using buffer operations
     * @return a configured GeometryFactory instance
     */
    @Bean
    public GeometryFactory geometryFactory() {
        return new GeometryFactory();
    }

}