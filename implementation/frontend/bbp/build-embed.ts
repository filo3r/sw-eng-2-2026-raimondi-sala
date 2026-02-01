import { readdirSync, statSync, readFileSync, writeFileSync } from 'fs';
import { join } from 'path';

/**
 * Recursively scans a directory and returns all file paths.
 * @param dir - Directory to scan
 * @param base - Base path for relative file names (used in recursion)
 * @returns Array of relative file paths
 */
function getAllFiles(dir: string, base = ''): string[] {
    const files: string[] = [];
    for (const file of readdirSync(dir)) {
        const path = join(dir, file);
        const relative = base ? `${base}/${file}` : file;
        if (statSync(path).isDirectory()) {
            // Recursively scan subdirectories
            files.push(...getAllFiles(path, relative));
        } else {
            files.push(relative);
        }
    }
    return files;
}

// Get all files from dist directory
const files = getAllFiles('./dist');

// Generate TypeScript code with embedded file contents as base64-encoded Buffers
const imports = files.map(f => {
    const content = readFileSync(`./dist/${f}`, 'base64');
    return `  "${f}": Buffer.from("${content}", "base64")`;
}).join(',\n');

// Build the complete TypeScript module code
const code = `export const embeddedFiles: Record<string, Buffer> = {
${imports}
};
`;

// Write the generated code to embedded-files.ts
writeFileSync('./embedded-files.ts', code);