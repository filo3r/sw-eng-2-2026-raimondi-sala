import { defineStore } from 'pinia'
import type { BikePathResponse } from '@/types/bikePath'

/**
 * State interface for BikePathFinder store.
 * Manages search parameters, results, pagination, and UI state.
 */
interface BikePathFinderState {
    /** Origin address for search */
    originAddress: string
    /** Destination address for search */
    destinationAddress: string
    /** Search radius around origin in kilometers */
    originRadius: number
    /** Search radius around destination in kilometers */
    destinationRadius: number
    /** List of bike paths matching search criteria */
    searchResults: BikePathResponse[]
    /** Current page number for pagination (0-indexed) */
    currentPage: number
    /** Flag indicating if more results are available */
    hasMore: boolean
    /** ID of currently selected bike path (null if none selected) */
    selectedBikePathId: number | null
    /** Flag indicating if sidebar is open */
    isSidebarOpen: boolean
}

/**
 * Pinia store for BikePathFinder feature.
 * Manages search state, results, and UI state for bike path discovery.
 * Persists state across navigation for seamless user experience.
 */
export const useBikePathFinderStore = defineStore('bikePathFinder', {
    state: (): BikePathFinderState => ({
        // Search parameters with default values
        originAddress: '',
        destinationAddress: '',
        originRadius: 0.1,
        destinationRadius: 0.1,
        // Results and pagination
        searchResults: [],
        currentPage: 0,
        hasMore: false,
        // UI state
        selectedBikePathId: null,
        isSidebarOpen: false
    }),
    getters: {
        /**
         * Checks if there's saved search state to restore.
         * Used to determine if previous search should be displayed on page load.
         * @returns True if search results exist in store
         */
        hasSearchState(state): boolean {
            return state.searchResults.length > 0
        },
        /**
         * Gets the currently selected bike path from search results.
         * @returns Selected bike path or undefined if none selected or not found
         */
        selectedBikePath(state): BikePathResponse | undefined {
            if (!state.selectedBikePathId) return undefined
            return state.searchResults.find(bp => bp.id === state.selectedBikePathId)
        }
    },
    actions: {
        /**
         * Saves complete search state before navigating away.
         * Preserves all search parameters, results, and UI state for restoration.
         * @param data - Complete search state to save
         */
        saveSearchState(data: {
            originAddress: string
            destinationAddress: string
            originRadius: number
            destinationRadius: number
            searchResults: BikePathResponse[]
            currentPage: number
            hasMore: boolean
            selectedBikePathId: number | null
            isSidebarOpen: boolean
        }) {
            // Update all state properties from provided data
            this.originAddress = data.originAddress
            this.destinationAddress = data.destinationAddress
            this.originRadius = data.originRadius
            this.destinationRadius = data.destinationRadius
            this.searchResults = data.searchResults
            this.currentPage = data.currentPage
            this.hasMore = data.hasMore
            this.selectedBikePathId = data.selectedBikePathId
            this.isSidebarOpen = data.isSidebarOpen
        },
        /**
         * Clears all search state and resets to default values.
         * Called when user explicitly clears search or navigates to new search.
         */
        clearSearchState() {
            // Reset search parameters to defaults
            this.originAddress = ''
            this.destinationAddress = ''
            this.originRadius = 0.1
            this.destinationRadius = 0.1
            // Clear results and pagination
            this.searchResults = []
            this.currentPage = 0
            this.hasMore = false
            // Reset UI state
            this.selectedBikePathId = null
            this.isSidebarOpen = false
        },
    }
})