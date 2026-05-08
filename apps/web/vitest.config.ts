import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'node:path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    exclude: ['node_modules', 'dist', 'e2e/**', 'playwright-report'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/main.tsx',
        'src/App.tsx',
        'src/store/index.ts',
        'src/features/events/useSseStream.ts',
        'src/**/*.d.ts',
        'src/tests/**',
        'src/**/*.test.{ts,tsx}',
      ],
      thresholds: {
        lines: 75,
        functions: 70,
        statements: 75,
        branches: 65,
      },
    },
  },
});
