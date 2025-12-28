/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{vue,js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            // Font family customization
            fontFamily: {
                // Sans-serif fonts (default body text)
                sans: [
                    'ui-sans-serif',
                    'system-ui',
                    '-apple-system',
                    'BlinkMacSystemFont',
                    '"Segoe UI"',
                    'Roboto',
                    '"Helvetica Neue"',
                    'Arial',
                    '"Noto Sans"',
                    'sans-serif',
                    '"Apple Color Emoji"',
                    '"Segoe UI Emoji"',
                    '"Segoe UI Symbol"',
                    '"Noto Color Emoji"',
                ],
                // Serif fonts (for titles, special text)
                serif: [
                    'ui-serif',
                    'Georgia',
                    'Cambria',
                    '"Times New Roman"',
                    'Times',
                    'serif',
                ],
                // Monospace fonts (for code blocks)
                mono: [
                    'ui-monospace',
                    'SFMono-Regular',
                    'Menlo',
                    'Monaco',
                    'Consolas',
                    '"Liberation Mono"',
                    '"Courier New"',
                    'monospace',
                ],
            },
        },
    },
    plugins: [require("daisyui")],
    daisyui: {
        themes: [
            {
                light: {
                    // Extend default light theme
                    ...require("daisyui/src/theming/themes")["light"],
                    // Color tokens - Main colors
                    primary: "#570df8",              // Primary color (buttons, links)
                    secondary: "#f000b8",            // Secondary color
                    accent: "#37cdbe",               // Accent color
                    neutral: "#3d4451",              // Neutral color (text on neutral bg)
                    // Base colors - Backgrounds
                    "base-100": "#ffffff",           // Base background color
                    "base-200": "#f2f2f2",           // Secondary background color
                    "base-300": "#e5e6e6",           // Third background color
                    "base-content": "#1f2937",       // Text color on base backgrounds
                    // State colors
                    info: "#3abff8",                 // Info color
                    success: "#36d399",              // Success color
                    warning: "#fbbd23",              // Warning color
                    error: "#f87272",                // Error color
                    // Content colors (text colors on colored backgrounds)
                    "primary-content": "#ffffff",    // Text on primary color
                    "secondary-content": "#ffffff",  // Text on secondary color
                    "accent-content": "#ffffff",     // Text on accent color
                    "neutral-content": "#ffffff",    // Text on neutral color
                    "info-content": "#002b3d",       // Text on info color
                    "success-content": "#003320",    // Text on success color
                    "warning-content": "#382800",    // Text on warning color
                    "error-content": "#470000",      // Text on error color
                    // Border radius
                    "--rounded-box": "1rem",         // Border radius for cards, modals (16px)
                    "--rounded-btn": "0.5rem",       // Border radius for buttons (8px)
                    "--rounded-badge": "1.9rem",     // Border radius for badges (30px)
                    // Animations
                    "--animation-btn": "0.25s",      // Button animation duration
                    "--animation-input": "0.2s",     // Input animation duration
                    // Button styling
                    "--btn-text-case": "uppercase",  // Button text case (uppercase/lowercase/capitalize)
                    "--border-btn": "1px",           // Button border width
                    // Tab styling
                    "--tab-border": "1px",           // Tab border width
                    "--tab-radius": "0.5rem",        // Tab border radius (8px)
                },
            },
        ],
        // Global DaisyUI settings
        base: true,                      // Apply base styles
        styled: true,                    // Apply component styles
        utils: true,                     // Add utility classes
        prefix: "",                      // Prefix for DaisyUI classes (e.g., "daisy-")
        logs: true,                      // Show logs in console
        themeRoot: ":root",              // Element where theme CSS variables are applied
    },
}