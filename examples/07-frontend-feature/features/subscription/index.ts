/**
 * The feature's public surface — its "contract", the frontend echo of a
 * backend BC's `contract` package. Other parts of the app import from
 * `features/subscription` (this barrel) and nothing deeper. The
 * `eslint-plugin-boundaries` config forbids reaching into
 * `features/subscription/hooks/...` or `.../api.js` from outside; only what's
 * re-exported here is reachable.
 */
export {
  useCurrentSubscription,
  useAvailablePlans,
  useSuspendSubscription,
  useResumeSubscription,
} from './hooks/useSubscription.js';

export { createSubscriptionApi, type SubscriptionApi } from './api.js';

export { PlanId, SubscriptionId } from './types.js';
export type { Plan, Subscription, SubscriptionStatus, PlanVisibility } from './types.js';
