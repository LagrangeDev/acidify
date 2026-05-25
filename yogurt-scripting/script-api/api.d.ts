import type z from 'zod';

import type { zodApiEndpoints } from './milky-types.js';

type ZodApiEndpoints = typeof zodApiEndpoints;

type RequiredKeys<T> = {
  [K in keyof T]-?: {} extends Pick<T, K> ? never : K;
}[keyof T];

type AllOptional<T> = RequiredKeys<T> extends never ? true : false;

type RawApiEndpoint<E extends keyof ZodApiEndpoints> = {
  request: ZodApiEndpoints[E]['requestSchema'] extends null ? null : z.input<ZodApiEndpoints[E]['requestSchema']>;
  response: ZodApiEndpoints[E]['responseSchema'] extends null ? null : z.output<ZodApiEndpoints[E]['responseSchema']>;
};

type ApiEndpointFunction<E extends keyof ZodApiEndpoints> = RawApiEndpoint<E>['request'] extends null
  ? () => Promise<RawApiEndpoint<E>['response']>
  : AllOptional<RawApiEndpoint<E>['request']> extends true
    ? (params?: RawApiEndpoint<E>['request']) => Promise<RawApiEndpoint<E>['response']>
    : (params: RawApiEndpoint<E>['request']) => Promise<RawApiEndpoint<E>['response']>;

export type ApiCollection = {
  [E in keyof ZodApiEndpoints]: ApiEndpointFunction<E>;
};