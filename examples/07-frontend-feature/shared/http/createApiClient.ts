import type { AuthAdapter } from '../auth/AuthAdapter.js';

/**
 * The typed HTTP client every feature's `api.ts` shares (see
 * `features/subscription/api.ts`). The contract is deliberately narrow: a verb
 * per HTTP method that returns parsed JSON or throws an {@link ApiError} on a
 * non-2xx. Components and hooks never call `fetch` directly — only a feature's
 * `api.ts` calls this client. That single chokepoint is the frontend's
 * equivalent of a backend service facade.
 */
export interface ApiClient {
  get<T>(path: string, init?: RequestInit): Promise<T>;
  post<T>(path: string, body?: unknown, init?: RequestInit): Promise<T>;
  put<T>(path: string, body?: unknown, init?: RequestInit): Promise<T>;
  delete<T>(path: string, init?: RequestInit): Promise<T>;
}

export interface CreateApiClientOptions {
  /** Prefixed to relative paths. Defaults to same-origin (the dev proxy forwards `/api/**`). */
  readonly baseUrl?: string;
  /** Supplies the `Authorization` header; the adapter owns token refresh. */
  readonly authAdapter?: AuthAdapter | undefined;
  /** `fetch` override — handy in unit tests. Defaults to the global. */
  readonly fetch?: typeof globalThis.fetch | undefined;
}

/** Thrown on any non-2xx response. Carries the status so callers can branch (e.g. 404 → null). */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

const JSON_MEDIA = 'application/json';

export function createApiClient(options: CreateApiClientOptions = {}): ApiClient {
  const baseUrl = options.baseUrl ?? '';
  const fetchImpl = options.fetch ?? globalThis.fetch.bind(globalThis);
  const authAdapter = options.authAdapter;

  async function request(path: string, init: RequestInit = {}): Promise<Response> {
    const headers = new Headers(init.headers);
    if (!headers.has('Accept')) headers.set('Accept', JSON_MEDIA);

    if (authAdapter) {
      const session = await authAdapter.getSession();
      if (session?.idToken && !headers.has('Authorization')) {
        headers.set('Authorization', `Bearer ${session.idToken}`);
      }
    }

    const response = await fetchImpl(`${baseUrl}${path}`, { ...init, headers });
    if (!response.ok) {
      throw new ApiError(response.status, `${init.method ?? 'GET'} ${path} → ${response.status}`);
    }
    return response;
  }

  async function readJson<T>(response: Response): Promise<T> {
    if (response.status === 204 || response.headers.get('content-length') === '0') {
      return undefined as T;
    }
    return (await response.json()) as T;
  }

  function withBody(body: unknown, init: RequestInit | undefined): RequestInit {
    if (body === undefined) return init ?? {};
    const headers = new Headers(init?.headers);
    if (!headers.has('Content-Type')) headers.set('Content-Type', JSON_MEDIA);
    return { ...init, headers, body: JSON.stringify(body) };
  }

  return {
    async get<T>(path, init) {
      return readJson<T>(await request(path, { ...init, method: 'GET' }));
    },
    async post<T>(path, body, init) {
      return readJson<T>(await request(path, withBody(body, { ...init, method: 'POST' })));
    },
    async put<T>(path, body, init) {
      return readJson<T>(await request(path, withBody(body, { ...init, method: 'PUT' })));
    },
    async delete<T>(path, init) {
      return readJson<T>(await request(path, { ...init, method: 'DELETE' }));
    },
  };
}
