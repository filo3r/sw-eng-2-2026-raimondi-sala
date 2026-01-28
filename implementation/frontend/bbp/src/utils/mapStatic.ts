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

interface Point {
    x: number
    y: number
}

/**
 * Calculates perpendicular distance from point to line segment.
 */
function getSquareSegmentDistance(p: Point, p1: Point, p2: Point): number {
    let x = p1.x
    let y = p1.y
    let dx = p2.x - x
    let dy = p2.y - y

    if (dx !== 0 || dy !== 0) {
        const t = ((p.x - x) * dx + (p.y - y) * dy) / (dx * dx + dy * dy)

        if (t > 1) {
            x = p2.x
            y = p2.y
        } else if (t > 0) {
            x += dx * t
            y += dy * t
        }
    }

    dx = p.x - x
    dy = p.y - y

    return dx * dx + dy * dy
}

/**
 * Recursive Douglas-Peucker simplification step.
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
 * Simplifies path using Douglas-Peucker algorithm.
 */
function simplifyDouglasPeucker(points: Point[], sqTolerance: number): Point[] {
    if (points.length === 0) return []

    const last = points.length - 1
    const firstPoint = points[0]
    const lastPoint = points[last]

    if (!firstPoint || !lastPoint) return points

    const simplified: Point[] = [firstPoint]

    simplifyDPStep(points, 0, last, sqTolerance, simplified)
    simplified.push(lastPoint)

    return simplified
}

/**
 * Simplifies path with given tolerance.
 */
function simplifyPath(points: Point[], tolerance: number): Point[] {
    if (points.length <= 2) return points

    const sqTolerance = tolerance * tolerance
    return simplifyDouglasPeucker(points, sqTolerance)
}

/**
 * Rounds coordinate to 6 decimal places (~10cm precision).
 */
function roundTo6Decimals(value: number): number {
    return Math.round(value * 1000000) / 1000000
}

/**
 * Generates optimized Mapbox static map URL for a bike path.
 * Automatically simplifies route if URL exceeds length limit.
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

    const sortedPoints = [...points].sort((a, b) => a.sequentialPosition - b.sequentialPosition)

    if (sortedPoints.length === 0) {
        throw new Error('No valid points after sorting')
    }

    const pathPoints: Point[] = sortedPoints.map(p => ({
        x: p.longitude,
        y: p.latitude
    }))

    const originPoint = sortedPoints[0]
    const destinationPoint = sortedPoints[sortedPoints.length - 1]

    if (!originPoint || !destinationPoint) {
        throw new Error('Missing origin or destination point')
    }

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

            if (url.length <= STATIC_MAP_MAX_URL_LENGTH) {
                const simplified = simplifyPath(pathPoints, tolerance)
                const reduction = ((1 - simplified.length / pathPoints.length) * 100).toFixed(1)
                console.log(
                    `âœ“ Static map URL generated: ${simplified.length}/${pathPoints.length} points (${reduction}% reduction), tolerance: ${tolerance.toFixed(5)}`
                )
                return url
            }

            lastError = new Error(`URL too long: ${url.length} characters`)
            tolerance += STATIC_MAP_TOLERANCE_STEP
        } catch (error) {
            lastError = error as Error
            tolerance += STATIC_MAP_TOLERANCE_STEP
        }
    }

    throw new Error(
        `Failed to generate valid static map URL. Last error: ${lastError?.message}`
    )
}

/**
 * Builds Mapbox static map URL using 'auto' with smart padding.
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
    const simplified = simplifyPath(points, tolerance)

    const coordinates: [number, number][] = simplified.map(p => [
        roundTo6Decimals(p.y),
        roundTo6Decimals(p.x)
    ])

    const encoded = polyline.encode(coordinates)

    const overlays: string[] = []

    if (options.addMarkers) {
        const originColor = ORIGIN_MARKER_COLOR.replace('#', '')
        const destColor = DESTINATION_MARKER_COLOR.replace('#', '')

        // Add markers with labels 'O' and 'D'
        overlays.push(
            `pin-s-o+${originColor}(${options.originPoint.longitude},${options.originPoint.latitude})`
        )
        overlays.push(
            `pin-s-d+${destColor}(${options.destinationPoint.longitude},${options.destinationPoint.latitude})`
        )
    }

    const pathOverlay = `path-${options.strokeWidth}+${options.strokeColor}-${options.strokeOpacity}(${encodeURIComponent(encoded)})`
    overlays.push(pathOverlay)

    const baseUrl = `https://api.mapbox.com/styles/v1/${options.username}/${options.styleId}/static`
    const overlay = overlays.join(',')

    // Calculate smart padding
    const padY = Math.max(
        STATIC_MAP_MIN_VERTICAL_PADDING,
        Math.round(options.height * STATIC_MAP_VERTICAL_PADDING_PERCENT)
    )

    const padX = Math.max(
        STATIC_MAP_MIN_HORIZONTAL_PADDING,
        Math.round(options.width * STATIC_MAP_HORIZONTAL_PADDING_PERCENT)
    )

    // Apply safety limits
    const finalPadY = Math.min(padY, Math.round(options.height * STATIC_MAP_MAX_PADDING_PERCENT))
    const finalPadX = Math.min(padX, Math.round(options.width * STATIC_MAP_MAX_PADDING_PERCENT))

    const padding = `${finalPadY},${finalPadX},${finalPadY},${finalPadX}`

    return `${baseUrl}/${overlay}/auto/${options.width}x${options.height}@2x?padding=${padding}&access_token=${options.accessToken}`
}