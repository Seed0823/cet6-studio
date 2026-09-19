import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    // 拆包后主包已远低于默认 500KB；这里把告警线放到 700KB，
    // 避免单个「稳定大依赖」chunk 反复刷警告噪音。
    chunkSizeWarningLimit: 700,
    rollupOptions: {
      output: {
        /**
         * 手动分包：把体积最大的第三方库拆成独立 chunk。
         *
         * 收益有三点：
         * 1. echarts 仅「学习统计」页使用 —— 拆出后首屏（登录/今日学习）不再加载它；
         * 2. element-plus / vue 等依赖升级频率低，单独成包利于浏览器长缓存命中；
         * 3. 避免全部依赖被压进 index-*.js 造成单包过大、首屏阻塞。
         */
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          // zrender 是 echarts 的底层渲染库，必须与 echarts 同包，否则会拆散
          if (id.includes('echarts') || id.includes('zrender')) return 'vendor-echarts'
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'vendor-element-plus'
          return 'vendor'
        }
      }
    }
  },
  server: {
    port: 5173,
    // 开发环境代理：前端请求 /api 自动转发到后端 8080，绕开跨域
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
