import { onMounted, onBeforeUnmount } from 'vue'

/**
 * Disable vertical scrolling (only for BikePathFinder)
 */
export function useNoScroll() {
    const disableScroll = () => {
        document.body.style.removeProperty('overflow')
        document.body.style.removeProperty('height')
        document.body.style.removeProperty('position')
        document.body.style.overflowY = 'hidden'
        void document.body.offsetHeight
    }

    onMounted(() => {
        disableScroll()
        requestAnimationFrame(disableScroll)
    })

    onBeforeUnmount(() => {
        document.body.style.overflowY = 'auto'
    })
}