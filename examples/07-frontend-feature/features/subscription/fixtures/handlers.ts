import { http, HttpResponse } from 'msw';

/**
 * MSW request handlers — the feature's HTTP test fixtures. Integration tests
 * (React Testing Library + MSW) render real components against these, so the
 * component, the hook, and `api.ts`'s wire→domain parsing are all exercised
 * with no network and no backend. Fixtures live inside the feature folder, so
 * everything one feature needs is in one place.
 */

const TENANT = '11111111-1111-1111-1111-111111111111';
const PLAN_STARTER = 'aaaaaaaa-bbbb-bbbb-bbbb-aaaaaaaaaaaa';
const PLAN_PRO = 'aaaaaaaa-bbbb-bbbb-bbbb-bbbbbbbbbbbb';
const SUBSCRIPTION = 'cccccccc-1111-1111-1111-cccccccccccc';

export const sampleSubscription = {
  id: SUBSCRIPTION,
  planId: PLAN_STARTER,
  planName: 'Starter',
  status: 'ACTIVE',
};

export const samplePlansPage = {
  content: [
    { id: PLAN_STARTER, name: 'Starter', active: true, visibility: 'PUBLIC', priceMinorUnits: 4900, priceCurrency: 'EUR' },
    { id: PLAN_PRO, name: 'Pro', active: true, visibility: 'PUBLIC', priceMinorUnits: 14900, priceCurrency: 'EUR' },
  ],
  page: { size: 100, number: 0, totalElements: 2 },
};

export const subscriptionHandlers = [
  http.get(`/api/v1/subscription/${TENANT}`, () => HttpResponse.json(sampleSubscription)),
  http.get(`/api/v1/subscription/${TENANT}/available-plans`, () => HttpResponse.json(samplePlansPage)),
  http.post(`/api/v1/subscription/${TENANT}/suspend`, () =>
    HttpResponse.json({ ...sampleSubscription, status: 'SUSPENDED' })),
  http.post(`/api/v1/subscription/${TENANT}/resume`, () =>
    HttpResponse.json({ ...sampleSubscription, status: 'ACTIVE' })),
];
