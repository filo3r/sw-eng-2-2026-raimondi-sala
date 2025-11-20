package it.polimi.se.bbp.repository;

import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.enums.BikePathStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BikePathRepositoryTest {

    @Autowired
    private BikePathRepository bikePathRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .name("Mario")
                .surname("Rossi")
                .username("mario.rossi")
                .email("mario@test.com")
                .password("password")
                .build();
        entityManager.persist(user);
    }

    @Test
    void shouldFindPublishedWithinBoundingBoxes() {
        BikePath inPath = BikePath.builder()
                .createdBy(user)
                .createdAt(OffsetDateTime.now())
                .origin("reachable starting point")
                .originLatitude(45.46)
                .originLongitude(9.19)
                .destination("final point")
                .destinationLatitude(45.48)
                .destinationLongitude(9.20)
                .description("path in bounding box")
                .score(BigDecimal.valueOf(4.5))
                .status(BikePathStatus.EXCELLENT)
                .totalDistance(BigDecimal.TEN)
                .published(true)
                .version(1L)
                .build();
        entityManager.persist(inPath);

        BikePath outPath = BikePath.builder()
                .createdBy(user)
                .createdAt(OffsetDateTime.now())
                .origin("non-reachable starting point")
                .originLatitude(41.90)
                .originLongitude(12.49)
                .destination("non-reachable destination")
                .destinationLatitude(41.92)
                .destinationLongitude(12.50)
                .description("path outside bounding box")
                .score(BigDecimal.valueOf(4.5))
                .status(BikePathStatus.EXCELLENT)
                .totalDistance(BigDecimal.TEN)
                .published(true)
                .version(1L)
                .build();
        entityManager.persist(outPath);
        
        entityManager.flush();

        // bounding box inPath
        double minLat = 45.0;
        double maxLat = 46.0;
        double minLon = 9.0;
        double maxLon = 10.0;

        List<BikePath> results = bikePathRepository.findPublishedWithinBoundingBoxes(
                minLat, maxLat, minLon, maxLon, // "from" box
                minLat, maxLat, minLon, maxLon  // "to" box
        );

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getOrigin()).isEqualTo("reachable starting point");
    }
}