import { readdir, readFile } from "node:fs/promises"
import { join } from "node:path"
async function walk(dir) { const entries = await readdir(dir, { withFileTypes: true }); const files = []; for (const entry of entries) { const path = join(dir, entry.name); if (entry.isDirectory()) files.push(...await walk(path)); else if (/\.(ts|tsx)$/.test(entry.name)) files.push(path) } return files }
const files = await walk("src")
const violations = []
for (const file of files) {
  const source = await readFile(file, "utf8")
  const normalized = file.replaceAll("\\", "/")
  const imports = [...source.matchAll(/from\s+["']([^"']+)["']/g)].map((match) => match[1])
  if (/export default/.test(source)) violations.push(`${file}: default export`)
  if (/role\s*[!=]==?/.test(source) && !normalized.includes("domain/permissions/")) violations.push(`${file}: role comparison outside permissions`)
  if (normalized.includes("src/features/") && imports.some((value) => value.includes("mocks"))) violations.push(`${file}: features cannot import mocks directly`)
  if (normalized.includes("src/store/") && imports.some((value) => value.includes("mocks"))) violations.push(`${file}: store cannot import mocks directly`)
  if (normalized.includes("src/shared/") && imports.some((value) => value.includes("features") || value.includes("store") || value.includes("domain") || value.includes("api") || value.includes("mocks"))) violations.push(`${file}: shared layer has an upward import`)
}
if (violations.length) { console.error(violations.join("\n")); process.exit(1) }
console.log(`linted ${files.length} TypeScript files`)
