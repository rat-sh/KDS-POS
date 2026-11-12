import { readdir, readFile } from "node:fs/promises"
import { join, relative } from "node:path"
import { fileURLToPath } from "node:url"

const root = fileURLToPath(new URL("../src/", import.meta.url))
const forbidden = new Set(["App.tsx", "data.ts", "index.css"])
const exceptions = (file) => /(?:\.types|\.constants|\.mock|\.module\.css|\.test)\.(?:ts|tsx|css)$/.test(file) || file.endsWith("index.ts")
const walk = async (dir) => { const entries = await readdir(dir, { withFileTypes: true }); const files = []; for (const entry of entries) { const path = join(dir, entry.name); if (entry.isDirectory()) files.push(...await walk(path)); else files.push(path) } return files }
const files = await walk(root)
const violations = []
for (const file of files) {
  const name = file.split(/[\\/]/).pop()
  const source = await readFile(file, "utf8")
  const lines = source.split(/\r?\n/).length
  if (forbidden.has(name)) violations.push(`${relative(root, file)}: forbidden legacy file`)
  const sourceFile = /\.(ts|tsx)$/.test(name)
  const limit = sourceFile ? 120 : 200
  if (lines > limit) violations.push(`${relative(root, file)}: ${lines} lines exceeds ${limit}`)
  if (!exceptions(name) && /\.(ts|tsx)$/.test(name)) {
    const declarations = source.match(/^(?:export\s+)?(?:async\s+)?function\s+\w+/gm) || []
    const arrows = source.match(/^(?:export\s+)?const\s+\w+\s*=.*=>/gm) || []
    if (declarations.length + arrows.length > 1) violations.push(`${relative(root, file)}: more than one top-level function`)
  }
}
if (violations.length) { console.error(violations.join("\n")); process.exit(1) }
console.log(`checked ${files.length} source files; structure is within limits`)
