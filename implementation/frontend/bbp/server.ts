import { serve } from "bun";
import { parseArgs } from "util";

// ============================================================================
// CLI Arguments Parsing
// ============================================================================

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

// Validate required API key
if (!mapboxApiKey) {
    console.error("Error: --mapbox.api.key is required");
    console.log("\nUsage:");
    console.log("  ./bbp-frontend --mapbox.api.key=YOUR_KEY [--frontend.port=3000] [--backend.port=8080]");
    process.exit(1);
}

console.log("Starting frontend server...");

const backendUrl = `http://localhost:${backendPort}`;

// ============================================================================
// HTTP Server
// ============================================================================

serve({
    port: frontendPort,
    async fetch(req) {
        const url = new URL(req.url);
        let filePath = url.pathname;

        // Redirect root to index.html
        if (filePath === "/") {
            filePath = "/index.html";
        }

        const file = Bun.file(`./dist${filePath}`);

        // Serve existing files
        if (await file.exists()) {
            // Inject runtime configuration into HTML files
            if (filePath.endsWith(".html")) {
                let html = await file.text();

                // Inject Mapbox API key and backend URL as global variables
                const keyScript = `
          <script>
            window.MAPBOX_API_KEY = "${mapboxApiKey}";
            window.BACKEND_URL = "${backendUrl}";
          </script>
        `;
                html = html.replace("</head>", `${keyScript}</head>`);

                return new Response(html, {
                    headers: { "Content-Type": "text/html" },
                });
            }

            // Serve other files as-is (CSS, JS, images, etc.)
            return new Response(file);
        }

        // SPA fallback: serve index.html for client-side routes
        if (!filePath.includes(".")) {
            const indexFile = Bun.file("./dist/index.html");
            let html = await indexFile.text();

            const keyScript = `
        <script>
          window.MAPBOX_API_KEY = "${mapboxApiKey}";
          window.BACKEND_URL = "${backendUrl}";
        </script>
      `;
            html = html.replace("</head>", `${keyScript}</head>`);

            return new Response(html, {
                headers: { "Content-Type": "text/html" },
            });
        }

        return new Response("Not Found", { status: 404 });
    },
});

// ============================================================================
// Startup Info & Browser Launch
// ============================================================================

console.log(`\nFrontend server started!`);
console.log(`Frontend: http://localhost:${frontendPort}`);
console.log(`Backend:  ${backendUrl}`);
console.log(`\nPress Ctrl+C to stop\n`);

// Auto-open browser (skip on headless Linux servers)
if (process.platform !== "linux" || process.env.DISPLAY) {
    const openCommand =
        process.platform === "darwin"
            ? "open"
            : process.platform === "win32"
                ? "start"
                : "xdg-open";

    Bun.spawn([openCommand, `http://localhost:${frontendPort}`]);
}