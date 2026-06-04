/**
 * Domain types for the subscription feature. Note the *branded* ids: a
 * `PlanId` is a `string` the compiler refuses to mix up with a
 * `SubscriptionId` or a bare string. They're constructed (and validated) only
 * at the API boundary in `api.ts`, so an unparsed wire string can never leak
 * into the UI as a domain id. This is the frontend echo of the backend's
 * `TenantId` / `PlanId` value objects.
 */

declare const planIdBrand: unique symbol;
export type PlanId = string & { readonly [planIdBrand]: 'PlanId' };

declare const subscriptionIdBrand: unique symbol;
export type SubscriptionId = string & { readonly [subscriptionIdBrand]: 'SubscriptionId' };

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function PlanId(raw: string): PlanId {
  if (!UUID_RE.test(raw)) throw new Error(`Invalid PlanId: ${raw}`);
  return raw as PlanId;
}

export function SubscriptionId(raw: string): SubscriptionId {
  if (!UUID_RE.test(raw)) throw new Error(`Invalid SubscriptionId: ${raw}`);
  return raw as SubscriptionId;
}

/** A discriminated set of states — the UI switches on these exhaustively. */
export type SubscriptionStatus = 'ACTIVE' | 'SUSPENDED' | 'CANCELLED';

export type PlanVisibility = 'PUBLIC' | 'INTERNAL';

export interface Subscription {
  readonly id: SubscriptionId;
  readonly planId: PlanId;
  readonly planName: string | null;
  readonly status: SubscriptionStatus;
}

export interface Plan {
  readonly id: PlanId;
  readonly name: string;
  readonly active: boolean;
  readonly visibility: PlanVisibility;
  /** Monthly price in minor units (cents); null when no price is set. */
  readonly priceMinorUnits: number | null;
  readonly priceCurrency: string | null;
}
