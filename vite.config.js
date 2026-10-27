import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const port = Number.parseInt(env.PORT, 10) || 5173;
  const host = env.HOST || '0.0.0.0';

  return {
    plugins: [react()],
    server: { host, port, strictPort: true },
    preview: { host, port, strictPort: true }
  };
});
