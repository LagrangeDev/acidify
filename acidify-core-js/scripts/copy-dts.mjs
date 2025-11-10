import fs from 'node:fs';

fs.copyFileSync(
  '../acidify-core/build/dist/js/productionLibrary/acidify-acidify-core.d.mts',
  'dist/acidify-acidify-core.d.ts'
);
console.log('Copied DTS to dist');