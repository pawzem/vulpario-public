import { ApiError, type ApiClient } from '@bookly/shared';

import {
  PlanId,
  SubscriptionId,
  type Plan,
  type PlanVisibility,
  type Subscription,
  type SubscriptionStatus,
} from './types.js';

/**
 * `api.ts` is the ONLY file in the feature that talks to the HTTP client.
 * Components and hooks call this; they never `fetch`. Its second job is the
 * anti-corruption boundary: it owns the `*Wire` shapes (what the server
 * actually sends) and parses them into the feature's domain types — branding
 * ids, narrowing unions — so nothing downstream handles raw wire data.
 */

interface SubscriptionWire {
  readonly id: string;
  readonly planId: string;
  readonly planName: string | null;
  readonly status: SubscriptionStatus;
}

interface PlanWire {
  readonly id: string;
  readonly name: string;
  readonly active: boolean;
  readonly visibility: PlanVisibility;
  readonly priceMinorUnits: number | null;
  readonly priceCurrency: string | null;
}

interface PageWire<T> {
  readonly content: readonly T[];
  readonly page: { readonly size: number; readonly number: number; readonly totalElements: number };
}

export interface SubscriptionApi {
  current(tenantId: string): Promise<Subscription | null>;
  availablePlans(tenantId: string): Promise<readonly Plan[]>;
  suspend(tenantId: string): Promise<Subscription>;
  resume(tenantId: string): Promise<Subscription>;
}

export function createSubscriptionApi(client: ApiClient): SubscriptionApi {
  return {
    async current(tenantId) {
      try {
        return parseSubscription(await client.get<SubscriptionWire>(`/api/v1/subscription/${tenantId}`));
      } catch (err) {
        // A 404 means "no current subscription yet" — a domain state, not an error.
        if (err instanceof ApiError && err.status === 404) return null;
        throw err;
      }
    },
    async availablePlans(tenantId) {
      const wire = await client.get<PageWire<PlanWire>>(
        `/api/v1/subscription/${tenantId}/available-plans?size=100`,
      );
      return wire.content.map(parsePlan);
    },
    async suspend(tenantId) {
      return parseSubscription(await client.post<SubscriptionWire>(`/api/v1/subscription/${tenantId}/suspend`, {}));
    },
    async resume(tenantId) {
      return parseSubscription(await client.post<SubscriptionWire>(`/api/v1/subscription/${tenantId}/resume`, {}));
    },
  };
}

function parseSubscription(wire: SubscriptionWire): Subscription {
  return {
    id: SubscriptionId(wire.id),
    planId: PlanId(wire.planId),
    planName: wire.planName,
    status: wire.status,
  };
}

function parsePlan(wire: PlanWire): Plan {
  return {
    id: PlanId(wire.id),
    name: wire.name,
    active: wire.active,
    visibility: wire.visibility,
    priceMinorUnits: wire.priceMinorUnits,
    priceCurrency: wire.priceCurrency,
  };
}
