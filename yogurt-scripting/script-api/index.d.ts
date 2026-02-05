import type { Event } from "@saltify/milky-types";
import type { ApiCollection } from "./api.js";

export interface HttpRequestOptions {
  method?: string;
  headers?: Record<string, string>;
  body?: string | Uint8Array;
}

declare global {
  const console: {
    log(...args: unknown[]): void;
    info(...args: unknown[]): void;
    warn(...args: unknown[]): void;
    error(...args: unknown[]): void;
    debug(...args: unknown[]): void;
    assert(condition?: boolean, ...args: unknown[]): void;
    trace(...args: unknown[]): void;
    group(...args: unknown[]): void;
    groupEnd(): void;
    time(label?: string): void;
    timeEnd(label?: string): void;
    count(label?: string): void;
    table(data?: unknown): void;
  };
  const http: {
    request(url: string, options?: HttpRequestOptions): Promise<string>;
    requestBytes(url: string, options?: HttpRequestOptions): Promise<Int8Array>;
  };
  const yogurt: {
    api: ApiCollection;
    event: {
      on<T extends Event["event_type"]>(
        eventType: T,
        listener: (event: Extract<Event, { event_type: T }>) => void,
      ): void;
    };
  };
}
