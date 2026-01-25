/**
 * Composable to disable vertical scrolling on page mount.
 * Primarily used for full-screen map views (e.g., BikePathFinder).
 */
import { onMounted, onBeforeUnmount } from 'vue'

/**
 * Disables body scroll on mount, restores on unmount.
 * Uses force reflow to ensure style changes apply immediately.
 */
export function useNoScroll() {
    const disableScroll = () => {
        // Reset any existing scroll styles
        document.body.style.removeProperty('overflow')
        document.body.style.removeProperty('height')
        document.body.style.removeProperty('position')
        // Disable vertical scrolling
        document.body.style.overflowY = 'hidden'
        // Force reflow to apply changes immediately
        void document.body.offsetHeight
    }
    onMounted(() => {
        disableScroll()
        requestAnimationFrame(disableScroll) // Ensure applies after render
    })
    onBeforeUnmount(() => {
        document.body.style.overflowY = 'auto'
    })
}