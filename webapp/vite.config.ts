import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import basicSsl from '@vitejs/plugin-basic-ssl';

const springHttpsTarget = 'https://localhost:8080';

export default defineConfig({
  base: '/app/',
  plugins: [react(), basicSsl()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: springHttpsTarget,
        changeOrigin: true,
        secure: false,
      },
      '/login': {
        target: springHttpsTarget,
        changeOrigin: false,
        secure: false,
      },
      '/logout': {
        target: springHttpsTarget,
        changeOrigin: false,
        secure: false,
      },
      '/signup': {
        target: springHttpsTarget,
        changeOrigin: false,
        secure: false,
      },
    },
  },
  build: {
    outDir: '../src/main/resources/static/app',
    emptyOutDir: true,
  },
});
