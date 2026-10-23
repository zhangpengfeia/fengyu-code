import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
export default defineConfig({ base: process.env.GAME_BASE_PATH || '/', plugins: [vue()] })
