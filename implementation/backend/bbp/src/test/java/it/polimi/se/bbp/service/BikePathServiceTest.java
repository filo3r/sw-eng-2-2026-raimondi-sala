package it.polimi.se.bbp.service;

import it.polimi.se.bbp.dto.request.BikePathManualCreateRequest;
import it.polimi.se.bbp.dto.request.BikePathSearchRequest;
import it.polimi.se.bbp.dto.request.BikePathUpdateRequest;
import it.polimi.se.bbp.dto.result.CyclingRouteResult;
import it.polimi.se.bbp.dto.result.GeocodeResult;
import it.polimi.se.bbp.entity.BikePath;
import it.polimi.se.bbp.entity.User;
import it.polimi.se.bbp.enums.BikePathStatus;
import it.polimi.se.bbp.geo.Coordinate;
import it.polimi.se.bbp.geo.SpatialService;
import it.polimi.se.bbp.mapper.entity.BikePathMapper;
import it.polimi.se.bbp.mapper.entity.BikePathPointMapper;
import it.polimi.se.bbp.repository.BikePathPointRepository;
import it.polimi.se.bbp.repository.BikePathRepository;
import it.polimi.se.bbp.repository.ObstacleRepository;
import it.polimi.se.bbp.service.mapbox.MapboxService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Bike Path Service Tests")
class BikePathServiceTest {

    @Mock
    private BikePathRepository bikePathRepository;
    @Mock
    private BikePathPointRepository bikePathPointRepository;
    @Mock
    private ObstacleRepository obstacleRepository;
    @Mock
    private UserAuthService userAuthService;
    @Mock
    private MapboxService mapboxService;
    @Mock
    private ObstacleService obstacleService;
    @Mock
    private SpatialService spatialService;
    @Mock
    private BikePathMapper bikePathMapper;
    @Mock
    private BikePathPointMapper bikePathPointMapper;

    @InjectMocks
    private BikePathService bikePathService;

    private User currentUser;
    private BikePath publicBikePath;
    private BikePath privateBikePath;

    @BeforeEach
    void setUp() {
        // Initialize the test environment
        currentUser = User.builder()
                .id(1L)
                .name("Mario")
                .surname("Rossi")
                .username("testuser")
                .build();

        publicBikePath = BikePath.builder()
                .id(10L)
                .createdBy(currentUser)
                .origin("Origin Address")
                .destination("Dest Address")
                .published(true)
                .status(BikePathStatus.GOOD)
                .totalDistance(BigDecimal.valueOf(10.5))
                .score(BigDecimal.valueOf(4.5))
                .version(1L)
                .bikePathPoints(new ArrayList<>())
                .obstacles(new ArrayList<>())
                .build();

        privateBikePath = BikePath.builder()
                .id(20L)
                .createdBy(currentUser)
                .origin("Secret Origin")
                .destination("Secret Dest")
                .published(false)
                .status(BikePathStatus.FAIR)
                .version(1L)
                .bikePathPoints(new ArrayList<>())
                .obstacles(new ArrayList<>())
                .build();
    }


    @Test
    @DisplayName("Should create bike path manually when valid data is provided")
    void createBikePathManually_Success() {
        // Simulates the end-to-end flow of manually creating a bike path: geocoding addresses, calculating the route geometry, determining the buffer zone, and saving the entity with calculated scores
        List<String> addresses = List.of("Origin", "Destination");
        BikePathManualCreateRequest request = new BikePathManualCreateRequest(
                addresses, "Description", BikePathStatus.EXCELLENT, true, new ArrayList<>()
        );

        GeocodeResult geoRes1 = new GeocodeResult("Origin", new Coordinate(45.0, 9.0));
        GeocodeResult geoRes2 = new GeocodeResult("Destination", new Coordinate(45.1, 9.1));
        List<GeocodeResult> geocodeResults = List.of(geoRes1, geoRes2);

        List<Coordinate> routeCoords = List.of(new Coordinate(45.0, 9.0), new Coordinate(45.1, 9.1));
        CyclingRouteResult routeResult = new CyclingRouteResult(routeCoords, 10000.0);

        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(mapboxService.geocodeAddresses(addresses)).thenReturn(geocodeResults);
        when(mapboxService.calculateCyclingRoute(any())).thenReturn(routeResult);
        when(bikePathMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(publicBikePath);
        when(bikePathRepository.save(any(BikePath.class))).thenReturn(publicBikePath);
        when(obstacleService.calculateObstacleScore(any(), any())).thenReturn(BigDecimal.valueOf(5.0));
        when(bikePathPointMapper.toEntities(anyList(), any(), any())).thenReturn(new ArrayList<>());

        BikePath createdPath = bikePathService.createBikePathManually(request);

        assertNotNull(createdPath);
        assertEquals(publicBikePath.getId(), createdPath.getId());

        verify(mapboxService).geocodeAddresses(addresses);
        verify(mapboxService).calculateCyclingRoute(anyList());
        verify(bikePathPointRepository).saveAll(anyList());
        verify(obstacleService).createAndSaveObstacles(any(), any(), any(), any(), any(), any());
        verify(bikePathRepository, times(2)).save(any(BikePath.class));
        verify(spatialService).createRouteBuffer(anyList(), anyDouble());
    }

    @Test
    @DisplayName("Should retrieve public bike path by ID successfully")
    void getBikePathById_Public_Success() {
        // Ensures that a published bike path is accessible to any user
        when(bikePathRepository.findById(10L)).thenReturn(Optional.of(publicBikePath));

        BikePath result = bikePathService.getBikePathById(10L);

        assertEquals(publicBikePath, result);
        verify(bikePathRepository).findById(10L);
    }

    @Test
    @DisplayName("Should retrieve private bike path if user is creator")
    void getBikePathById_Private_Creator_Success() {
        // Verifies that a private bike path is retrievable if the requesting user is the creator
        when(bikePathRepository.findById(20L)).thenReturn(Optional.of(privateBikePath));
        when(userAuthService.getAuthenticatedUserOrNull()).thenReturn(currentUser);

        BikePath result = bikePathService.getBikePathById(20L);

        assertEquals(privateBikePath, result);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when retrieving private bike path of another user")
    void getBikePathById_Private_NotCreator_ThrowsException() {
        // Ensures that a private bike path cannot be accessed by a user who is not the creator
        User otherUser = User.builder().id(999L).build();
        when(bikePathRepository.findById(20L)).thenReturn(Optional.of(privateBikePath));
        when(userAuthService.getAuthenticatedUserOrNull()).thenReturn(otherUser);

        assertThrows(AccessDeniedException.class, () -> bikePathService.getBikePathById(20L));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when bike path does not exist")
    void getBikePathById_NotFound_ThrowsException() {
        // If the bike path ID does not exist in the database, the service must throw an EntityNotFoundException
        when(bikePathRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bikePathService.getBikePathById(99L));
    }

    @Test
    @DisplayName("Should update bike path fields when user is owner and versions match")
    void updateBikePath_Success() {
        // Tests the successful update of bike path metadata, ensuring fields are modified correctly and the version check passes
        BikePathUpdateRequest request = new BikePathUpdateRequest(
                1L, BikePathStatus.POOR, "New Desc", true, null, null
        );

        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(bikePathRepository.findById(10L)).thenReturn(Optional.of(publicBikePath));
        when(obstacleService.calculateObstacleScore(any(), any())).thenReturn(BigDecimal.valueOf(2.5));
        when(bikePathRepository.save(any(BikePath.class))).thenAnswer(i -> i.getArguments()[0]);

        BikePath updatedPath = bikePathService.updateBikePath(10L, request);

        assertEquals("New Desc", updatedPath.getDescription());
        assertEquals(BikePathStatus.POOR, updatedPath.getStatus());
        assertEquals(currentUser, updatedPath.getUpdatedBy());

        verify(obstacleService, never()).updateAndSaveObstacles(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw OptimisticLockException when version mismatch")
    void updateBikePath_VersionMismatch_ThrowsException() {
        // Verifies that an OptimisticLockException is thrown when the request version does not match the current entity version
        BikePathUpdateRequest request = new BikePathUpdateRequest(
                2L, null, null, null, null, null
        );

        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(bikePathRepository.findById(10L)).thenReturn(Optional.of(publicBikePath));

        assertThrows(OptimisticLockException.class, () -> bikePathService.updateBikePath(10L, request));
        verify(bikePathRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when updating another user's bike path")
    void updateBikePath_NotOwner_ThrowsException() {
        // Ensures that a user cannot update a bike path they do not own, even if it exists
        User otherUser = User.builder().id(999L).build();
        BikePathUpdateRequest request = new BikePathUpdateRequest(
                1L, BikePathStatus.POOR, "Desc", true, null, null
        );

        when(userAuthService.getAuthenticatedUser()).thenReturn(otherUser);
        when(bikePathRepository.findById(20L)).thenReturn(Optional.of(privateBikePath));

        assertThrows(AccessDeniedException.class, () -> bikePathService.updateBikePath(20L, request));
        verify(bikePathRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent bike path")
    void updateBikePath_NotFound_ThrowsException() {
        // The update operation should fail fast with EntityNotFoundException if the target bike path is missing
        BikePathUpdateRequest request = new BikePathUpdateRequest(
                1L, null, null, null, null, null
        );
        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(bikePathRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bikePathService.updateBikePath(99L, request));
    }

    @Test
    @DisplayName("Should delete bike path when user is creator")
    void deleteBikePath_Success() {
        // Verifies that the creator can successfully delete their own bike path
        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(bikePathRepository.findById(10L)).thenReturn(Optional.of(publicBikePath));

        bikePathService.deleteBikePath(10L);

        verify(bikePathRepository).delete(publicBikePath);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when deleting another user's bike path")
    void deleteBikePath_NotCreator_ThrowsException() {
        // Attempts to delete another user's bike path must be blocked with an AccessDeniedException
        User otherUser = User.builder().id(999L).build();
        when(userAuthService.getAuthenticatedUser()).thenReturn(otherUser);
        when(bikePathRepository.findById(10L)).thenReturn(Optional.of(publicBikePath));

        assertThrows(AccessDeniedException.class, () -> bikePathService.deleteBikePath(10L));

        verify(bikePathRepository, never()).delete(any(BikePath.class));
    }

    @Test
    @DisplayName("Should retrieve paginated user bike paths with enrichment")
    void getUserBikePaths_Enrichment_Success() {
        // Tests retrieval of bike paths created by the user, ensuring the result page is correctly enriched with points and obstacles
        Page<BikePath> page = new PageImpl<>(List.of(publicBikePath));

        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);
        when(bikePathRepository.findPageByCreatedById(eq(currentUser.getId()), any(Pageable.class))).thenReturn(page);

        when(bikePathPointRepository.findAllByBikePathIdInOrderByBikePathIdAscSequentialPositionAsc(anyList()))
                .thenReturn(new ArrayList<>());
        when(obstacleRepository.findAllByBikePathIdInOrderByBikePathIdAscPositionOnPathAsc(anyList()))
                .thenReturn(new ArrayList<>());

        Page<BikePath> result = bikePathService.getUserBikePaths(0, 5, "createdAt", "DESC");

        assertEquals(1, result.getTotalElements());
        verify(bikePathPointRepository).findAllByBikePathIdInOrderByBikePathIdAscSequentialPositionAsc(anyList());
        verify(obstacleRepository).findAllByBikePathIdInOrderByBikePathIdAscPositionOnPathAsc(anyList());
    }

    @Test
    @DisplayName("Should search bike paths with filters and return enriched results")
    void searchBikePaths_Success() {
        // Tests the search functionality, ensuring that specifications are applied and the resulting page of bike paths is enriched with associated points and obstacles
        BikePathSearchRequest searchRequest = new BikePathSearchRequest(
                "Stazione Centrale ", "Piola", null, null
        );

        Page<BikePath> page = new PageImpl<>(List.of(publicBikePath));

        when(userAuthService.getAuthenticatedUser()).thenReturn(currentUser);

        when(bikePathRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        when(bikePathPointRepository.findAllByBikePathIdInOrderByBikePathIdAscSequentialPositionAsc(anyList()))
                .thenReturn(new ArrayList<>());
        when(obstacleRepository.findAllByBikePathIdInOrderByBikePathIdAscPositionOnPathAsc(anyList()))
                .thenReturn(new ArrayList<>());

        Page<BikePath> result = bikePathService.searchBikePaths(searchRequest, 0, 10, "score", "DESC");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(publicBikePath, result.getContent().get(0));

        verify(bikePathRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(bikePathPointRepository).findAllByBikePathIdInOrderByBikePathIdAscSequentialPositionAsc(anyList());
        verify(obstacleRepository).findAllByBikePathIdInOrderByBikePathIdAscPositionOnPathAsc(anyList());
    }
}