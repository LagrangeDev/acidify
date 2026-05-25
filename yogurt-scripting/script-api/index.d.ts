import type { Event } from "@saltify/milky-types";
import type { ApiCollection } from "./api.js";

export interface HttpRequestOptions {
  method?: string;
  headers?: Record<string, string>;
  body?: string | Uint8Array;
}

export interface FileMetadata {
  isRegularFile: boolean;
  isDirectory: boolean;
  size: number;
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
  const fs: {
    exists(path: string): Promise<boolean>;
    delete(path: string, mustExist?: boolean): Promise<void>;
    createDirectories(path: string, mustCreate?: boolean): Promise<void>;
    atomicMove(source: string, destination: string): Promise<void>;
    metadataOrNull(path: string): Promise<FileMetadata | null>;
    resolve(path: string): Promise<string>;
    list(directory: string): Promise<string[]>;
    readText(path: string): Promise<string>;
    writeText(
      path: string,
      text: string,
      append?: boolean,
      createParentDirectories?: boolean,
    ): Promise<void>;
    readBytes(path: string): Promise<Int8Array>;
    writeBytes(
      path: string,
      bytes: Int8Array | Uint8Array,
      append?: boolean,
      createParentDirectories?: boolean,
    ): Promise<void>;
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
