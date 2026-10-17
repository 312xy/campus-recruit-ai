import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

// 路由表 + 路由守卫。
//
// 人话讲解：
// - 路由就是"URL 和页面的对应关系"：访问 /login 就显示 Login.vue。
// - meta.requiresAuth 是给这个页面打的"标记"：写着 true 表示"必须登录才能进"。
// - router.beforeEach 是"路由守卫"，在每次跳转前先运行一段判断：
//   要去受保护页面但没 token → 强制踢回 /login。
//
// 面试必懂点：
// - 前端路由守卫只是"用户体验"层面的拦截，防不住坏人。
//   真正安全靠后端 —— 就算前端代码被改，请求受保护接口时后端依然会验 token 返回 401。
//   所以：前端守卫管体验，后端鉴权管安全，两者都要有。这一点面试官很爱追问。
const routes = [
  { path: '/', redirect: '/home' },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录 / 注册' }
  },
  {
    path: '/home',
    name: 'home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true, title: '首页' }
  },
  {
    // 简历解析页。组件用 () => import(...) 的"懒加载"写法：
    // 只有真的访问这个页面时才去下载对应的 JS 文件，
    // 首屏体积更小、加载更快。这是单页应用(SPA)最常用的优化手段之一。
    path: '/resume',
    name: 'resume',
    component: () => import('../views/Resume.vue'),
    meta: { requiresAuth: true, title: '简历解析' }
  },
  {
    path: '/job',
    name: 'job',
    component: () => import('../views/Job.vue'),
    meta: { requiresAuth: true, title: '岗位管理' }
  },
  {
    path: '/match',
    name: 'match',
    component: () => import('../views/Match.vue'),
    meta: { requiresAuth: true, title: 'AI 匹配评分' }
  },
  {
    path: '/interview',
    name: 'interview',
    component: () => import('../views/Interview.vue'),
    meta: { requiresAuth: true, title: 'AI 模拟面试' }
  },
  {
    path: '/membership',
    name: 'membership',
    component: () => import('../views/Membership.vue'),
    meta: { requiresAuth: true, title: '会员中心' }
  },
  {
    // 兼容手滑写成复数的情况：接口路径是 /api/jobs（复数），页面路径是 /job（单数），
    // 用户很容易按接口习惯输成 /jobs —— 与其白屏，不如自动送到正确的页面。
    path: '/jobs',
    redirect: '/job'
  },
  {
    // 兜底路由：任何没匹配上的路径（比如手输错 URL、收藏了已删除的页面）
    // 统一送回首页。没有它，Vue Router 匹配不到任何路由时会渲染一个空白页，
    // 用户会以为"网站坏了"——这是 SPA 很经典的白屏坑。
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.token) {
    // 没登录却想进受保护页面 → 送回登录页
    return '/login'
  }
  // 已登录还想去登录页 → 直接进首页（避免重复登录）
  if (to.path === '/login' && userStore.token) {
    return '/home'
  }
  return true
})

export default router
