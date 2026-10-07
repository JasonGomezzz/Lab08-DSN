import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

const backend = 'http://localhost:8080'

// En desarrollo la interfaz (5173) reenvía la API y el baile de OAuth2 al backend (8080),
// así el navegador siempre habla con un único origen y no hace falta CORS.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': { target: backend },
      '/oauth2': { target: backend },
      '/login/oauth2': { target: backend },
    },
  },
})
