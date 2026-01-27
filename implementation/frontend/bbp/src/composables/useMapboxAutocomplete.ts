/**
 * Composable for Mapbox address autocomplete functionality.
 * Provides debounced search suggestions with coordinate data.
 */
import { ref } from 'vue'
import { getMapboxApiKey } from '@/config/mapbox'
import {
    AUTOCOMPLETE_DEBOUNCE_MS,
    AUTOCOMPLETE_MIN_CHARS,
    AUTOCOMPLETE_LIMIT,
    AUTOCOMPLETE_BLUR_DELAY_MS
} from '@/constants/map'

export interface AutocompleteSuggestion {
    name: string
    full_address: string
    coordinates: { longitude: number; latitude: number }
}

/**
 * Creates autocomplete utilities for address search.
 * @returns Suggestions state and input handling methods
 */
export function useMapboxAutocomplete() {
    const suggestions = ref<AutocompleteSuggestion[]>([])
    const showSuggestions = ref(false)
    const debounceTimeout = ref<number | null>(null)

    /**
     * Fetches autocomplete suggestions from Mapbox API.
     * @param query - Search query string
     */
    async function fetchSuggestions(query: string) {
        const token = getMapboxApiKey()
        const url = `https://api.mapbox.com/search/searchbox/v1/suggest?q=${encodeURIComponent(query)}&access_token=${token}&session_token=temp&limit=${AUTOCOMPLETE_LIMIT}&types=address,street,poi`

        try {
            const res = await fetch(url)
            const data = await res.json()

            if (data.suggestions) {
                suggestions.value = data.suggestions.map((s: any) => ({
                    name: s.name,
                    full_address: s.full_address || s.place_formatted || s.name,
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
     * Handles input with debouncing.
     * @param value - Current input value
     */
    function onInput(value: string) {
        if (debounceTimeout.value) {
            clearTimeout(debounceTimeout.value)
        }

        if (!value || value.trim().length < AUTOCOMPLETE_MIN_CHARS) {
            suggestions.value = []
            showSuggestions.value = false
            return
        }

        debounceTimeout.value = setTimeout(() => {
            fetchSuggestions(value)
        }, AUTOCOMPLETE_DEBOUNCE_MS) as unknown as number
    }

    /**
     * Clears suggestions and hides dropdown.
     */
    function clearSuggestions() {
        suggestions.value = []
        showSuggestions.value = false
    }

    /**
     * Handles blur with delay to allow suggestion click.
     */
    function onBlur() {
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