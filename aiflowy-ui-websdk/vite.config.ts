import { defineConfig } from "vite";
import { resolve } from "path";
import dts from "unplugin-dts/vite";

export default defineConfig({
  build: {
    lib: {
      entry: resolve(__dirname, "src/index.ts"),
      name: "AiflowySDK",
      formats: ["es", "umd"],
      fileName: (format) => `index.${format === "es" ? "esm.js" : "js"}`,
    },
    rolldownOptions: {
      output: {
        exports: "named",
      },
    },
    outDir: "dist",
    sourcemap: true,
  },
  plugins: [dts({ entryRoot: resolve(__dirname, "src") })],
});
