import { describe, expect, it } from 'vitest';

import { ApiError, type ApiClient } from '@bookly/shared';

import { createSubscriptionApi } from '../api.js';

/**
 * A unit test for the feature's parsing boundary. It hands `api.ts` a fake
 * `ApiClient` and asserts the wire→domain mapping — including that a 404 on
 * "current subscription" is translated to `null` (a domain state), not an
 * error. No network, no MSW, no React: the smallest possible test for the
 * riskiest line in the feature.
 */

function fakeClient(handlers: {
  get?: (path: string) => unknown;
  post?: (path: string, body: unknown) => unknown;
}): ApiClient {
  return {
    get: (p: string) => Promise.resolve(handlers.get?.(p) as never),
    post: (p: string, body: unknown) => Promise.resolve(handlers.post?.(p, body) as never),
    put: () => Promise.reject(new Error('unused')),
    delete: () => Promise.reject(new Error('unused')),
  };
}

const TENANT = '11111111-1111-1111-1111-111111111111';

describe('subscription api', () => {
  it('parses the current-subscription wire payload into branded domain types', async () => {
    const api = createSubscriptionApi(
      fakeClient({
        get: () => ({
          id: 'cccccccc-1111-1111-1111-cccccccccccc',
          planId: 'aaaaaaaa-bbbb-bbbb-bbbb-aaaaaaaaaaaa',
          planName: 'Starter',
          status: 'ACTIVE',
        }),
      }),
    );

    const sub = await api.current(TENANT);

    expect(sub?.status).toBe('ACTIVE');
    expect(sub?.planName).toBe('Starter');
  });

  it('maps a 404 on current subscription to null', async () => {
    const api = createSubscriptionApi(
      fakeClient({
        get: () => {
          throw new ApiError(404, 'Not Found');
        },
      }),
    );

    expect(await api.current(TENANT)).toBeNull();
  });
});
