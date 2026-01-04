import { defineStore } from 'pinia'
import type { BikePathResponse } from '@/types/bikePath'

interface BikePathFinderState {
    // Search parameters
    originAddress: string
    destinationAddress: string
    originRadius: number
    destinationRadius: number

    // Results and pagination
    searchResults: BikePathResponse[]
    currentPage: number
    hasMore: boolean

    // UI state
    selectedBikePathId: number | null
    isSidebarOpen: boolean
}

export const useBikePathFinderStore = defineStore('bikePathFinder', {
    state: (): BikePathFinderState => ({
        // Search parameters
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
         * Check if there's saved search state to restore
         */
        hasSearchState(state): boolean {
            return state.searchResults.length > 0
        },

        /**
         * Get the currently selected bike path
         */
        selectedBikePath(state): BikePathResponse | undefined {
            if (!state.selectedBikePathId) return undefined
            return state.searchResults.find(bp => bp.id === state.selectedBikePathId)
        }
    },

    actions: {
        /**
         * Save complete search state before navigating away
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
            this.originAddress = data.originAddress
            this.destinationAddress = data.destinationAddress
            this.originRadius = data.originRadius
            this.destinationRadius = data.destinationRadius
            this.searchResults = data.searchResults
            this.currentPage = data.currentPage
            this.hasMore = data.hasMore
            this.selectedBikePathId = data.selectedBikePathId
            this.isSidebarOpen = data.isSidebarOpen

            console.log('✓ BikePathFinder state saved to store')
        },

        /**
         * Clear all search state (e.g., when user explicitly clears search)
         */
        clearSearchState() {
            this.originAddress = ''
            this.destinationAddress = ''
            this.originRadius = 0.1
            this.destinationRadius = 0.1
            this.searchResults = []
            this.currentPage = 0
            this.hasMore = false
            this.selectedBikePathId = null
            this.isSidebarOpen = false

            console.log('✓ BikePathFinder state cleared')
        },

        /**
         * Update selected bike path (e.g., when clicking on a result card)
         */
        setSelectedBikePath(id: number | null) {
            this.selectedBikePathId = id
        },

        /**
         * Toggle sidebar state
         */
        setSidebarOpen(isOpen: boolean) {
            this.isSidebarOpen = isOpen
        }
    }
})