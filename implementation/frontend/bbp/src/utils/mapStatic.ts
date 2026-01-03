/**
 * Utility for generating Mapbox static map images with route optimization.
 * Handles polyline encoding and Douglas-Peucker simplification to respect URL length limits.
 */

import polyline from '@mapbox/polyline'
import { ROUTE_LINE_COLOR, ROUTE_LINE_WIDTH, ORIGIN_MARKER_COLOR, DESTINATION_MARKER_COLOR } from '@/constants/map'
import type { BikePathPointResponse } from '@/types/bikePath'

// ============================================================================
// CONFIGURATION
// ============================================================================

const MAX_URL_LENGTH = 8192
const MIN_TOLERANCE = 0.00001 // ~1 meter
const MAX_TOLERANCE = 0.01 // ~1 km
const TOLERANCE_STEP = 0.00005

export interface StaticMapConfig {
    /** Mapbox access token */
    accessToken: string
    /** Image width in pixels */
    width?: number
    /** Image height in pixels */
    height?: number
    /** Mapbox style ID */
    styleId?: string
    /** Mapbox username */
    username?: string
    /** Path stroke color (hex without #) */
    strokeColor?: string
    /** Path stroke width */
    strokeWidth?: number
    /** Path stroke opacity (0-1) */
    strokeOpacity?: number
    /** Add origin/destination markers */
    addMarkers?: boolean
}

// ============================================================================
// DOUGLAS-PEUCKER SIMPLIFICATION
// ============================================================================

interface Point {
    x: number
    y: number
}

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

function simplifyPath(points: Point[], tolerance: number): Point[] {
    if (points.length <= 2) return points

    const sqTolerance = tolerance * tolerance
    return simplifyDouglasPeucker(points, sqTolerance)
}

// ============================================================================
// STATIC MAP URL GENERATION
// ============================================================================

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
        width = 600,
        height = 400,
        styleId = 'streets-v12',
        username = 'mapbox',
        strokeColor = ROUTE_LINE_COLOR.replace('#', ''), // Remove # for Mapbox API
        strokeWidth = ROUTE_LINE_WIDTH,
        strokeOpacity = 0.9,
        addMarkers = false
    } = config

    if (!points || points.length === 0) {
        throw new Error('No points provided for static map generation')
    }

    // Sort by sequential position
    const sortedPoints = [...points].sort((a, b) => a.sequentialPosition - b.sequentialPosition)

    if (sortedPoints.length === 0) {
        throw new Error('No valid points after sorting')
    }

    // Convert to Point format
    const pathPoints: Point[] = sortedPoints.map(p => ({
        x: p.longitude,
        y: p.latitude
    }))

    const originPoint = sortedPoints[0]
    const destinationPoint = sortedPoints[sortedPoints.length - 1]

    if (!originPoint || !destinationPoint) {
        throw new Error('Missing origin or destination point')
    }

    // Try with increasing tolerance until URL is valid
    let tolerance = MIN_TOLERANCE
    let lastError: Error | null = null

    while (tolerance <= MAX_TOLERANCE) {
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

            if (url.length <= MAX_URL_LENGTH) {
                const simplified = simplifyPath(pathPoints, tolerance)
                const reduction = ((1 - simplified.length / pathPoints.length) * 100).toFixed(1)
                console.log(
                    `✓ Static map URL generated: ${simplified.length}/${pathPoints.length} points (${reduction}% reduction), tolerance: ${tolerance.toFixed(5)}`
                )
                return url
            }

            lastError = new Error(`URL too long: ${url.length} characters`)
            tolerance += TOLERANCE_STEP
        } catch (error) {
            lastError = error as Error
            tolerance += TOLERANCE_STEP
        }
    }

    throw new Error(
        `Failed to generate valid static map URL. Last error: ${lastError?.message}`
    )
}

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
    // Simplify path
    const simplified = simplifyPath(points, tolerance)

    // Round to 6 decimals and convert to [lat, lng] for polyline encoding
    const coordinates: [number, number][] = simplified.map(p => [
        roundTo6Decimals(p.y), // latitude
        roundTo6Decimals(p.x)  // longitude
    ])

    // Encode as polyline
    const encoded = polyline.encode(coordinates)

    // Build overlays
    const overlays: string[] = []

    // Add markers if requested
    if (options.addMarkers) {
        const originColor = ORIGIN_MARKER_COLOR.replace('#', '')
        const destColor = DESTINATION_MARKER_COLOR.replace('#', '')

        overlays.push(
            `pin-s+${originColor}(${options.originPoint.longitude},${options.originPoint.latitude})`
        )
        overlays.push(
            `pin-s+${destColor}(${options.destinationPoint.longitude},${options.destinationPoint.latitude})`
        )
    }

    // Add path
    const pathOverlay = `path-${options.strokeWidth}+${options.strokeColor}-${options.strokeOpacity}(${encodeURIComponent(encoded)})`
    overlays.push(pathOverlay)

    // Build final URL
    const baseUrl = `https://api.mapbox.com/styles/v1/${options.username}/${options.styleId}/static`
    const overlay = overlays.join(',')
    const url = `${baseUrl}/${overlay}/auto/${options.width}x${options.height}@2x?access_token=${options.accessToken}`

    return url
}