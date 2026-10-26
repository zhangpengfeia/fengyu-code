import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  return {
    plugins: [vue()],
    base: env.VITE_BASE_PATH || "/",
    resolve: {
      alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
    },
    server: {
      port: 5175,
      strictPort: true,
      proxy: {
        "/education-api": {
          target: env.EDUCATION_API_TARGET || "http://127.0.0.1:8081",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/education-api/, ""),
        },
        "/agent-api": {
          target: env.AGENT_API_TARGET || "http://127.0.0.1:8088",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/agent-api/, ""),
        },
        "/play": {
          target: env.EDUCATION_API_TARGET || "http://127.0.0.1:8081",
          changeOrigin: true,
        },
      },
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (
              id.includes("/node_modules/echarts/") ||
              id.includes("/node_modules/zrender/")
            )
              return "charts";
          },
        },
      },
    },
  };
});
