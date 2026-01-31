/**
 * Composable to disable vertical scrolling on page mount and restore on unmount.
 * Primarily used for full-screen map views where scrolling should be prevented (e.g., BikePathFinder).
 * Handles cleanup and style restoration to avoid affecting other pages.
 */
import { onMounted, onBeforeUnmount } from 'vue'

/**
 * Disables body vertical scrolling on component mount and restores it on unmount.
 * Uses force reflow technique to ensure style changes apply immediately.
 * Cleans up any existing scroll-related styles before applying new ones.
 */
export function useNoScroll() {
    /**
     * Disables vertical scrolling by setting overflow-y hidden on body.
     * Clears any conflicting styles and forces browser reflow for immediate effect.
     */
    const disableScroll = () => {
        // Reset any existing scroll-related styles to avoid conflicts
        document.body.style.removeProperty('overflow')
        document.body.style.removeProperty('height')
        document.body.style.removeProperty('position')
        // Disable vertical scrolling on body
        document.body.style.overflowY = 'hidden'
        // Force browser reflow to apply style changes immediately
        void document.body.offsetHeight
    }
    // Disable scroll on component mount
    onMounted(() => {
        disableScroll()
        requestAnimationFrame(disableScroll) // Ensure applies after DOM render completes
    })
    // Restore scroll on component unmount
    onBeforeUnmount(() => {
        document.body.style.overflowY = 'auto'
    })
}