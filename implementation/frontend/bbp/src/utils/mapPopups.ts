/**
 * Utility functions for creating styled Mapbox popup HTML.
 * Provides consistent popup formatting across the application.
 */
import {
    POPUP_CONTENT_PADDING,
    POPUP_MIN_WIDTH
} from '@/constants/map'
import type { ObstacleResponse } from '@/types/obstacle'

interface PopupSection {
    label: string
    value: string
}

interface PopupContent {
    title: string
    sections: PopupSection[]
}

/**
 * Creates formatted HTML for popup content.
 * @param content - Popup title and sections
 * @returns HTML string
 */
function createPopupHTML(content: PopupContent): string {
    const sectionsHTML = content.sections.map(section => `
    <div style="margin-bottom: 8px;">
      <span style="font-size: 14px; font-weight: 500; color: #374151;">${section.label}:</span>
      <p style="font-size: 14px; color: #6b7280; margin: 4px 0 0 0;">${section.value}</p>
    </div>
  `).join('')

    return `
    <div style="padding: ${POPUP_CONTENT_PADDING}px; min-width: ${POPUP_MIN_WIDTH}px;">
      <h3 style="font-size: 16px; font-weight: 600; margin: 0 0 12px 0; color: #1f2937;">
        ${content.title}
      </h3>
      ${sectionsHTML}
    </div>
  `
}

/**
 * Creates popup HTML for route origin marker.
 * @param address - Origin address
 * @returns HTML string
 */
export function createOriginPopupHTML(address: string): string {
    return createPopupHTML({
        title: 'Origin',
        sections: [
            { label: 'Address', value: address }
        ]
    })
}

/**
 * Creates popup HTML for route destination marker.
 * @param address - Destination address
 * @returns HTML string
 */
export function createDestinationPopupHTML(address: string): string {
    return createPopupHTML({
        title: 'Destination',
        sections: [
            { label: 'Address', value: address }
        ]
    })
}

/**
 * Creates popup HTML for obstacle marker.
 * @param obstacle - Obstacle data
 * @returns HTML string
 */
export function createObstaclePopupHTML(obstacle: ObstacleResponse): string {
    return createPopupHTML({
        title: obstacle.typeDescription,
        sections: [
            { label: 'Location', value: obstacle.address },
            { label: 'Severity', value: obstacle.severityDescription }
        ]
    })
}