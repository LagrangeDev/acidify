import { defineConfig } from 'tsup';

export default defineConfig({
  entry: ['../acidify-core/build/dist/js/productionLibrary/acidify-acidify-core.mjs'],
  format: 'cjs',
  clean: true,
});