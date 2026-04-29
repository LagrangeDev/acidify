import {zodApiCategories} from '@saltify/milky-types';
import * as fs from 'node:fs';
import * as path from 'node:path';

const preserveAllCapitalWords = new Set([
    'csrf'
])

function snakeToPascal(str) {
    return str
        .split('_')
        .filter(Boolean)
        .map(word => {
            if (preserveAllCapitalWords.has(word)) {
                return word.toUpperCase();
            }
            return word.charAt(0).toUpperCase() + word.slice(1);
        })
        .join('');
}

const ApiCollection = Object.entries(zodApiCategories)
    .map(([category, data]) => {
        const res = Object.entries(data.apis).map(([endpoint, api]) => {
            const input = api.requestSchema ? `input: z.input<typeof types.${snakeToPascal(endpoint)}Input>` : '';
            const output = api.responseSchema ? `types.${snakeToPascal(endpoint)}Output` : 'void';
            return `${endpoint}: (${input}) => Promise<${output}>;`;
        });
        return `  // ${category} API\n  ${res.join('\n  ')}\n`;
    })
    .join('\n');

fs.writeFileSync(path.resolve('api.d.ts'), `
/* eslint-disable */
import * as types from '@saltify/milky-types';
import type z from 'zod';

export interface ApiCollection {
${ApiCollection}
}
`.trim()
);