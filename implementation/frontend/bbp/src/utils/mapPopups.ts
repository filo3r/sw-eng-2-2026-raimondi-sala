/**
 * Utility functions for creating styled Mapbox popup HTML.
 * Provides consistent popup formatting across the application.
 */
import {
    POPUP_CONTENT_PADDING,
    POPUP_MIN_WIDTH
} from '@/constants/map'
import type { ObstacleResponse } from '@/types/obstacle'

/**
 * Represents a single section within a popup.
 * @property label - Section label text
 * @property value - Section content value
 */
interface PopupSection {
    label: string
    value: string
}

/**
 * Structure for popup content with title and multiple sections.
 * @property title - Main popup title
 * @property sections - Array of label-value pairs to display
 */
interface PopupContent {
    title: string
    sections: PopupSection[]
}

/**
 * Creates formatted HTML for popup content with consistent styling.
 * Generates a structured popup with title and labeled sections.
 * @param content - Popup title and sections to render
 * @returns HTML string with inline styles
 */
function createPopupHTML(content: PopupContent): string {
    // Build HTML for each section with label and value
    const sectionsHTML = content.sections.map(section => `
    <div style="margin-bottom: 8px;">
      <span style="font-size: 14px; font-weight: 500; color: #374151;">${section.label}:</span>
      <p style="font-size: 14px; color: #6b7280; margin: 4px 0 0 0;">${section.value}</p>
    </div>
  `).join('')
    // Combine title and sections into complete popup structure
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
 * Displays "Origin" title with address information.
 * @param address - Origin address to display
 * @returns Formatted HTML string for origin popup
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
 * Displays "Destination" title with address information.
 * @param address - Destination address to display
 * @returns Formatted HTML string for destination popup
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
 * Displays obstacle type, location, and severity level.
 * @param obstacle - Obstacle data containing type, address, and severity
 * @returns Formatted HTML string for obstacle popup
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