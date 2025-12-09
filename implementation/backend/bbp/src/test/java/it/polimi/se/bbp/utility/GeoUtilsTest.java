package it.polimi.se.bbp.utility;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeoUtilsTest {

    @Test
    void testHaversineDistance() {
        // milan duomo
        double lat1 = 45.4642;
        double lon1 = 9.1900;
        // milan central station
        double lat2 = 45.4859;
        double lon2 = 9.2041;

        double result = GeoUtils.haversineDistance(lat1, lon1, lat2, lon2);

        // distance should be approx 2.6 km
        assertTrue(result > 2.5 && result < 2.7);
    }

    @Test
    void testCalculateBoundingBox() {
        double lat = 45.0;
        double lon = 9.0;
        double radius = 10.0;

        GeoUtils.BoundingBox box = GeoUtils.calculateBoundingBox(lat, lon, radius);

        // check integrity
        assertTrue(box.getMinLat() < box.getMaxLat());
        assertTrue(box.getMinLon() < box.getMaxLon());
    }
}