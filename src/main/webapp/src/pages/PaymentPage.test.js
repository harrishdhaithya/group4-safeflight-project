import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import PaymentPage from './PaymentPage';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate
}));

const validPayload = {
  scheduleId: 10,
  seats: [{ seatNo: '1A', fname: 'John', lname: 'Doe' }]
};

const validFlightSummary = { flightCode: 'SF101', fromAirport: 'SYD', toAirport: 'MEL' };

const renderWithState = (state) =>
  render(
    <MemoryRouter initialEntries={[{ pathname: '/payment', state }]}>
      <PaymentPage />
    </MemoryRouter>
  );

const fillValidCard = () => {
  fireEvent.change(document.querySelector('input[name="number"]'), { target: { value: '1234567890123456' } });
  fireEvent.change(document.querySelector('input[name="expiry"]'), { target: { value: '12/25' } });
  fireEvent.change(document.querySelector('input[name="cvv"]'), { target: { value: '123' } });
};

const fillValidBilling = () => {
  fireEvent.change(document.querySelector('input[name="addressLine1"]'), { target: { value: '123 Main St' } });
  fireEvent.change(document.querySelector('input[name="city"]'), { target: { value: 'Sydney' } });
  fireEvent.change(document.querySelector('input[name="postalCode"]'), { target: { value: '2000' } });
  fireEvent.change(document.querySelector('input[name="country"]'), { target: { value: 'AU' } });
};

describe('PaymentPage', () => {
  beforeEach(() => {
    globalThis.fetch = jest.fn();
    mockNavigate.mockClear();
  });

  afterEach(() => jest.clearAllMocks());

  // PAY-01: no bookingPayload → warning
  test('shows warning when no booking payload in state', () => {
    renderWithState(null);
    expect(screen.getByText(/No booking information/)).toBeInTheDocument();
  });

  // PAY-02: empty seats → warning
  test('shows warning when seats array is empty', () => {
    renderWithState({ bookingPayload: { scheduleId: 1, seats: [] } });
    expect(screen.getByText(/No booking information/)).toBeInTheDocument();
  });

  // PAY-03: valid state → Payment heading and form
  test('renders payment form with valid state', () => {
    renderWithState({ bookingPayload: validPayload });
    expect(screen.getByText('Payment')).toBeInTheDocument();
    expect(document.querySelector('input[name="number"]')).toBeInTheDocument();
    expect(document.querySelector('input[name="expiry"]')).toBeInTheDocument();
    expect(document.querySelector('input[name="cvv"]')).toBeInTheDocument();
  });

  // PAY-04: flightSummary present → order summary shown
  test('shows order summary when flightSummary present', () => {
    renderWithState({ bookingPayload: validPayload, flightSummary: validFlightSummary });
    expect(screen.getByText(/SF101/)).toBeInTheDocument();
    expect(screen.getByText(/SYD → MEL/)).toBeInTheDocument();
  });

  // PAY-05: flightSummary absent → no flight code
  test('does not show flight code when flightSummary absent', () => {
    renderWithState({ bookingPayload: validPayload, flightSummary: undefined });
    expect(screen.queryByText('SF101')).not.toBeInTheDocument();
  });

  // PAY-06: totalAmount > 0 → breakdown shown
  test('shows amount breakdown when totalAmount > 0', () => {
    renderWithState({ bookingPayload: validPayload, totalAmount: 500, seatTotal: 400, baggageTotal: 100, bagPrice: 10 });
    expect(screen.getByText('Seat fare')).toBeInTheDocument();
    expect(screen.getByText(/Amount due/)).toBeInTheDocument();
  });

  // PAY-07: totalAmount null → no breakdown
  test('does not show amount breakdown when totalAmount null', () => {
    renderWithState({ bookingPayload: validPayload, totalAmount: null });
    expect(screen.queryByText(/Amount due/)).not.toBeInTheDocument();
  });

  // PAY-08: seatTotal present → seat fare row
  test('shows seat fare row when seatTotal present', () => {
    renderWithState({ bookingPayload: validPayload, totalAmount: 400, seatTotal: 400 });
    expect(screen.getByText('Seat fare')).toBeInTheDocument();
  });

  // PAY-09: seatTotal null → no seat fare row
  test('does not show seat fare row when seatTotal null', () => {
    renderWithState({ bookingPayload: validPayload, totalAmount: 100, seatTotal: null, baggageTotal: 100 });
    expect(screen.queryByText('Seat fare')).not.toBeInTheDocument();
  });

  // PAY-10: baggageTotal present → baggage row
  test('shows baggage row when baggageTotal present', () => {
    renderWithState({ bookingPayload: validPayload, totalAmount: 100, baggageTotal: 100, bagPrice: 10 });
    expect(screen.getByText(/Baggage/)).toBeInTheDocument();
    expect(screen.getByText(/10 per kg/)).toBeInTheDocument();
  });

  // PAY-11: baggageTotal null → no baggage row
  test('does not show baggage row when baggageTotal null', () => {
    renderWithState({ bookingPayload: validPayload, totalAmount: 400, seatTotal: 400, baggageTotal: null });
    expect(screen.queryByText(/Baggage/)).not.toBeInTheDocument();
  });

  // PAY-12: bagPrice null → baggage label without per-kg price
  test('shows baggage label without per-kg price when bagPrice null', () => {
    renderWithState({ bookingPayload: validPayload, totalAmount: 100, baggageTotal: 100, bagPrice: null });
    expect(screen.getByText('Baggage')).toBeInTheDocument();
    expect(screen.queryByText(/per kg/)).not.toBeInTheDocument();
  });

  // PAY-13/14/15/16: typing in fields updates state
  test('typing in card fields updates inputs', () => {
    renderWithState({ bookingPayload: validPayload });
    fireEvent.change(document.querySelector('input[name="number"]'), { target: { value: '4111111111111111' } });
    expect(document.querySelector('input[name="number"]').value).toBe('4111111111111111');
    fireEvent.change(document.querySelector('input[name="expiry"]'), { target: { value: '01/28' } });
    expect(document.querySelector('input[name="expiry"]').value).toBe('01/28');
  });

  test('typing in billing fields updates inputs', () => {
    renderWithState({ bookingPayload: validPayload });
    fireEvent.change(document.querySelector('input[name="city"]'), { target: { value: 'Melbourne' } });
    expect(document.querySelector('input[name="city"]').value).toBe('Melbourne');
    fireEvent.change(document.querySelector('input[name="addressLine2"]'), { target: { value: 'Unit 2' } });
    expect(document.querySelector('input[name="addressLine2"]').value).toBe('Unit 2');
  });

  // PAY-17: card number too short → error, no fetch
  test('shows error when card number is too short', async () => {
    renderWithState({ bookingPayload: validPayload });
    fireEvent.change(document.querySelector('input[name="number"]'), { target: { value: '123' } });
    fireEvent.change(document.querySelector('input[name="expiry"]'), { target: { value: '12/25' } });
    fireEvent.change(document.querySelector('input[name="cvv"]'), { target: { value: '123' } });
    fillValidBilling();
    fireEvent.click(screen.getByRole('button', { name: /Complete transaction/ }));
    expect(await screen.findByText(/Please fill in card details/)).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
  });

  // PAY-27: expiry too short → error
  test('shows error when expiry is too short', async () => {
    renderWithState({ bookingPayload: validPayload });
    fireEvent.change(document.querySelector('input[name="number"]'), { target: { value: '1234567890123456' } });
    fireEvent.change(document.querySelector('input[name="expiry"]'), { target: { value: '12' } });
    fireEvent.change(document.querySelector('input[name="cvv"]'), { target: { value: '123' } });
    fillValidBilling();
    fireEvent.click(screen.getByRole('button', { name: /Complete transaction/ }));
    expect(await screen.findByText(/Please fill in card details/)).toBeInTheDocument();
  });

  // PAY-28: cvv too short → error
  test('shows error when CVV is too short', async () => {
    renderWithState({ bookingPayload: validPayload });
    fillValidCard();
    fireEvent.change(document.querySelector('input[name="cvv"]'), { target: { value: '12' } });
    fillValidBilling();
    fireEvent.click(screen.getByRole('button', { name: /Complete transaction/ }));
    expect(await screen.findByText(/Please fill in card details/)).toBeInTheDocument();
  });

  // PAY-18: billing city empty → error
  test('shows error when billing city is empty', async () => {
    renderWithState({ bookingPayload: validPayload });
    fillValidCard();
    fireEvent.change(document.querySelector('input[name="addressLine1"]'), { target: { value: '123 St' } });
    fireEvent.change(document.querySelector('input[name="postalCode"]'), { target: { value: '2000' } });
    fireEvent.change(document.querySelector('input[name="country"]'), { target: { value: 'AU' } });
    fireEvent.click(screen.getByRole('button', { name: /Complete transaction/ }));
    expect(await screen.findByText(/Please fill in card details/)).toBeInTheDocument();
  });

  // PAY-29: postalCode empty → error
  test('shows error when postal code is empty', async () => {
    renderWithState({ bookingPayload: validPayload });
    fillValidCard();
    fireEvent.change(document.querySelector('input[name="addressLine1"]'), { target: { value: '123 St' } });
    fireEvent.change(document.querySelector('input[name="city"]'), { target: { value: 'Sydney' } });
    fireEvent.change(document.querySelector('input[name="country"]'), { target: { value: 'AU' } });
    fireEvent.click(screen.getByRole('button', { name: /Complete transaction/ }));
    expect(await screen.findByText(/Please fill in card details/)).toBeInTheDocument();
  });

  // PAY-19/20: valid card+billing → fetch called → navigate to /confirmation
  test('calls fetch and navigates to confirmation on success', async () => {
    const bookingSummary = { bookingId: 99, status: 'CONFIRMED' };
    fetch.mockResolvedValueOnce({ ok: true, json: async () => bookingSummary });
    renderWithState({ bookingPayload: validPayload, totalAmount: 300 });
    fillValidCard();
    fillValidBilling();
    fireEvent.click(screen.getByRole('button', { name: /Complete transaction/ }));
    await waitFor(() => expect(fetch).toHaveBeenCalledWith('/api/bookings', expect.objectContaining({ method: 'POST' })));
    await waitFor(() =>
      expect(mockNavigate).toHaveBeenCalledWith('/confirmation', expect.objectContaining({
        state: expect.objectContaining({ booking: bookingSummary })
      }))
    );
  });

  // PAY-21: fetch non-ok with body → error shown
  test('shows server error message when fetch returns non-ok', async () => {
    fetch.mockResolvedValueOnce({ ok: false, text: async () => 'Seat taken' });
    renderWithState({ bookingPayload: validPayload });
    fillValidCard();
    fillValidBilling();
    fireEvent.click(screen.getByRole('button', { name: /Complete transaction/ }));
    expect(await screen.findByText('Seat taken')).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  // PAY-22: fetch non-ok empty body → fallback error
  test('shows fallback error when fetch non-ok with empty body', async () => {
    fetch.mockResolvedValueOnce({ ok: false, text: async () => '' });
    renderWithState({ bookingPayload: validPayload });
    fillValidCard();
    fillValidBilling();
    fireEvent.click(screen.getByRole('button', { name: /Complete transaction/ }));
    expect(await screen.findByText(/Booking failed/)).toBeInTheDocument();
  });

  // PAY-23: fetch throws → generic error
  test('shows generic error when fetch throws', async () => {
    fetch.mockRejectedValueOnce(new Error('Network down'));
    renderWithState({ bookingPayload: validPayload });
    fillValidCard();
    fillValidBilling();
    fireEvent.click(screen.getByRole('button', { name: /Complete transaction/ }));
    expect(await screen.findByText(/Something went wrong/)).toBeInTheDocument();
  });

  // PAY-24: button shows "Processing..." while pending
  test('shows Processing while fetch in progress', async () => {
    fetch.mockImplementationOnce(() => new Promise(() => {}));
    renderWithState({ bookingPayload: validPayload });
    fillValidCard();
    fillValidBilling();
    fireEvent.click(screen.getByRole('button', { name: /Complete transaction/ }));
    expect(await screen.findByText('Processing...')).toBeInTheDocument();
  });

  // PAY-25: back button navigates to /passengers
  test('back button navigates to passengers page', () => {
    renderWithState({ bookingPayload: validPayload });
    fireEvent.click(screen.getByRole('button', { name: /← Back/ }));
    expect(mockNavigate).toHaveBeenCalledWith('/passengers/10', expect.anything());
  });

  // PAY-26: back to home navigates to /
  test('back to home button navigates to /', () => {
    renderWithState({ bookingPayload: validPayload });
    fireEvent.click(screen.getByRole('button', { name: /Back to home/ }));
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });
});
