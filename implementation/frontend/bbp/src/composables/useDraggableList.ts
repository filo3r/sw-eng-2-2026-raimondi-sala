/**
 * Composable for HTML5 native drag and drop list reordering.
 * Provides drag event handlers and state management without external dependencies.
 * Supports visual feedback during drag operations with hover states.
 */
import { ref } from 'vue'

/**
 * Composable that creates drag and drop utilities for reorderable lists.
 * Manages drag state, visual feedback, and item reordering through native HTML5 drag events.
 * @returns Object containing drag state refs and event handler methods
 */
export function useDraggableList() {
    /** Index of the item currently being dragged (null when not dragging) */
    const draggedIndex = ref<number | null>(null)
    /** Index of the item currently being hovered over during drag (null when not hovering) */
    const dragOverIndex = ref<number | null>(null)
    /**
     * Handles drag start event when user begins dragging an item.
     * Stores the index of the item being dragged for later use in drop handler.
     * @param index - Index of the list item being dragged (0-indexed)
     */
    function onDragStart(index: number) {
        draggedIndex.value = index
    }
    /**
     * Handles drag over event when dragged item hovers over drop target.
     * Prevents default to allow drop and tracks hovered item for visual feedback.
     * @param event - Native drag event (preventDefault required for drop to work)
     * @param index - Index of the list item being hovered over (0-indexed)
     */
    function onDragOver(event: DragEvent, index: number) {
        event.preventDefault() // Required to allow drop event to fire
        dragOverIndex.value = index
    }
    /**
     * Handles drag leave event when dragged item exits a drop target.
     * Clears hover state to remove visual feedback.
     */
    function onDragLeave() {
        dragOverIndex.value = null
    }
    /**
     * Handles drop event when user releases dragged item on a target.
     * Executes reorder callback if valid drop (different index) and resets drag state.
     * @param targetIndex - Index where the item is being dropped (0-indexed)
     * @param onReorder - Callback function to execute item reordering logic
     */
    function onDrop(targetIndex: number, onReorder: (fromIndex: number, toIndex: number) => void) {
        // Ignore drop if no item being dragged or dropped on itself
        if (draggedIndex.value === null || draggedIndex.value === targetIndex) {
            draggedIndex.value = null
            dragOverIndex.value = null
            return
        }
        // Execute reorder callback with source and target indices
        onReorder(draggedIndex.value, targetIndex)
        // Reset drag state after successful drop
        draggedIndex.value = null
        dragOverIndex.value = null
    }
    /**
     * Handles drag end event when drag operation completes (drop or cancel).
     * Resets all drag state to clean up after drag operation.
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