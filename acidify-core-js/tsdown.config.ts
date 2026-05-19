import { defineConfig } from 'tsdown';

export default defineConfig({
  entry: ['../acidify-core/build/dist/js/productionLibrary/acidify-acidify-core.mjs'],
  format: 'esm',
  clean: true,
  dts: false,
  target: false,
});