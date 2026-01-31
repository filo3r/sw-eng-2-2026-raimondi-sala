/**
 * Utility for generating Mapbox static map images with route optimization.
 * Handles polyline encoding and Douglas-Peucker simplification to respect URL length limits.
 */
import polyline from '@mapbox/polyline'
import {
    ROUTE_LINE_COLOR,
    ROUTE_LINE_WIDTH,
    ORIGIN_MARKER_COLOR,
    DESTINATION_MARKER_COLOR,
    STATIC_MAP_WIDTH,
    STATIC_MAP_HEIGHT,
    STATIC_MAP_STYLE_ID,
    STATIC_MAP_USERNAME,
    STATIC_MAP_STROKE_OPACITY,
    STATIC_MAP_MAX_URL_LENGTH,
    STATIC_MAP_MIN_TOLERANCE,
    STATIC_MAP_MAX_TOLERANCE,
    STATIC_MAP_TOLERANCE_STEP,
    STATIC_MAP_MIN_VERTICAL_PADDING,
    STATIC_MAP_MIN_HORIZONTAL_PADDING,
    STATIC_MAP_VERTICAL_PADDING_PERCENT,
    STATIC_MAP_HORIZONTAL_PADDING_PERCENT,
    STATIC_MAP_MAX_PADDING_PERCENT
} from '@/constants/map'
import type { BikePathPointResponse } from '@/types/bikePath'

/**
 * Configuration options for generating a Mapbox static map.
 * @property accessToken - Mapbox API access token (required)
 * @property width - Image width in pixels (default: STATIC_MAP_WIDTH)
 * @property height - Image height in pixels (default: STATIC_MAP_HEIGHT)
 * @property styleId - Mapbox style identifier (default: STATIC_MAP_STYLE_ID)
 * @property username - Mapbox username (default: STATIC_MAP_USERNAME)
 * @property strokeColor - Route line color without # prefix (default: ROUTE_LINE_COLOR)
 * @property strokeWidth - Route line width in pixels (default: ROUTE_LINE_WIDTH)
 * @property strokeOpacity - Route line opacity 0-1 (default: STATIC_MAP_STROKE_OPACITY)
 * @property addMarkers - Whether to add origin/destination markers (default: false)
 */
export interface StaticMapConfig {
    accessToken: string
    width?: number
    height?: number
    styleId?: string
    username?: string
    strokeColor?: string
    strokeWidth?: number
    strokeOpacity?: number
    addMarkers?: boolean
}

/**
 * Represents a 2D coordinate point.
 * @property x - Longitude coordinate
 * @property y - Latitude coordinate
 */
interface Point {
    x: number
    y: number
}

/**
 * Calculates the perpendicular distance from a point to a line segment.
 * Uses squared distance to avoid expensive square root operations.
 * @param p - The point to measure distance from
 * @param p1 - First endpoint of the line segment
 * @param p2 - Second endpoint of the line segment
 * @returns Squared perpendicular distance from point to line segment
 */
function getSquareSegmentDistance(p: Point, p1: Point, p2: Point): number {
    let x = p1.x
    let y = p1.y
    let dx = p2.x - x
    let dy = p2.y - y
    if (dx !== 0 || dy !== 0) {
        // Calculate projection of point onto line segment (normalized parameter t)
        const t = ((p.x - x) * dx + (p.y - y) * dy) / (dx * dx + dy * dy)
        if (t > 1) {
            // Point projects beyond p2
            x = p2.x
            y = p2.y
        } else if (t > 0) {
            // Point projects onto segment interior
            x += dx * t
            y += dy * t
        }
        // If t <= 0, point projects before p1, use p1 coordinates
    }
    // Calculate squared distance from point to closest point on segment
    dx = p.x - x
    dy = p.y - y
    return dx * dx + dy * dy
}

/**
 * Recursive step for Douglas-Peucker path simplification algorithm.
 * Finds the point with maximum distance and recursively simplifies both sides.
 * @param points - Array of all points in the path
 * @param first - Index of the first point in current segment
 * @param last - Index of the last point in current segment
 * @param sqTolerance - Squared tolerance threshold for simplification
 * @param simplified - Accumulator array for simplified points
 */
function simplifyDPStep(
    points: Point[],
    first: number,
    last: number,
    sqTolerance: number,
    simplified: Point[]
): void {
    let maxSqDist = sqTolerance
    let index = 0
    // Find point with maximum distance from line segment
    for (let i = first + 1; i < last; i++) {
        const currentPoint = points[i]
        const firstPoint = points[first]
        const lastPoint = points[last]
        if (!currentPoint || !firstPoint || !lastPoint) continue
        const sqDist = getSquareSegmentDistance(currentPoint, firstPoint, lastPoint)
        if (sqDist > maxSqDist) {
            index = i
            maxSqDist = sqDist
        }
    }
    // If max distance exceeds tolerance, split and recurse
    if (maxSqDist > sqTolerance) {
        if (index - first > 1) {
            simplifyDPStep(points, first, index, sqTolerance, simplified)
        }
        const indexPoint = points[index]
        if (indexPoint) {
            simplified.push(indexPoint)
        }
        if (last - index > 1) {
            simplifyDPStep(points, index, last, sqTolerance, simplified)
        }
    }
}

/**
 * Simplifies a path using the Douglas-Peucker algorithm.
 * Preserves first and last points while removing intermediate points below tolerance.
 * @param points - Array of points to simplify
 * @param sqTolerance - Squared tolerance threshold (in coordinate units squared)
 * @returns Simplified array of points
 */
function simplifyDouglasPeucker(points: Point[], sqTolerance: number): Point[] {
    if (points.length === 0) return []
    const last = points.length - 1
    const firstPoint = points[0]
    const lastPoint = points[last]
    if (!firstPoint || !lastPoint) return points
    // Initialize simplified path with first point
    const simplified: Point[] = [firstPoint]
    simplifyDPStep(points, 0, last, sqTolerance, simplified)
    simplified.push(lastPoint)
    return simplified
}

/**
 * Simplifies a path with the given tolerance value.
 * Wrapper function that squares tolerance for Douglas-Peucker algorithm.
 * @param points - Array of points to simplify
 * @param tolerance - Distance tolerance in coordinate units
 * @returns Simplified array of points, or original if 2 or fewer points
 */
function simplifyPath(points: Point[], tolerance: number): Point[] {
    if (points.length <= 2) return points
    const sqTolerance = tolerance * tolerance
    return simplifyDouglasPeucker(points, sqTolerance)
}

/**
 * Rounds a coordinate value to 6 decimal places.
 * Provides approximately 10cm precision for geographic coordinates.
 * @param value - Coordinate value to round
 * @returns Rounded coordinate value
 */
function roundTo6Decimals(value: number): number {
    return Math.round(value * 1000000) / 1000000
}

/**
 * Generates an optimized Mapbox static map URL for a bike path route.
 * Automatically simplifies the route using Douglas-Peucker algorithm if URL exceeds length limit.
 * Iteratively increases simplification tolerance until valid URL is generated.
 * @param points - Array of bike path points with coordinates and sequential positions
 * @param config - Configuration options for map generation
 * @returns Valid Mapbox static map URL string
 * @throws {Error} If no points provided, no valid points after sorting, or URL cannot be generated within tolerance limits
 */
export function generateStaticMapUrl(
    points: BikePathPointResponse[],
    config: StaticMapConfig
): string {
    const {
        accessToken,
        width = STATIC_MAP_WIDTH,
        height = STATIC_MAP_HEIGHT,
        styleId = STATIC_MAP_STYLE_ID,
        username = STATIC_MAP_USERNAME,
        strokeColor = ROUTE_LINE_COLOR.replace('#', ''),
        strokeWidth = ROUTE_LINE_WIDTH,
        strokeOpacity = STATIC_MAP_STROKE_OPACITY,
        addMarkers = false
    } = config
    if (!points || points.length === 0) {
        throw new Error('No points provided for static map generation')
    }
    // Sort points by sequential position to ensure correct route order
    const sortedPoints = [...points].sort((a, b) => a.sequentialPosition - b.sequentialPosition)
    if (sortedPoints.length === 0) {
        throw new Error('No valid points after sorting')
    }
    // Convert to Point format for simplification algorithm
    const pathPoints: Point[] = sortedPoints.map(p => ({
        x: p.longitude,
        y: p.latitude
    }))
    const originPoint = sortedPoints[0]
    const destinationPoint = sortedPoints[sortedPoints.length - 1]
    if (!originPoint || !destinationPoint) {
        throw new Error('Missing origin or destination point')
    }
    // Iteratively increase tolerance until URL fits within length limit
    let tolerance = STATIC_MAP_MIN_TOLERANCE
    let lastError: Error | null = null
    while (tolerance <= STATIC_MAP_MAX_TOLERANCE) {
        try {
            const url = buildStaticMapUrl(
                pathPoints,
                tolerance,
                {
                    width,
                    height,
                    styleId,
                    username,
                    strokeColor,
                    strokeWidth,
                    strokeOpacity,
                    accessToken,
                    addMarkers,
                    originPoint,
                    destinationPoint
                }
            )
            // Check if URL fits within Mapbox's length limit
            if (url.length <= STATIC_MAP_MAX_URL_LENGTH) {
                return url
            }
            lastError = new Error(`URL too long: ${url.length} characters`)
            tolerance += STATIC_MAP_TOLERANCE_STEP
        } catch (error) {
            lastError = error as Error
            tolerance += STATIC_MAP_TOLERANCE_STEP
        }
    }
    throw new Error(`Failed to generate valid static map URL. Last error: ${lastError?.message}`)
}

/**
 * Builds a Mapbox static map URL with automatic viewport calculation and smart padding.
 * Uses polyline encoding for efficient route representation and applies simplification.
 * @param points - Array of points representing the route path
 * @param tolerance - Simplification tolerance for Douglas-Peucker algorithm
 * @param options - Map styling and configuration options
 * @returns Complete Mapbox static API URL
 */
function buildStaticMapUrl(
    points: Point[],
    tolerance: number,
    options: {
        width: number
        height: number
        styleId: string
        username: string
        strokeColor: string
        strokeWidth: number
        strokeOpacity: number
        accessToken: string
        addMarkers: boolean
        originPoint: BikePathPointResponse
        destinationPoint: BikePathPointResponse
    }
): string {
    // Apply path simplification with current tolerance
    const simplified = simplifyPath(points, tolerance)
    // Convert to [lat, lng] format and round for consistency
    const coordinates: [number, number][] = simplified.map(p => [
        roundTo6Decimals(p.y),
        roundTo6Decimals(p.x)
    ])
    // Encode coordinates using polyline algorithm for URL efficiency
    const encoded = polyline.encode(coordinates)
    const overlays: string[] = []
    // Add optional origin and destination markers
    if (options.addMarkers) {
        const originColor = ORIGIN_MARKER_COLOR.replace('#', '')
        const destColor = DESTINATION_MARKER_COLOR.replace('#', '')
        // Add markers with labels 'O' (origin) and 'D' (destination)
        overlays.push(
            `pin-s-o+${originColor}(${options.originPoint.longitude},${options.originPoint.latitude})`
        )
        overlays.push(
            `pin-s-d+${destColor}(${options.destinationPoint.longitude},${options.destinationPoint.latitude})`
        )
    }
    // Add the route path overlay
    const pathOverlay = `path-${options.strokeWidth}+${options.strokeColor}-${options.strokeOpacity}(${encodeURIComponent(encoded)})`
    overlays.push(pathOverlay)
    const baseUrl = `https://api.mapbox.com/styles/v1/${options.username}/${options.styleId}/static`
    const overlay = overlays.join(',')
    // Calculate dynamic padding based on image dimensions
    const padY = Math.max(
        STATIC_MAP_MIN_VERTICAL_PADDING,
        Math.round(options.height * STATIC_MAP_VERTICAL_PADDING_PERCENT)
    )
    const padX = Math.max(
        STATIC_MAP_MIN_HORIZONTAL_PADDING,
        Math.round(options.width * STATIC_MAP_HORIZONTAL_PADDING_PERCENT)
    )
    // Apply maximum padding limits to prevent excessive whitespace
    const finalPadY = Math.min(padY, Math.round(options.height * STATIC_MAP_MAX_PADDING_PERCENT))
    const finalPadX = Math.min(padX, Math.round(options.width * STATIC_MAP_MAX_PADDING_PERCENT))
    // Padding format: top,right,bottom,left (CSS-like)
    const padding = `${finalPadY},${finalPadX},${finalPadY},${finalPadX}`
    // Use 'auto' for automatic viewport fitting and @2x for retina displays
    return `${baseUrl}/${overlay}/auto/${options.width}x${options.height}@2x?padding=${padding}&access_token=${options.accessToken}`
}