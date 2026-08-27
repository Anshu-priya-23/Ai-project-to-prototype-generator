import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import http from 'node:http';

const proxyAgent = new http.Agent({ keepAlive: true });

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        agent: proxyAgent
      }
    }
  }
});
