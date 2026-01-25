/**
 * Production server for serving the compiled Vue frontend.
 * Injects runtime configuration (Mapbox API key, backend URL) into HTML.
 */
import { serve } from "bun";
import { parseArgs } from "util";

// Parse CLI arguments
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

const mapboxApiKey = values["mapbox.api.key"];
const frontendPort = parseInt(values["frontend.port"] || "3000");
const backendPort = values["backend.port"] || "8080";
const backendUrl = `http://localhost:${backendPort}`;
const distPath = "./dist";

// Validate required API key
if (!mapboxApiKey) {
    console.error("Error: --mapbox.api.key is required");
    console.log("\nUsage:");
    console.log("  ./bbp-frontend --mapbox.api.key=YOUR_KEY [--frontend.port=3000] [--backend.port=8080]");
    process.exit(1);
}

/**
 * Injects runtime configuration into HTML.
 */
function injectConfig(html: string): string {
    const configScript = `
  <script>
    window.MAPBOX_API_KEY = "${mapboxApiKey}";
    window.BACKEND_URL = "${backendUrl}";
  </script>`;
    return html.replace("</head>", `${configScript}</head>`);
}

console.log("Starting frontend server...");

serve({
    port: frontendPort,
    async fetch(req) {
        const url = new URL(req.url);
        let filePath = url.pathname === "/" ? "/index.html" : url.pathname;
        const file = Bun.file(`${distPath}${filePath}`);
        // Serve existing files
        if (await file.exists()) {
            if (filePath.endsWith(".html")) {
                const html = await file.text();
                return new Response(injectConfig(html), {
                    headers: { "Content-Type": "text/html" },
                });
            }
            return new Response(file);
        }
        // SPA fallback: serve index.html for client-side routes
        if (!filePath.includes(".")) {
            const indexFile = Bun.file(`${distPath}/index.html`);
            const html = await indexFile.text();
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

// Auto-open browser (skip on headless servers)
if (process.platform !== "linux" || process.env.DISPLAY) {
    const openCommand =
        process.platform === "darwin" ? "open" :
            process.platform === "win32" ? "start" : "xdg-open";
    Bun.spawn([openCommand, `http://localhost:${frontendPort}`]);
}