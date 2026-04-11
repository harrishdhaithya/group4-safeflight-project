import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProfilePage from './ProfilePage';

const mockRefreshMe = jest.fn();

jest.mock('../context/AuthContext', () => ({
  useAuth: () => ({ refreshMe: mockRefreshMe })
}));

const mockUser = {
  fname: 'John',
  lname: 'Doe',
  email: 'john@test.com',
  phone: '123456',
  dob: '1990-01-01',
  country: 'AU'
};

const renderPage = (user) =>
  render(
    <MemoryRouter>
      <ProfilePage user={user} />
    </MemoryRouter>
  );

describe('ProfilePage', () => {
  beforeEach(() => {
    globalThis.fetch = jest.fn();
    mockRefreshMe.mockClear();
  });

  afterEach(() => jest.clearAllMocks());

  // PROF-01: user null → not logged in
  test('shows not logged in warning when user is null', () => {
    renderPage(null);
    expect(screen.getByText(/You are not logged in/)).toBeInTheDocument();
  });

  // PROF-02/03: user present → form with data; email disabled
  test('renders profile form with user data', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    expect(document.querySelector('input[name="fname"]').value).toBe('John');
    expect(document.querySelector('input[name="lname"]').value).toBe('Doe');
    const emailInput = document.querySelector('input[type="email"]');
    expect(emailInput).toBeDisabled();
  });

  // PROF-04: fetch /api/bookings/me called on mount
  test('fetches bookings on mount when user present', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalledWith('/api/bookings/me'));
  });

  // PROF-05: user null → fetch NOT called
  test('does not fetch bookings when user is null', () => {
    renderPage(null);
    expect(fetch).not.toHaveBeenCalled();
  });

  // PROF-06: loading state
  test('shows loading state while fetching bookings', () => {
    fetch.mockImplementationOnce(() => new Promise(() => {}));
    renderPage(mockUser);
    expect(screen.getByText('Loading bookings...')).toBeInTheDocument();
  });

  // PROF-07: fetch throws → error shown
  test('shows error when fetch throws', async () => {
    fetch.mockRejectedValueOnce(new Error('Network error'));
    renderPage(mockUser);
    expect(await screen.findByText(/Network error/)).toBeInTheDocument();
  });

  // PROF-08: fetch non-ok → error shown
  test('shows error when fetch returns non-ok', async () => {
    fetch.mockResolvedValueOnce({ ok: false });
    renderPage(mockUser);
    expect(await screen.findByText(/Failed to load bookings/)).toBeInTheDocument();
  });

  // PROF-09: empty bookings → "no bookings" message
  test('shows no bookings message when list is empty', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderPage(mockUser);
    expect(await screen.findByText(/You have no bookings yet/)).toBeInTheDocument();
  });

  // PROF-10: bookings present → table row
  test('shows booking table when bookings present', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [{
        bookingId: 1, flightCode: 'SF101', fromAirport: 'SYD', toAirport: 'MEL',
        travelDate: '2026-06-01', travelTime: '08:30', status: 'CONFIRMED',
        paymentStatus: 'SUCCESS', seats: ['1A']
      }]
    });
    renderPage(mockUser);
    expect(await screen.findByText('SF101')).toBeInTheDocument();
  });

  // PROF-11: paymentStatus SUCCESS → "Paid" badge
  test('shows Paid badge for SUCCESS payment', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [{ bookingId: 1, flightCode: 'X', fromAirport: 'A', toAirport: 'B', status: 'CONFIRMED', paymentStatus: 'SUCCESS', seats: [] }]
    });
    renderPage(mockUser);
    expect(await screen.findByText('Paid')).toBeInTheDocument();
  });

  // PROF-12: paymentStatus non-SUCCESS → raw value with bg-secondary badge
  test('shows raw paymentStatus for non-SUCCESS', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [{ bookingId: 1, flightCode: 'X', fromAirport: 'A', toAirport: 'B', status: 'CONFIRMED', paymentStatus: 'FAILED', seats: [] }]
    });
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    const badge = await screen.findByText('FAILED');
    expect(badge).toHaveClass('bg-secondary');
  });

  // PROF-13: paymentStatus absent → "—"
  test('shows dash when paymentStatus absent', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [{ bookingId: 1, flightCode: 'X', fromAirport: 'A', toAirport: 'B', status: 'CONFIRMED', paymentStatus: undefined, seats: [] }]
    });
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    expect(await screen.findByText('—')).toBeInTheDocument();
  });

  // PROF-14: Edit button → fields become editable
  test('clicking Edit makes form fields editable', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /Edit/ }));
    expect(document.querySelector('input[name="fname"]')).not.toBeDisabled();
  });

  // PROF-15: typing in editable field updates value
  test('typing in fname field updates value when editing', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /Edit/ }));
    fireEvent.change(document.querySelector('input[name="fname"]'), { target: { value: 'Jane' } });
    expect(document.querySelector('input[name="fname"]').value).toBe('Jane');
  });

  // PROF-16: Save submits PUT /api/auth/me
  test('Save button submits PUT to /api/auth/me', async () => {
    fetch
      .mockResolvedValueOnce({ ok: true, json: async () => [] })
      .mockResolvedValueOnce({ ok: true, json: async () => mockUser });
    mockRefreshMe.mockResolvedValueOnce(mockUser);
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    fireEvent.click(screen.getByRole('button', { name: /Edit/ }));
    fireEvent.click(screen.getByRole('button', { name: /^Save$/ }));
    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith('/api/auth/me', expect.objectContaining({ method: 'PUT' }))
    );
  });

  // PROF-17: save succeeds → success message; editing exits
  test('shows success message after successful save', async () => {
    fetch
      .mockResolvedValueOnce({ ok: true, json: async () => [] })
      .mockResolvedValueOnce({ ok: true, json: async () => mockUser });
    mockRefreshMe.mockResolvedValueOnce(mockUser);
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    fireEvent.click(screen.getByRole('button', { name: /Edit/ }));
    fireEvent.click(screen.getByRole('button', { name: /^Save$/ }));
    expect(await screen.findByText(/Profile updated successfully/)).toBeInTheDocument();
    expect(document.querySelector('input[name="fname"]')).toBeDisabled();
  });

  // PROF-18: save fails with error text → error message
  test('shows error message when save fails', async () => {
    fetch
      .mockResolvedValueOnce({ ok: true, json: async () => [] })
      .mockResolvedValueOnce({ ok: false, text: async () => 'Update failed' });
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    fireEvent.click(screen.getByRole('button', { name: /Edit/ }));
    fireEvent.click(screen.getByRole('button', { name: /^Save$/ }));
    expect(await screen.findByText('Update failed')).toBeInTheDocument();
  });

  // PROF-19: save fails empty body → fallback error
  test('shows fallback error when save fails with empty body', async () => {
    fetch
      .mockResolvedValueOnce({ ok: true, json: async () => [] })
      .mockResolvedValueOnce({ ok: false, text: async () => '' });
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    fireEvent.click(screen.getByRole('button', { name: /Edit/ }));
    fireEvent.click(screen.getByRole('button', { name: /^Save$/ }));
    expect(await screen.findByText('Update failed')).toBeInTheDocument();
  });

  // PROF-20: Cancel restores original values
  test('Cancel exits editing and restores original values', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /Edit/ }));
    fireEvent.change(document.querySelector('input[name="fname"]'), { target: { value: 'Changed' } });
    fireEvent.click(screen.getByRole('button', { name: /Cancel/ }));
    expect(document.querySelector('input[name="fname"]').value).toBe('John');
    expect(document.querySelector('input[name="fname"]')).toBeDisabled();
  });

  // PROF-21: success message → text-success class
  test('success message has text-success class', async () => {
    fetch
      .mockResolvedValueOnce({ ok: true, json: async () => [] })
      .mockResolvedValueOnce({ ok: true, json: async () => mockUser });
    mockRefreshMe.mockResolvedValueOnce(mockUser);
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    fireEvent.click(screen.getByRole('button', { name: /Edit/ }));
    fireEvent.click(screen.getByRole('button', { name: /^Save$/ }));
    const msg = await screen.findByText(/Profile updated successfully/);
    expect(msg).toHaveClass('text-success');
  });

  // PROF-22: error message → text-danger class
  test('error message has text-danger class', async () => {
    fetch
      .mockResolvedValueOnce({ ok: true, json: async () => [] })
      .mockResolvedValueOnce({ ok: false, text: async () => 'Oops' });
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    fireEvent.click(screen.getByRole('button', { name: /Edit/ }));
    fireEvent.click(screen.getByRole('button', { name: /^Save$/ }));
    const msg = await screen.findByText('Oops');
    expect(msg).toHaveClass('text-danger');
  });

  // PROF-23/24: Saving... state while fetch in progress
  test('shows Saving... and disables buttons while saving', async () => {
    fetch
      .mockResolvedValueOnce({ ok: true, json: async () => [] })
      .mockImplementationOnce(() => new Promise(() => {}));
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    fireEvent.click(screen.getByRole('button', { name: /Edit/ }));
    fireEvent.click(screen.getByRole('button', { name: /^Save$/ }));
    expect(await screen.findByText('Saving...')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Cancel/ })).toBeDisabled();
  });

  // PROF-25: user.dob present → dob field pre-filled
  test('pre-fills dob field from user.dob', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /Edit/ }));
    expect(document.querySelector('input[name="dob"]').value).toBe('1990-01-01');
  });

  // PROF-26: user.dob absent → dob field empty
  test('dob field is empty when user.dob absent', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => [] });
    renderPage({ ...mockUser, dob: undefined });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /Edit/ }));
    expect(document.querySelector('input[name="dob"]').value).toBe('');
  });

  // PROF-27: "View booking" link has correct href
  test('View booking link has correct href', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [{ bookingId: 5, flightCode: 'X', fromAirport: 'A', toAirport: 'B', status: 'CONFIRMED', paymentStatus: 'SUCCESS', seats: [] }]
    });
    renderPage(mockUser);
    const link = await screen.findByRole('link', { name: /View booking/ });
    expect(link.getAttribute('href')).toBe('/bookings/5');
  });

  // PROF-28: seats joined with comma
  test('shows seats joined with comma in table', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [{ bookingId: 1, flightCode: 'X', fromAirport: 'A', toAirport: 'B', status: 'CONFIRMED', paymentStatus: 'SUCCESS', seats: ['1A', '2B'] }]
    });
    renderPage(mockUser);
    expect(await screen.findByText('1A, 2B')).toBeInTheDocument();
  });

  // PROF-30: refreshMe called after successful save
  test('calls refreshMe after successful save', async () => {
    fetch
      .mockResolvedValueOnce({ ok: true, json: async () => [] })
      .mockResolvedValueOnce({ ok: true, json: async () => mockUser });
    mockRefreshMe.mockResolvedValueOnce(mockUser);
    renderPage(mockUser);
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    fireEvent.click(screen.getByRole('button', { name: /Edit/ }));
    fireEvent.click(screen.getByRole('button', { name: /^Save$/ }));
    await waitFor(() => expect(mockRefreshMe).toHaveBeenCalledTimes(1));
  });
});
