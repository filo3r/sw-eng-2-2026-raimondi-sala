/**
 * Axios instance for backend API calls.
 * Base URL is automatically configured based on environment.
 */

import axios from 'axios';

/**
 * Gets backend API base URL (dev: from env, prod: from window).
 */
const getBackendUrl = (): string => {
    // Development: from VITE_BACKEND_PORT
    if (import.meta.env.DEV) {
        const port = import.meta.env.VITE_BACKEND_PORT || '8080';
        return `http://localhost:${port}/api`;
    }

    // Production: from window.BACKEND_URL
    if (window.BACKEND_URL) {
        return `${window.BACKEND_URL}/api`;
    }

    // Fallback
    return 'http://localhost:8080/api';
};

const api = axios.create({
    baseURL: getBackendUrl(),
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json'
    }
});

// TODO: Add interceptors when needed

export default api;