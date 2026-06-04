import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useApiClient, useTenant } from '@bookly/shared';

import { createSubscriptionApi } from '../api.js';
import type { Plan, Subscription } from '../types.js';

/**
 * Hooks are the only thing components use. Server state lives in TanStack
 * Query's cache and is never copied into `useState` — the cache is the single
 * source of truth, and a mutation invalidates the relevant query keys so every
 * view re-reads. (`useApiClient` / `useTenant` come from React context, the
 * app-wide composition root — also in `shared`.)
 */

function useApi() {
  return createSubscriptionApi(useApiClient());
}

export function useCurrentSubscription() {
  const api = useApi();
  const tenantId = useTenant();
  return useQuery<Subscription | null>({
    queryKey: ['subscription', tenantId],
    queryFn: () => api.current(tenantId),
  });
}

export function useAvailablePlans() {
  const api = useApi();
  const tenantId = useTenant();
  return useQuery<readonly Plan[]>({
    queryKey: ['subscription-available-plans', tenantId],
    queryFn: () => api.availablePlans(tenantId),
  });
}

export function useSuspendSubscription() {
  const api = useApi();
  const tenantId = useTenant();
  const qc = useQueryClient();
  return useMutation<Subscription, Error, void>({
    mutationFn: () => api.suspend(tenantId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['subscription', tenantId] });
    },
  });
}

export function useResumeSubscription() {
  const api = useApi();
  const tenantId = useTenant();
  const qc = useQueryClient();
  return useMutation<Subscription, Error, void>({
    mutationFn: () => api.resume(tenantId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['subscription', tenantId] });
    },
  });
}
