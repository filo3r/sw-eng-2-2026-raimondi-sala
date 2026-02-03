/**
 * Production server for compiled Vue frontend.
 * Serves embedded static files and injects runtime configuration into HTML.
 */
import { serve } from "bun";
import { parseArgs } from "util";
import { embeddedFiles } from "./embedded-files";

/**
 * CLI arguments parsing.
 * Uses Bun.argv as input and Node's util.parseArgs for typed option parsing. [web:269]
 */
const { values } = parseArgs({
    args: Bun.argv,
    options: {
        "mapbox.api.key": { type: "string" },
        "frontend.port": { type: "string", default: "3000" },
        "backend.port": { type: "string", default: "8080" },
    },
    strict: true,
    allowPositionals: true,
});

/** Mapbox API key injected into the frontend HTML as a global variable. */
const mapboxApiKey = values["mapbox.api.key"];

/** Port where this embedded frontend server will listen. */
const frontendPort = parseInt(values["frontend.port"] || "3000");

/** Port where the backend is expected to be reachable locally. */
const backendPort = values["backend.port"] || "8080";

/** Backend base URL injected into the frontend HTML as a global variable. */
const backendUrl = `http://localhost:${backendPort}`;

/**
 * Validates required configuration.
 * Exits with a non-zero status code when missing mandatory flags.
 */
if (!mapboxApiKey) {
    console.error("Error: --mapbox.api.key is required");
    console.log("\nUsage:");
    console.log("  ./bbp-frontend --mapbox.api.key=YOUR_KEY [--frontend.port=3000] [--backend.port=8080]");
    process.exit(1);
}

/**
 * Injects runtime configuration into an HTML document by inserting a <script> before </head>.
 * @param html - Original HTML content.
 * @returns HTML content with window globals injected.
 */
function injectConfig(html: string): string {
    const configScript = `
  <script>
    window.MAPBOX_API_KEY = "${mapboxApiKey}";
    window.BACKEND_URL = "${backendUrl}";
  </script>`;
    return html.replace("</head>", `${configScript}</head>`);
}

/**
 * Static MIME type map for embedded assets.
 * Falls back to application/octet-stream for unknown extensions.
 */
const mimeTypes: Record<string, string> = {
    '.html': 'text/html',
    '.css': 'text/css',
    '.js': 'application/javascript',
    '.json': 'application/json',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon',
    '.woff': 'font/woff',
    '.woff2': 'font/woff2',
};

/**
 * Returns a Content-Type based on the file extension.
 * @param path - Request path (or embedded file path).
 * @returns MIME type string.
 */
function getMimeType(path: string): string {
    const ext = path.substring(path.lastIndexOf('.'));
    return mimeTypes[ext] || 'application/octet-stream';
}

console.log("Starting frontend server...");

/**
 * Embedded frontend HTTP server.
 * - Serves files from `embeddedFiles`.
 * - Injects runtime config into HTML pages.
 * - SPA fallback: if the path has no file extension, serves index.html.
 */
serve({
    port: frontendPort,
    async fetch(req) {
        const url = new URL(req.url);
        let filePath = url.pathname === "/" ? "index.html" : url.pathname.slice(1);
        if (filePath in embeddedFiles) {
            const content = embeddedFiles[filePath];
            if (filePath.endsWith(".html")) {
                const html = content.toString('utf-8');
                return new Response(injectConfig(html), {
                    headers: { "Content-Type": "text/html" },
                });
            }
            return new Response(content, {
                headers: { "Content-Type": getMimeType(filePath) }
            });
        }
        if (!filePath.includes(".")) {
            const content = embeddedFiles["index.html"];
            const html = content.toString('utf-8');
            return new Response(injectConfig(html), {
                headers: { "Content-Type": "text/html" },
            });
        }
        return new Response("Not Found", { status: 404 });
    },
});

console.log(`\nFrontend server started!`);
console.log(`Frontend: http://localhost:${frontendPort}`);
console.log(`Backend:  ${backendUrl}`);
console.log(`\nPress Ctrl+C to stop\n`);

/**
 * Opens the browser automatically on macOS/Windows and on Linux when DISPLAY is available.
 * Skips auto-open on headless Linux environments.
 */
let openCommand;

if (process.platform === "win32") {
  openCommand = "explorer"; // Windows
} else if (process.platform === "darwin") {
  openCommand = "open";     // Mac
} else {
  openCommand = "xdg-open"; // Linux
}

Bun.spawn([openCommand, http://localhost:${frontendPort}]);