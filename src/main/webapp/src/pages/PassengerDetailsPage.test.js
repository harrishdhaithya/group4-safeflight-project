import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import PassengerDetailsPage from './PassengerDetailsPage';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useParams: () => ({ scheduleId: '10' })
}));

const mockSeatMapData = {
  flightCode: 'SF101',
  fromAirport: 'SYD',
  toAirport: 'MEL'
};

const renderWithState = (state) =>
  render(
    <MemoryRouter initialEntries={[{ pathname: '/passengers/10', state }]}>
      <PassengerDetailsPage />
    </MemoryRouter>
  );

const fillPassenger = () => {
  fireEvent.change(document.querySelector('input[type="text"]'), { target: { value: 'John' } });
  const textInputs = document.querySelectorAll('input[type="text"]');
  fireEvent.change(textInputs[1], { target: { value: 'Doe' } });
  fireEvent.change(document.querySelector('input[type="date"]'), { target: { value: '1990-01-01' } });
  fireEvent.change(document.querySelector('input[type="tel"]'), { target: { value: '123456' } });
  fireEvent.change(document.querySelector('input[type="email"]'), { target: { value: 'j@test.com' } });
  fireEvent.change(textInputs[2], { target: { value: 'P1234' } });
};

describe('PassengerDetailsPage', () => {
  beforeEach(() => {
    globalThis.fetch = jest.fn();
    mockNavigate.mockClear();
  });

  afterEach(() => jest.clearAllMocks());

  // PASS-01: no seats → warning
  test('shows warning when no seats selected', () => {
    renderWithState({ selectedSeats: [] });
    expect(screen.getByText(/No seats selected/)).toBeInTheDocument();
    expect(document.querySelector('a[href="/"]')).toBeInTheDocument();
  });

  // PASS-01 variant: no state at all
  test('shows warning when location state is null', () => {
    renderWithState(null);
    expect(screen.getByText(/No seats selected/)).toBeInTheDocument();
  });

  // PASS-02/03: seats present → fetch called; loading shown
  test('shows loading state while fetching', () => {
    fetch.mockImplementationOnce(() => new Promise(() => {}));
    renderWithState({ selectedSeats: ['1A'] });
    expect(screen.getByText('Loading flight details...')).toBeInTheDocument();
  });

  test('fetches seatmap on mount when seats present', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => mockSeatMapData });
    renderWithState({ selectedSeats: ['1A'] });
    await waitFor(() => expect(fetch).toHaveBeenCalledWith('/api/flights/10/seatmap'));
  });

  // PASS-04: fetch throws → error
  test('shows error when fetch throws', async () => {
    fetch.mockRejectedValueOnce(new Error('Network error'));
    renderWithState({ selectedSeats: ['1A'] });
    expect(await screen.findByText(/Network error/)).toBeInTheDocument();
  });

  // PASS-05: fetch non-ok → error
  test('shows error when fetch returns non-ok', async () => {
    fetch.mockResolvedValueOnce({ ok: false });
    renderWithState({ selectedSeats: ['1A'] });
    expect(await screen.findByText(/Failed to load flight details/)).toBeInTheDocument();
  });

  // PASS-06: form rendered for each seat
  test('renders passenger form for each selected seat', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => mockSeatMapData });
    renderWithState({ selectedSeats: ['1A', '2B'] });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    expect(await screen.findByText('1A')).toBeInTheDocument();
    expect(screen.getByText('2B')).toBeInTheDocument();
  });

  // PASS-07: flight info shown in subtitle
  test('shows flight info subtitle when data loaded', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => mockSeatMapData });
    renderWithState({ selectedSeats: ['1A'] });
    expect(await screen.findByText(/SF101/)).toBeInTheDocument();
    expect(screen.getByText(/SYD → MEL/)).toBeInTheDocument();
  });

  // PASS-09: proceed button disabled when form empty
  test('proceed button is disabled when form is empty', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => mockSeatMapData });
    renderWithState({ selectedSeats: ['1A'] });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    const btn = await screen.findByRole('button', { name: /Proceed to payment/ });
    expect(btn).toBeDisabled();
  });

  // PASS-10: proceed button enabled when all fields filled
  test('proceed button enabled when all required fields filled', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => mockSeatMapData });
    renderWithState({ selectedSeats: ['1A'] });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    await screen.findByRole('button', { name: /Proceed to payment/ });
    fillPassenger();
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Proceed to payment/ })).not.toBeDisabled()
    );
  });

  // PASS-11: fname blank → button disabled
  test('proceed button stays disabled when fname is blank', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => mockSeatMapData });
    renderWithState({ selectedSeats: ['1A'] });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    await screen.findByRole('button', { name: /Proceed to payment/ });
    fillPassenger();
    fireEvent.change(document.querySelector('input[type="text"]'), { target: { value: '   ' } });
    expect(screen.getByRole('button', { name: /Proceed to payment/ })).toBeDisabled();
  });

  // PASS-17: typing in fname updates input
  test('typing in fname field updates value', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => mockSeatMapData });
    renderWithState({ selectedSeats: ['1A'] });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    await screen.findByRole('button', { name: /Proceed to payment/ });
    fireEvent.change(document.querySelector('input[type="text"]'), { target: { value: 'Alice' } });
    expect(document.querySelector('input[type="text"]').value).toBe('Alice');
  });

  // PASS-18: valid baggage number
  test('typing valid number in baggage field updates value', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => mockSeatMapData });
    renderWithState({ selectedSeats: ['1A'] });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    await screen.findByRole('button', { name: /Proceed to payment/ });
    const baggageInput = document.querySelector('input[type="number"]');
    fireEvent.change(baggageInput, { target: { value: '2' } });
    expect(baggageInput.value).toBe('2');
  });

  // PASS-19: empty string in baggage → stays empty
  test('baggage input allows empty string', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => mockSeatMapData });
    renderWithState({ selectedSeats: ['1A'] });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    await screen.findByRole('button', { name: /Proceed to payment/ });
    const baggageInput = document.querySelector('input[type="number"]');
    fireEvent.change(baggageInput, { target: { value: '' } });
    expect(baggageInput.value).toBe('');
  });

  // PASS-20: negative baggage → clamped to "0"
  test('negative baggage value is clamped to 0', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => mockSeatMapData });
    renderWithState({ selectedSeats: ['1A'] });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    await screen.findByRole('button', { name: /Proceed to payment/ });
    const baggageInput = document.querySelector('input[type="number"]');
    fireEvent.change(baggageInput, { target: { value: '-1' } });
    expect(baggageInput.value).toBe('0');
  });

  // PASS-23: clicking Proceed navigates to /payment
  test('clicking Proceed navigates to /payment with correct state', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => mockSeatMapData });
    renderWithState({ selectedSeats: ['1A'], seatPricingSummary: { totalAmount: 300 } });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    await screen.findByRole('button', { name: /Proceed to payment/ });
    fillPassenger();
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /Proceed to payment/ })).not.toBeDisabled()
    );
    fireEvent.click(screen.getByRole('button', { name: /Proceed to payment/ }));
    expect(mockNavigate).toHaveBeenCalledWith('/payment', expect.objectContaining({
      state: expect.objectContaining({
        bookingPayload: expect.objectContaining({ scheduleId: 10 })
      })
    }));
  });

  // PASS-27: back to seat map link
  test('shows back to seat map link with correct href', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => mockSeatMapData });
    renderWithState({ selectedSeats: ['1A'] });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    const link = await screen.findByRole('link', { name: /Back to seat map/ });
    expect(link.getAttribute('href')).toBe('/flights/10');
  });

  // PASS-28: passengersBySeat pre-populated from state
  test('pre-populates passenger fields from location state', async () => {
    fetch.mockResolvedValueOnce({ ok: true, json: async () => mockSeatMapData });
    renderWithState({
      selectedSeats: ['1A'],
      passengersBySeat: { '1A': { fname: 'Pre', lname: 'Filled', dob: '1985-05-05', phone: '999', email: 'p@test.com', passport: 'PP1', baggageQuantity: '1' } }
    });
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    await screen.findByRole('button', { name: /Proceed to payment/ });
    expect(document.querySelector('input[type="text"]').value).toBe('Pre');
  });
});
