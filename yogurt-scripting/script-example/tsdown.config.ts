import packageJson from "./package.json" with { type: "json" };
import { defineConfig } from "tsdown";

export default defineConfig({
  entry: "./src/index.ts",
  outputOptions: {
    entryFileNames: `${packageJson.name.split("/").pop()}.yogurtx.js`,
  },
  format: "esm",
});
