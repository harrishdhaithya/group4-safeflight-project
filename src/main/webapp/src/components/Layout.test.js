import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Layout from './Layout';

const mockNavigate = jest.fn();
const mockLogout = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate
}));

jest.mock('../context/AuthContext', () => ({
  useAuth: jest.fn()
}));

const { useAuth } = require('../context/AuthContext');

const renderWithUser = (user) => {
  useAuth.mockReturnValue({ user, logout: mockLogout });
  return render(
    <MemoryRouter>
      <Layout><div>child content</div></Layout>
    </MemoryRouter>
  );
};

describe('Layout', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mockLogout.mockClear();
  });

  afterEach(() => jest.clearAllMocks());

  // LAY-04: brand link always present
  test('renders Safeflight brand link', () => {
    renderWithUser(null);
    expect(screen.getByText('Safeflight')).toBeInTheDocument();
  });

  // LAY-01: user null → no email link, no logout button
  test('does not show email link or logout when user is null', () => {
    renderWithUser(null);
    expect(screen.queryByText(/Logout/)).not.toBeInTheDocument();
  });

  // LAY-02: user present → email link and logout button shown
  test('shows email link and logout button when user is present', () => {
    renderWithUser({ email: 'test@test.com' });
    expect(screen.getByText('test@test.com')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Logout/ })).toBeInTheDocument();
  });

  // LAY-03: logout button click → logout() called and navigate('/')
  test('clicking logout calls logout and navigates to /', async () => {
    mockLogout.mockResolvedValueOnce();
    renderWithUser({ email: 'test@test.com' });
    fireEvent.click(screen.getByRole('button', { name: /Logout/ }));
    await waitFor(() => expect(mockLogout).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/'));
  });

  // children rendered
  test('renders children inside main', () => {
    renderWithUser(null);
    expect(screen.getByText('child content')).toBeInTheDocument();
  });
});
