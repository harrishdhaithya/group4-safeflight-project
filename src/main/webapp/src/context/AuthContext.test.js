import React from 'react';
import { render, waitFor, act } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';

// Helper component to expose context values
/** @param {{ onValue: function }} props */
function AuthConsumer({ onValue }) {
  const ctx = useAuth();
  onValue(ctx);
  return null;
}

const renderProvider = (onValue) =>
  render(
    <AuthProvider>
      <AuthConsumer onValue={onValue} />
    </AuthProvider>
  );

describe('AuthContext', () => {
  beforeEach(() => {
    globalThis.fetch = jest.fn();
  });

  afterEach(() => jest.clearAllMocks());

  // CTX-07: refreshMe called on mount
  test('calls refreshMe on mount', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => ({ id: 1, email: 'a@test.com' }) });
    renderProvider(() => {});
    await waitFor(() => expect(fetch).toHaveBeenCalledWith('/api/auth/me'));
  });

  // CTX-01: refreshMe success → user set
  test('refreshMe sets user on success', async () => {
    const user = { id: 1, email: 'a@test.com' };
    fetch.mockResolvedValueOnce({ ok: true, json: async () => user });
    let capturedCtx = null;
    renderProvider((c) => { capturedCtx = c; });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    fetch.mockResolvedValueOnce({ ok: true, json: async () => user });
    let result = null;
    await act(async () => { result = await capturedCtx.refreshMe(); });
    expect(result).toEqual(user);
  });

  // CTX-02: refreshMe non-ok → user set to null
  test('refreshMe sets user to null when response not ok', async () => {
    fetch.mockResolvedValueOnce({ ok: false });
    let capturedCtx = null;
    renderProvider((c) => { capturedCtx = c; });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    fetch.mockResolvedValueOnce({ ok: false });
    let result = null;
    await act(async () => { result = await capturedCtx.refreshMe(); });
    expect(result).toBeNull();
  });

  // CTX-03: refreshMe throws → returns null
  test('refreshMe returns null when fetch throws', async () => {
    fetch.mockResolvedValueOnce({ ok: false });
    let capturedCtx = null;
    renderProvider((c) => { capturedCtx = c; });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    fetch.mockRejectedValueOnce(new Error('Network'));
    let result = null;
    await act(async () => { result = await capturedCtx.refreshMe(); });
    expect(result).toBeNull();
  });

  // CTX-04: logout calls POST /api/auth/logout and sets user to null
  test('logout calls logout endpoint and clears user', async () => {
    const user = { id: 1, email: 'a@test.com' };
    fetch.mockResolvedValueOnce({ ok: true, json: async () => user });
    let capturedCtx = null;
    renderProvider((c) => { capturedCtx = c; });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    fetch.mockResolvedValueOnce({ ok: true });
    await act(async () => { await capturedCtx.logout(); });
    expect(fetch).toHaveBeenCalledWith('/api/auth/logout', expect.objectContaining({ method: 'POST' }));
    expect(capturedCtx.user).toBeNull();
  });

  // CTX-05: logout fetch throws → user still null (finally block)
  test('logout clears user even when fetch throws', async () => {
    fetch.mockResolvedValueOnce({ ok: false });
    let capturedCtx = null;
    renderProvider((c) => { capturedCtx = c; });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    fetch.mockRejectedValueOnce(new Error('Network'));
    await act(async () => { await capturedCtx.logout(); });
    expect(capturedCtx.user).toBeNull();
  });

  // CTX-06: useAuth outside provider → throws
  test('useAuth throws when used outside AuthProvider', () => {
    const spy = jest.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<AuthConsumer onValue={() => {}} />)).toThrow(
      'useAuth must be used within AuthProvider'
    );
    spy.mockRestore();
  });
});
