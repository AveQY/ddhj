import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src')
      }
    },
    server: {
      port: 42836,
      proxy: {
        [env.VITE_API_BASE_URL]: {
          target: 'http://localhost:42835',
          changeOrigin: true
        },
        [env.VITE_IMAGE_BASE_URL]: {
          target: 'http://localhost:42835/api',
          changeOrigin: true
        }
      }
    }
  }
})
