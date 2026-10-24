import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// El backend (bitácora §14) permite CORS desde http://localhost:3000.
// strictPort evita que Vite salte silenciosamente a otro puerto (p. ej. 5173)
// y rompa CORS sin avisar.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    strictPort: true,
  },
})
