import React from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';

// Helper component to expose context values
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
    global.fetch = jest.fn();
  });

  afterEach(() => jest.clearAllMocks());

  // CTX-07: refreshMe called on mount
  test('calls refreshMe on mount', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => ({ id: 1, email: 'a@test.com' }) });
    let ctx;
    renderProvider((c) => { ctx = c; });
    await waitFor(() => expect(fetch).toHaveBeenCalledWith('/api/auth/me'));
  });

  // CTX-01: refreshMe success → user set
  test('refreshMe sets user on success', async () => {
    const user = { id: 1, email: 'a@test.com' };
    fetch.mockResolvedValueOnce({ ok: true, json: async () => user });
    let ctx;
    renderProvider((c) => { ctx = c; });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    // call refreshMe again explicitly
    fetch.mockResolvedValueOnce({ ok: true, json: async () => user });
    let result;
    await act(async () => { result = await ctx.refreshMe(); });
    expect(result).toEqual(user);
  });

  // CTX-02: refreshMe non-ok → user set to null
  test('refreshMe sets user to null when response not ok', async () => {
    fetch.mockResolvedValueOnce({ ok: false });
    let ctx;
    renderProvider((c) => { ctx = c; });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    fetch.mockResolvedValueOnce({ ok: false });
    let result;
    await act(async () => { result = await ctx.refreshMe(); });
    expect(result).toBeNull();
  });

  // CTX-03: refreshMe throws → returns null
  test('refreshMe returns null when fetch throws', async () => {
    fetch.mockResolvedValueOnce({ ok: false });
    let ctx;
    renderProvider((c) => { ctx = c; });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    fetch.mockRejectedValueOnce(new Error('Network'));
    let result;
    await act(async () => { result = await ctx.refreshMe(); });
    expect(result).toBeNull();
  });

  // CTX-04: logout calls POST /api/auth/logout and sets user to null
  test('logout calls logout endpoint and clears user', async () => {
    const user = { id: 1, email: 'a@test.com' };
    fetch.mockResolvedValueOnce({ ok: true, json: async () => user });
    let ctx;
    renderProvider((c) => { ctx = c; });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    fetch.mockResolvedValueOnce({ ok: true });
    await act(async () => { await ctx.logout(); });
    expect(fetch).toHaveBeenCalledWith('/api/auth/logout', expect.objectContaining({ method: 'POST' }));
    expect(ctx.user).toBeNull();
  });

  // CTX-05: logout fetch throws → user still null (finally block)
  test('logout clears user even when fetch throws', async () => {
    fetch.mockResolvedValueOnce({ ok: false });
    let ctx;
    renderProvider((c) => { ctx = c; });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    fetch.mockRejectedValueOnce(new Error('Network'));
    await act(async () => { await ctx.logout(); });
    expect(ctx.user).toBeNull();
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
