/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Port 3000 matches the reference CRA dev server and the backend CORS origin.
export default defineConfig({
  plugins: [react()],
  server: { port: 3000, strictPort: true },
  preview: { port: 3000, strictPort: true },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.ts'],
    globals: true,
  },
});
