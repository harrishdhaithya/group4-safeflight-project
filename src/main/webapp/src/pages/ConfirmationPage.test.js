import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ConfirmationPage from './ConfirmationPage';

const mockBooking = {
  bookingId: 42,
  status: 'CONFIRMED',
  paymentStatus: 'SUCCESS',
  flightCode: 'SF101',
  airlineName: 'Safe Air',
  fromAirport: 'SYD',
  toAirport: 'MEL',
  travelDate: '2026-06-01',
  travelTime: '08:30:00',
  seats: ['1A', '2B'],
  passengers: [
    { fname: 'John', lname: 'Doe', seatNo: '1A', dob: '1990-01-01', email: 'j@test.com', phone: '123', passport: 'P1' }
  ]
};

const renderWithState = (state) =>
  render(
    <MemoryRouter initialEntries={[{ pathname: '/confirmation', state }]}>
      <ConfirmationPage />
    </MemoryRouter>
  );

describe('ConfirmationPage', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
    global.URL.createObjectURL = jest.fn(() => 'blob:mock');
    global.URL.revokeObjectURL = jest.fn();
  });

  afterEach(() => jest.clearAllMocks());

  // CONF-01: no booking in state → warning
  test('shows warning when no booking in state', () => {
    renderWithState(null);
    expect(screen.getByText(/No booking information/)).toBeInTheDocument();
    expect(document.querySelector('a[href="/"]')).toBeInTheDocument();
    expect(document.querySelector('a[href="/profile"]')).toBeInTheDocument();
  });

  // CONF-02/03: booking present → heading and booking ID
  test('shows booking confirmed heading and booking ID', () => {
    renderWithState({ booking: mockBooking });
    expect(screen.getByText('Booking confirmed')).toBeInTheDocument();
    expect(screen.getAllByText(/Booking #42/).length).toBeGreaterThan(0);
  });

  // CONF-04: paymentStatus SUCCESS → "Paid" badge
  test('shows Paid badge when paymentStatus is SUCCESS', () => {
    renderWithState({ booking: mockBooking });
    expect(screen.getByText(/Payment: Paid/)).toBeInTheDocument();
  });

  // CONF-05: paymentStatus absent → no payment badge
  test('does not show payment badge when paymentStatus absent', () => {
    renderWithState({ booking: { ...mockBooking, paymentStatus: undefined } });
    expect(screen.queryByText(/Payment:/)).not.toBeInTheDocument();
  });

  // CONF-06: paymentStatus non-SUCCESS → raw value
  test('shows raw paymentStatus when not SUCCESS', () => {
    renderWithState({ booking: { ...mockBooking, paymentStatus: 'FAILED' } });
    expect(screen.getByText(/Payment: FAILED/)).toBeInTheDocument();
  });

  // CONF-07: airlineName present
  test('shows airline name when present', () => {
    renderWithState({ booking: mockBooking });
    expect(screen.getByText('Safe Air')).toBeInTheDocument();
  });

  // CONF-08: airlineName absent → not rendered
  test('does not show airline name when absent', () => {
    renderWithState({ booking: { ...mockBooking, airlineName: undefined } });
    expect(screen.queryByText('Safe Air')).not.toBeInTheDocument();
  });

  // CONF-09: fromAirport present
  test('shows fromAirport', () => {
    renderWithState({ booking: mockBooking });
    expect(screen.getByText('SYD')).toBeInTheDocument();
  });

  // CONF-10: seats joined with comma
  test('shows seats joined with comma', () => {
    renderWithState({ booking: mockBooking });
    expect(screen.getByText('1A, 2B')).toBeInTheDocument();
  });

  // CONF-11: seats empty → "—"
  test('shows dash when seats empty', () => {
    renderWithState({ booking: { ...mockBooking, seats: [] } });
    expect(screen.getByText('—')).toBeInTheDocument();
  });

  // CONF-12: totalAmount > 0 → "Total paid" shown
  test('shows total paid when totalAmount > 0', () => {
    renderWithState({ booking: mockBooking, totalAmount: 500 });
    expect(screen.getByText(/Total paid/)).toBeInTheDocument();
    expect(screen.getByText(/500/)).toBeInTheDocument();
  });

  // CONF-13: totalAmount null → no "Total paid"
  test('does not show total paid when totalAmount is null', () => {
    renderWithState({ booking: mockBooking, totalAmount: null });
    expect(screen.queryByText(/Total paid/)).not.toBeInTheDocument();
  });

  // CONF-14: totalAmount = 0 → no "Total paid"
  test('does not show total paid when totalAmount is 0', () => {
    renderWithState({ booking: mockBooking, totalAmount: 0 });
    expect(screen.queryByText(/Total paid/)).not.toBeInTheDocument();
  });

  // CONF-15: passengers present → list rendered
  test('shows passenger list when passengers present', () => {
    renderWithState({ booking: mockBooking });
    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText(/Seat 1A/)).toBeInTheDocument();
  });

  // CONF-16: passengers empty → no list
  test('does not show passenger list when empty', () => {
    renderWithState({ booking: { ...mockBooking, passengers: [] } });
    expect(screen.queryByText('John Doe')).not.toBeInTheDocument();
  });

  // CONF-17: passenger.seatNo absent → no "Seat X"
  test('does not show seat label when passenger seatNo absent', () => {
    const booking = { ...mockBooking, passengers: [{ fname: 'John', lname: 'Doe' }] };
    renderWithState({ booking });
    expect(screen.queryByText(/Seat 1A/)).not.toBeInTheDocument();
  });

  // CONF-18/19: passenger.dob present → DOB shown; absent → not shown
  test('shows DOB when passenger dob present', () => {
    renderWithState({ booking: mockBooking });
    expect(screen.getByText(/DOB:/)).toBeInTheDocument();
  });

  test('does not show DOB when passenger dob absent', () => {
    const booking = { ...mockBooking, passengers: [{ fname: 'John', lname: 'Doe', seatNo: '1A' }] };
    renderWithState({ booking });
    expect(screen.queryByText(/DOB:/)).not.toBeInTheDocument();
  });

  // CONF-20/21/22: email, phone, passport shown
  test('shows passenger email, phone and passport', () => {
    renderWithState({ booking: mockBooking });
    expect(screen.getByText(/j@test.com/)).toBeInTheDocument();
    expect(screen.getByText(/123/)).toBeInTheDocument();
    expect(screen.getByText(/Passport: P1/)).toBeInTheDocument();
  });

  // CONF-23/24: formatDate valid and null
  test('formats travel date correctly', () => {
    renderWithState({ booking: mockBooking });
    expect(screen.getByText(/Jun/)).toBeInTheDocument();
  });

  test('shows dash for null travel date', () => {
    renderWithState({ booking: { ...mockBooking, travelDate: null } });
    const dashes = screen.getAllByText('—');
    expect(dashes.length).toBeGreaterThan(0);
  });

  // CONF-25/26: formatTime valid and null
  test('formats departure time correctly', () => {
    renderWithState({ booking: mockBooking });
    expect(screen.getByText(/8:30 AM/)).toBeInTheDocument();
  });

  test('shows dash for null travel time', () => {
    renderWithState({ booking: { ...mockBooking, travelTime: null } });
    const dashes = screen.getAllByText('—');
    expect(dashes.length).toBeGreaterThan(0);
  });

  // CONF-27: PM time
  test('formats afternoon time as PM', () => {
    renderWithState({ booking: { ...mockBooking, travelTime: '14:00:00' } });
    expect(screen.getByText(/2:00 PM/)).toBeInTheDocument();
  });

  // CONF-28: midnight → 12 AM
  test('formats midnight as 12 AM', () => {
    renderWithState({ booking: { ...mockBooking, travelTime: '00:30:00' } });
    expect(screen.getByText(/12:30 AM/)).toBeInTheDocument();
  });

  // CONF-29: download button present and triggers fetch
  test('download ticket button triggers fetch', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      headers: { get: () => 'attachment; filename="ticket-booking-42.pdf"' },
      blob: async () => new Blob(['pdf'])
    });
    renderWithState({ booking: mockBooking });
    fireEvent.click(screen.getByRole('button', { name: /Download ticket/ }));
    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith('/api/bookings/42/ticket/pdf', { credentials: 'include' })
    );
  });

  // CONF-30: fetch not ok → no download
  test('does not trigger download when fetch response not ok', async () => {
    fetch.mockResolvedValueOnce({ ok: false });
    renderWithState({ booking: mockBooking });
    fireEvent.click(screen.getByRole('button', { name: /Download ticket/ }));
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    expect(URL.createObjectURL).not.toHaveBeenCalled();
  });

  // CONF-31: Content-Disposition present → filename extracted
  test('uses filename from Content-Disposition header', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      headers: { get: (h) => h === 'Content-Disposition' ? 'attachment; filename="ticket-booking-42.pdf"' : null },
      blob: async () => new Blob(['pdf'])
    });
    renderWithState({ booking: mockBooking });
    fireEvent.click(screen.getByRole('button', { name: /Download ticket/ }));
    await waitFor(() => expect(URL.createObjectURL).toHaveBeenCalled());
  });

  // CONF-32: Content-Disposition absent → fallback filename used (no crash)
  test('uses fallback filename when Content-Disposition absent', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      headers: { get: () => null },
      blob: async () => new Blob(['pdf'])
    });
    renderWithState({ booking: mockBooking });
    fireEvent.click(screen.getByRole('button', { name: /Download ticket/ }));
    await waitFor(() => expect(URL.createObjectURL).toHaveBeenCalled());
  });

  // CONF-33: navigation links
  test('shows Back to search and View my bookings links', () => {
    renderWithState({ booking: mockBooking });
    expect(screen.getByText('Back to search')).toBeInTheDocument();
    expect(screen.getByText('View my bookings')).toBeInTheDocument();
  });
});
