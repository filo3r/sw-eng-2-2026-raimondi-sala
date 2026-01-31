/**
 * Composable for Mapbox address autocomplete functionality.
 * Provides debounced search suggestions with coordinate data from Mapbox Search API.
 * Implements smart blur handling to allow suggestion click before dropdown closes.
 */
import { ref } from 'vue'
import { getMapboxApiKey } from '@/config/mapbox'
import {
    AUTOCOMPLETE_DEBOUNCE_MS,
    AUTOCOMPLETE_MIN_CHARS,
    AUTOCOMPLETE_LIMIT,
    AUTOCOMPLETE_BLUR_DELAY_MS
} from '@/constants/map'

/**
 * Autocomplete suggestion structure from Mapbox Search API.
 */
export interface AutocompleteSuggestion {
    /** Short name of the location */
    name: string
    /** Full formatted address string */
    full_address: string
    /** Geographic coordinates of the location */
    coordinates: { longitude: number; latitude: number }
}

/**
 * Composable that creates autocomplete utilities for address search input.
 * Manages suggestions state, debounced API calls, and dropdown visibility.
 * @returns Object containing suggestions state and input event handlers
 */
export function useMapboxAutocomplete() {
    /** Array of current autocomplete suggestions from Mapbox API */
    const suggestions = ref<AutocompleteSuggestion[]>([])
    /** Flag controlling suggestion dropdown visibility */
    const showSuggestions = ref(false)
    /** Timeout handle for debouncing input changes */
    const debounceTimeout = ref<number | null>(null)
    /**
     * Fetches autocomplete suggestions from Mapbox Search API.
     * Queries for addresses, streets, and points of interest matching the search term.
     * @param query - User's search query string
     */
    async function fetchSuggestions(query: string) {
        const token = getMapboxApiKey()
        // Build Mapbox Search API URL with query and filters
        const url = `https://api.mapbox.com/search/searchbox/v1/suggest?q=${encodeURIComponent(query)}&access_token=${token}&session_token=temp&limit=${AUTOCOMPLETE_LIMIT}&types=address,street,poi`
        try {
            const res = await fetch(url)
            const data = await res.json()
            // Map API response to simplified suggestion structure
            if (data.suggestions) {
                suggestions.value = data.suggestions.map((s: any) => ({
                    name: s.name,
                    full_address: s.full_address || s.place_formatted || s.name, // Fallback to name if address missing
                    coordinates: {
                        longitude: s.coordinates?.longitude,
                        latitude: s.coordinates?.latitude
                    }
                }))
                showSuggestions.value = true
            }
        } catch (e) {
            console.error('Error fetching suggestions:', e)
            suggestions.value = []
            showSuggestions.value = false
        }
    }
    /**
     * Handles input value changes with debouncing to prevent excessive API calls.
     * Clears suggestions if query is too short or empty.
     * @param value - Current input field value
     */
    function onInput(value: string) {
        // Clear existing debounce timer
        if (debounceTimeout.value) {
            clearTimeout(debounceTimeout.value)
        }
        // Clear suggestions if query too short
        if (!value || value.trim().length < AUTOCOMPLETE_MIN_CHARS) {
            suggestions.value = []
            showSuggestions.value = false
            return
        }
        // Debounce API call to avoid excessive requests during typing
        debounceTimeout.value = setTimeout(() => {
            fetchSuggestions(value)
        }, AUTOCOMPLETE_DEBOUNCE_MS) as unknown as number
    }
    /**
     * Clears all suggestions and hides the dropdown.
     * Used when clearing input or after selection.
     */
    function clearSuggestions() {
        suggestions.value = []
        showSuggestions.value = false
    }
    /**
     * Handles input blur event with delay to allow suggestion click.
     * Delay prevents dropdown from closing before click event fires.
     */
    function onBlur() {
        // Delay clearing to allow time for suggestion click to register
        setTimeout(() => {
            clearSuggestions()
        }, AUTOCOMPLETE_BLUR_DELAY_MS)
    }
    return {
        suggestions,
        showSuggestions,
        onInput,
        onBlur,
        clearSuggestions
    }
}