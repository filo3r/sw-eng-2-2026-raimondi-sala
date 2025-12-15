import { ref } from 'vue'

interface Toast {
    id: number
    message: string
    type: 'success' | 'error' | 'info'
}

const toasts = ref<Toast[]>([])

export function useToast() {
    function show(message: string, type: 'success' | 'error' | 'info' = 'error') {
        const id = Date.now()
        toasts.value.push({ id, message, type })

        setTimeout(() => {
            toasts.value = toasts.value.filter(t => t.id !== id)
        }, 5000)
    }

    return { toasts, show }
}