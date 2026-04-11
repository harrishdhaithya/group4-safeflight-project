import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import App from './App';

beforeEach(() => {
  global.fetch = jest.fn().mockResolvedValue({
    ok: false,
    json: async () => ({})
  });
});

afterEach(() => {
  jest.clearAllMocks();
});

test('renders auth page when user is not logged in', async () => {
  render(
    <MemoryRouter
      initialEntries={['/']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <App />
    </MemoryRouter>
  );

  expect(await screen.findByRole('heading', { name: 'Login' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /^Signup$/ })).toBeInTheDocument();
});

// APP-02: user present → SearchPage rendered
test('renders search page when user is logged in', async () => {
  const user = { id: 1, email: 'a@test.com', role: 'USER' };
  // First call: refreshMe on mount returns user; second call: destinations fetch
  fetch
    .mockResolvedValueOnce({ ok: true, json: async () => user })
    .mockResolvedValueOnce({ ok: true, json: async () => [] });

  render(
    <MemoryRouter
      initialEntries={['/']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <App />
    </MemoryRouter>
  );

  expect(await screen.findByText('Search one-way flights')).toBeInTheDocument();
});
