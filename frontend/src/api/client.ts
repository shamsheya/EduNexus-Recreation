import axios, { AxiosError } from 'axios';

// Single HTTP client (D7). The reference had two clients on two ports plus raw fetch() calls.
export const API_URL: string = import.meta.env.VITE_API_URL || 'http://localhost:8083/api';

export const TOKEN_KEY = 'token';

const client = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// The store registers its logout action here (avoids the reference's circular import
// utils/axios.ts -> store -> authSlice -> utils/axios.ts).
let onUnauthorized: (() => void) | null = null;
export const setUnauthorizedHandler = (handler: () => void) => {
  onUnauthorized = handler;
};

client.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    // Same behaviour as reference utils/axios.ts:32-35: a 401 logs out and returns to /login.
    if (error.response?.status === 401) {
      onUnauthorized?.();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

/** Extracts the backend's `{ message }` error text, with a fallback. */
export const errorMessage = (error: unknown, fallback: string): string => {
  const data = (error as AxiosError<{ message?: string }>)?.response?.data;
  return data?.message || fallback;
};

export default client;
