import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import router from '../router'

// 统一的 axios 实例 —— 整个前端所有请求都从这里发。
//
// 人话讲解（把它想成"前台收发室"）：
// - 前端调后端有几十处，如果每处都手写 URL、手写 token、手写错误处理，代码会重复且难改。
//   所以我们建一个"统一收发室"：定义好 baseURL、超时，并挂两个"拦截器"。
//
// - 请求拦截器：每个请求发出去之前，自动把 token 塞进请求头 Authorization。
//   这样业务代码里调 api.get('/xxx') 完全不用关心 token，非常干净。
//   这就是为什么登录后要往 Pinia 里存 token —— 存了才能被这里自动带上。
//
// - 响应拦截器：
//   ① 成功：直接把 response.data 返回（业务代码不用每次写 res.data.data 那种套娃）。
//   ② 失败：统一弹提示。特别是 401（token 过期/没登录）时，清掉本地登录态并跳回登录页。
//
// 面试必懂点：
// - 为什么 baseURL 是 '/api'？因为我们用 Vite 的 proxy 把 /api 转发到后端 8080，
//   开发时避开跨域。生产环境则改成 Nginx 同域反向代理，前端代码一行都不用改。
// - HTTP 状态码与业务错误码的区别：401/403 是 HTTP 语义层的"未认证/无权限"，
//   而"用户名已存在"这种是业务层的，后端用 { "error": "..." } 返回，HTTP 可能还是 400。
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器：自动带上 token
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一解包 + 统一报错
request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status
    // 后端返回的友好错误信息（GlobalExceptionHandler 统一格式：{ "error": "..." }）
    const message = error.response?.data?.error || '请求失败，请稍后重试'

    if (status === 401) {
      // token 失效：清空本地登录态，回到登录页
      const userStore = useUserStore()
      userStore.clear()
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default request
