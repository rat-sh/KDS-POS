import { defineConfig, loadEnv } from "vite"
import react from "@vitejs/plugin-react"
import tailwindcss from "@tailwindcss/vite"

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "")
  const backend = env.VITE_API_PROXY_TARGET || "http://localhost:8080"
  return {
  plugins: [react(), tailwindcss()],
  server: {
    host: "0.0.0.0",
    port: Number(process.env.PORT || env.PORT || 5173),
    strictPort: true,
    proxy: {
      "/api": { target: backend, changeOrigin: true },
      "/actuator": { target: backend, changeOrigin: true },
    },
  },
  preview: { host: "0.0.0.0", port: 4173 },
  build: { sourcemap: false },
  }
})
