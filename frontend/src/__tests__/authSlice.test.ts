import reducer, { getCurrentUser, login, logout, updateProfile, AuthState } from '../store/slices/authSlice';
import { enrollInCourse } from '../store/slices/courseSlice';
import { User } from '../types';

const user: User = {
  id: 'u1',
  email: 'a@b.dev',
  firstName: 'Ann',
  lastName: 'Lee',
  roles: ['student'],
  enrolledCourses: [],
  managedCourses: [],
};

const base: AuthState = { user: null, token: null, loading: false, error: null };

describe('authSlice', () => {
  beforeEach(() => localStorage.clear());

  it('stores user and token on login success', () => {
    const state = reducer(base, { type: login.fulfilled.type, payload: { token: 't', user } });
    expect(state.user).toEqual(user);
    expect(state.token).toBe('t');
    expect(state.loading).toBe(false);
  });

  it('keeps the backend message on login failure', () => {
    const state = reducer(base, { type: login.rejected.type, payload: 'Invalid email or password' });
    expect(state.error).toBe('Invalid email or password');
  });

  it('restores the user from /auth/me (session restore)', () => {
    const state = reducer({ ...base, token: 't', loading: true }, { type: getCurrentUser.fulfilled.type, payload: user });
    expect(state.user).toEqual(user);
    expect(state.loading).toBe(false);
  });

  it('signs out when session restore fails', () => {
    localStorage.setItem('token', 't');
    const state = reducer({ ...base, token: 't', loading: true }, { type: getCurrentUser.rejected.type });
    expect(state.token).toBeNull();
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('logout clears state and storage', () => {
    localStorage.setItem('token', 't');
    const state = reducer({ ...base, user, token: 't' }, logout());
    expect(state.user).toBeNull();
    expect(state.token).toBeNull();
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('updateProfile replaces the user', () => {
    const state = reducer({ ...base, user }, updateProfile({ ...user, firstName: 'Zed' }));
    expect(state.user?.firstName).toBe('Zed');
  });

  it('adds the course id to enrolledCourses after enrolment', () => {
    const state = reducer({ ...base, user }, { type: enrollInCourse.fulfilled.type, payload: { id: 'c1' } });
    expect(state.user?.enrolledCourses).toEqual(['c1']);
  });
});
