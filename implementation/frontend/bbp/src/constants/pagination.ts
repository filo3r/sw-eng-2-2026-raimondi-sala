/**
 * Default page number for paginated requests.
 * All endpoints use 0-indexed pagination.
 */
export const DEFAULT_PAGE = 0

/** Sort direction: descending (newest/highest first) */
export const SORT_DESC = 'DESC'

/** Sort direction: ascending (oldest/lowest first) */
export const SORT_ASC = 'ASC'

/**
 * Default page size for bike path finder endpoint.
 * Endpoint: POST /api/finder/bike-paths
 */
export const BIKE_PATH_FINDER_PAGE_SIZE = 5

/**
 * Maximum page size for bike path finder endpoint.
 */
export const BIKE_PATH_FINDER_MAX_SIZE = 10

/**
 * Default page size for user bike paths endpoints.
 * Endpoints: GET /api/bike-paths, POST /api/bike-paths/search
 */
export const BIKE_PATH_PAGE_SIZE = 6

/**
 * Maximum page size for user bike paths endpoints.
 */
export const BIKE_PATH_MAX_SIZE = 12

/**
 * Default page size for trip endpoints.
 * Endpoints: GET /api/trips, POST /api/trips/search
 */
export const TRIP_PAGE_SIZE = 6

/**
 * Maximum page size for trip endpoints.
 */
export const TRIP_MAX_SIZE = 12