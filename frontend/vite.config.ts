import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Dev: proxy /api to the Spring Boot app so the SPA talks to the real backend
// with no CORS. Build: emit into Spring's static resources so the whole app
// ships as a single jar. (static/ is gitignored; built by Maven at package time.)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": { target: "http://localhost:8080", changeOrigin: true },
    },
  },
  build: {
    outDir: "../src/main/resources/static",
    emptyOutDir: true,
  },
});
