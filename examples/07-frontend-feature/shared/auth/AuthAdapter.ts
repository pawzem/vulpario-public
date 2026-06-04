/**
 * The auth port — the frontend's port/adapter seam (the mirror of the
 * backend's `EmailSender` in example 03). The app depends on this interface;
 * a concrete adapter wraps whatever identity library is in use. Swapping the
 * IdP SDK is a new adapter, not a change to every component that needs a
 * token.
 *
 * Concrete adapters (e.g. an OIDC-client adapter, a hosted-auth adapter) live
 * in `shared/auth/` and are chosen at app bootstrap. Provider/account
 * specifics are out of scope for this reference repo.
 */
export interface AuthSession {
  readonly idToken: string;
  /** Epoch millis. The adapter is the single place that knows about refresh. */
  readonly expiresAt: number;
}

export interface AuthAdapter {
  /** The current session, refreshing transparently if needed; null when signed out. */
  getSession(): Promise<AuthSession | null>;
  signIn(): Promise<void>;
  signOut(): Promise<void>;
}
