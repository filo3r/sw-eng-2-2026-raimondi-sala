/**
 * Composable for HTML5 drag and drop list reordering.
 * Provides native drag & drop functionality without external libraries.
 */
import { ref } from 'vue'

/**
 * Creates drag and drop utilities for reorderable lists.
 * @returns Methods to handle drag events and reorder items
 */
export function useDraggableList() {
    const draggedIndex = ref<number | null>(null)
    const dragOverIndex = ref<number | null>(null)

    /**
     * Handles drag start event.
     * @param index - Index of item being dragged
     */
    function onDragStart(index: number) {
        draggedIndex.value = index
    }

    /**
     * Handles drag over event.
     * @param event - Drag event
     * @param index - Index of item being dragged over
     */
    function onDragOver(event: DragEvent, index: number) {
        event.preventDefault()
        dragOverIndex.value = index
    }

    /**
     * Handles drag leave event.
     */
    function onDragLeave() {
        dragOverIndex.value = null
    }

    /**
     * Handles drop event and reorders items.
     * @param targetIndex - Index where item is dropped
     * @param onReorder - Callback to execute reordering
     */
    function onDrop(targetIndex: number, onReorder: (fromIndex: number, toIndex: number) => void) {
        if (draggedIndex.value === null || draggedIndex.value === targetIndex) {
            draggedIndex.value = null
            dragOverIndex.value = null
            return
        }

        onReorder(draggedIndex.value, targetIndex)

        draggedIndex.value = null
        dragOverIndex.value = null
    }

    /**
     * Handles drag end event.
     */
    function onDragEnd() {
        draggedIndex.value = null
        dragOverIndex.value = null
    }

    return {
        draggedIndex,
        dragOverIndex,
        onDragStart,
        onDragOver,
        onDragLeave,
        onDrop,
        onDragEnd
    }
}