import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { store } from '../store';
import { AppRoutes } from '../App';
import theme from '../theme';
import { loginSchema } from '../pages/Login';
import { registerSchema } from '../pages/Register';
import { getModuleIcon } from '../pages/CourseDetail';

const renderAt = (path: string) =>
  render(
    <Provider store={store}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={[path]}>
          <AppRoutes />
        </MemoryRouter>
      </ThemeProvider>
    </Provider>
  );

describe('routing and pages', () => {
  it('redirects unauthenticated users to the login page', () => {
    renderAt('/dashboard');
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sign In' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: "Don't have an account? Sign Up" })).toBeInTheDocument();
  });

  it('shows Yup validation messages on the login form', async () => {
    renderAt('/login');
    fireEvent.click(screen.getByRole('button', { name: 'Sign In' }));
    await waitFor(() => {
      expect(screen.getByText('Email is required')).toBeInTheDocument();
      expect(screen.getByText('Password is required')).toBeInTheDocument();
    });
  });

  it('renders the registration page with the reference fields and role options', () => {
    renderAt('/register');
    expect(screen.getByRole('heading', { name: 'Create an Account' })).toBeInTheDocument();
    for (const label of ['First Name', 'Last Name', 'Email Address', 'Password']) {
      expect(screen.getByLabelText(label)).toBeInTheDocument();
    }
    fireEvent.mouseDown(screen.getByRole('combobox'));
    expect(screen.getByRole('option', { name: 'Student' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Instructor' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Admin' })).toBeInTheDocument();
  });
});

describe('validation schemas', () => {
  it('login and register share the 6-character password minimum (D2)', async () => {
    await expect(loginSchema.isValid({ email: 'a@b.dev', password: '123456' })).resolves.toBe(true);
    await expect(loginSchema.isValid({ email: 'a@b.dev', password: '12345' })).resolves.toBe(false);
    await expect(
      registerSchema.isValid({ email: 'a@b.dev', password: '123456', firstName: 'A', lastName: 'B', role: 'student' })
    ).resolves.toBe(true);
    await expect(
      registerSchema.isValid({ email: 'a@b.dev', password: '123456', firstName: 'A', lastName: 'B', role: 'owner' })
    ).resolves.toBe(false);
  });
});

describe('module icons', () => {
  it('maps lowercase content types like the reference', () => {
    expect(getModuleIcon('video').type).not.toBe(getModuleIcon('document').type);
    expect(getModuleIcon('quiz').type).not.toBe(getModuleIcon('document').type);
    expect(getModuleIcon('assignment').type).not.toBe(getModuleIcon('document').type);
  });
});
