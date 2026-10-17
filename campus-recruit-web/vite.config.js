import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 配置
// 人话讲解：
// - plugins: [vue()] 让 Vite 能编译 .vue 单文件组件（把 <template>/<script>/<style> 合成一个组件）。
// - server.port: 前端开发服务器跑在 5173。
// - server.proxy: 关键配置。开发时前端调 /api/xxx，Vite 帮我们转发到后端 http://localhost:8080，
//   这样浏览器眼里前后端"同源"，就绕开了跨域(CORS)问题。
//   （后端自己也配了 CORS，双保险；但走代理是最省心的开发方式。
//     注意：生产打包后没有 Vite 代理了，那时就要靠 Nginx 反向代理或后端 CORS。）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
