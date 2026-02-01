/**
 * Tailwind CSS configuration file.
 * Defines content paths for JIT compilation and integrates DaisyUI component library.
 * Tailwind scans specified files for class usage to generate optimized CSS bundle.
 */

/** @type {import('tailwindcss').Config} */
export default {
    /**
     * Content paths for Tailwind JIT compiler to scan for class usage.
     * Tailwind extracts classes from these files to generate the final CSS bundle.
     */
    content: [
        "./index.html", // Root HTML template
        "./src/**/*.{vue,js,ts,jsx,tsx}", // All Vue components and TypeScript/JavaScript files
    ],
    /**
     * Tailwind plugins array.
     * DaisyUI provides pre-styled components (buttons, alerts, toasts) using Tailwind utility classes.
     */
    plugins: [
        require("daisyui") // DaisyUI component library for semantic UI components
    ],
}