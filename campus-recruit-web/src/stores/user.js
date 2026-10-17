import { defineStore } from 'pinia'
import { ref } from 'vue'

// 用户状态仓库（Pinia）。
//
// 人话讲解：
// - 登录后的 token 和用户名，很多页面都要用（首页显示"欢迎 xxx"、请求要带 token）。
//   如果靠组件之间互相传，会很乱。Pinia 就是一个"全局共享的抽屉"，
//   任何组件都能打开它读/写数据。
// - 用 localStorage 做持久化：刷新浏览器后 token 还在，不会一刷新就退出登录。
//   （localStorage 存在浏览器本地，关掉页面也在；它是"明文存储"，安全性靠 HTTPS 和短过期时间保障。）
//
// 面试必懂点：
// - 为什么 token 存 localStorage 而不是 Cookie？
//   Cookie 会自动随请求发送，容易受 CSRF 攻击；localStorage 不会自动发，需要代码手动放进
//   请求头（我们就是在 axios 请求拦截器里做的），所以配 JWT 更常见。
//   代价是 localStorage 能被页面上任何 JS 读到，所以必须防 XSS（不轻信用户输入、避免 innerHTML）。
const TOKEN_KEY = 'campus_token'
const USERNAME_KEY = 'campus_username'

export const useUserStore = defineStore('user', () => {
  // 初始化时从 localStorage 读，实现"刷新不掉登录"
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const username = ref(localStorage.getItem(USERNAME_KEY) || '')

  /** 保存登录态 */
  function setLogin(newToken, newUsername) {
    token.value = newToken
    username.value = newUsername
    localStorage.setItem(TOKEN_KEY, newToken)
    localStorage.setItem(USERNAME_KEY, newUsername)
  }

  /** 清空登录态（退出登录 / token 失效时调用） */
  function clear() {
    token.value = ''
    username.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USERNAME_KEY)
  }

  return { token, username, setLogin, clear }
})
