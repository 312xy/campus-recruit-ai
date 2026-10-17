<script setup>
// 全站统一外壳：左侧导航 + 顶栏 + 内容区
// 所有"非登录页"页面（Home/Resume/Job/Match/Interview/Membership）都通过这个外壳渲染。
//
// 人话讲解：
// - 为什么不用 el-container？因为本项目页面较简单，每个页面想自己控制 padding，
//   强约束太大反而不好做。这里用最朴素的 flex + 侧栏，灵活度最高。
//
// 【移动端方案】
// 桌面：左侧 220px 固定侧栏（不变）。
// 手机（≤768px）：侧栏变成"抽屉"——默认滑出屏幕外，点顶栏的 ☰ 按钮滑进来，
//   点遮罩或点菜单项自动收起。这样内容区能拿到 100% 宽度。
//   为什么不改成底部 tab 栏？底部栏要压住页面底部内容，且本项目只有 6 个菜单、
//   每页都有长表单，抽屉更省屏幕空间。
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const username = computed(() => userStore.username || localStorage.getItem('campus_username') || '同学')

// 导航菜单：图标用 emoji 占位（0依赖、跨平台、立得住）
const menus = [
  { path: '/home', label: '首页', icon: '🏠' },
  { path: '/resume', label: '简历管理', icon: '📄' },
  { path: '/job', label: '岗位管理', icon: '💼' },
  { path: '/match', label: 'AI 匹配评分', icon: '🎯' },
  { path: '/interview', label: '模拟面试', icon: '🎤' },
  { path: '/membership', label: '会员中心', icon: '👑' }
]

const activeMenu = computed(() => route.path)

// ===== 移动端抽屉开关 =====
// 只在窄屏生效：宽屏时这个 class 挂上去也没有对应样式，无副作用。
const drawerOpen = ref(false)

function toggleDrawer() {
  drawerOpen.value = !drawerOpen.value
}
function closeDrawer() {
  drawerOpen.value = false
}

function goMenu(path) {
  closeDrawer()               // 手机上点完菜单顺手收起，省一次点击
  if (path === route.path) return
  router.push(path)
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

// 切路由后自动收起（比如点了页面内的按钮跳转）
watch(() => route.path, closeDrawer)

// 从手机横屏切回宽屏时，把抽屉状态清掉，避免遮罩残留挡住桌面布局
function onResize() {
  if (window.innerWidth > 768) drawerOpen.value = false
}
onMounted(() => window.addEventListener('resize', onResize))
onBeforeUnmount(() => window.removeEventListener('resize', onResize))

// 当前页面标题（顶栏右上角）
const pageTitle = computed(() => {
  return menus.find(m => m.path === activeMenu.value)?.label || 'AI 校招助手'
})
</script>

<template>
  <div class="app-layout">
    <!-- ========== 左侧栏（手机上是抽屉） ========== -->
    <aside class="sidebar" :class="{ 'drawer-open': drawerOpen }">
      <div class="brand" @click="goMenu('/home')">
        <div class="logo">AI</div>
        <div class="brand-text">
          <div class="brand-name">校招助手</div>
          <div class="brand-tag">AI · 求职加速器</div>
        </div>
      </div>

      <nav class="menu">
        <div
          v-for="m in menus"
          :key="m.path"
          class="menu-item"
          :class="{ active: activeMenu === m.path }"
          @click="goMenu(m.path)"
        >
          <span class="menu-icon">{{ m.icon }}</span>
          <span class="menu-label">{{ m.label }}</span>
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="footer-tip">v1.0 · MVP</div>
      </div>
    </aside>

    <!-- 手机端抽屉遮罩：点一下收起 -->
    <div v-if="drawerOpen" class="drawer-mask" @click="closeDrawer"></div>

    <!-- ========== 右侧 ========== -->
    <div class="main">
      <!-- 顶栏 -->
      <header class="topbar">
        <div class="topbar-left">
          <button class="hamburger" type="button" aria-label="打开菜单" @click="toggleDrawer">
            <span></span><span></span><span></span>
          </button>
          <div class="page-title">{{ pageTitle }}</div>
        </div>
        <div class="user-area">
          <span class="hello">你好，<b>{{ username }}</b></span>
          <el-button text @click="handleLogout" class="logout-btn">退出</el-button>
        </div>
      </header>

      <!-- 内容区：每个页面都从这里渲染 -->
      <main class="content fade-in">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
/* ========== 整体布局：左侧栏固定宽，右侧填满 ========== */
.app-layout {
  display: flex;
  height: 100vh;
  /* 用 100% 而不是 100vw：100vw 在移动端会把滚动条宽度也算进去，导致横向溢出 */
  width: 100%;
  background: var(--ink-50);
}

/* ========== 左侧栏 ========== */
.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #1E1B4B 0%, #312E81 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  padding: 22px 14px;
  box-shadow: 2px 0 30px rgba(99, 102, 241, 0.08);
  z-index: 30;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 8px 22px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 16px;
}

.logo {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: var(--grad-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: #fff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.5);
}

.brand-text { display: flex; flex-direction: column; }
.brand-name { font-size: 16px; font-weight: 700; letter-spacing: 0.5px; }
.brand-tag { font-size: 11px; color: rgba(255, 255, 255, 0.55); margin-top: 2px; }

.menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.78);
  transition: background 0.15s ease, color 0.15s ease, transform 0.15s ease;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}
.menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}
.menu-item.active {
  background: var(--grad-primary);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4);
}

.menu-icon { font-size: 16px; width: 22px; text-align: center; }
.menu-label { flex: 1; }

.sidebar-footer {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: 12px;
}
.footer-tip {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  text-align: center;
}

/* ========== 右侧 ========== */
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  border-bottom: 1px solid var(--ink-100);
  flex-shrink: 0;
}

.topbar-left { display: flex; align-items: center; gap: 10px; min-width: 0; }

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink-900);
  letter-spacing: 0.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 汉堡按钮：桌面隐藏，手机显示 */
.hamburger {
  display: none;
  width: 36px;
  height: 36px;
  padding: 8px 7px;
  border: 1px solid var(--ink-100);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  flex-direction: column;
  justify-content: space-between;
  flex-shrink: 0;
}
.hamburger span {
  display: block;
  height: 2px;
  border-radius: 2px;
  background: var(--ink-700);
}

.user-area { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.hello { font-size: 14px; color: var(--ink-500); }
.hello b { color: var(--ink-900); font-weight: 600; }
.logout-btn { font-size: 13px; }

/* ========== 内容区 ========== */
.content {
  flex: 1;
  padding: 24px 28px;
  overflow-y: auto;
  min-height: 0;
  /* 允许内部元素收缩，避免子元素把父容器撑宽 */
  min-width: 0;
  -webkit-overflow-scrolling: touch;
}

/* 抽屉遮罩：桌面不存在 */
.drawer-mask { display: none; }

/* ============================================================
   移动端断点（≤768px）：侧栏 → 抽屉
   ============================================================ */
@media (max-width: 768px) {
  .app-layout {
    /* 手机上改用 dvh，避开 iOS 地址栏收缩导致的 100vh 溢出。
       老浏览器不认 dvh，所以上一行 100vh 是兜底。 */
    height: 100vh;
    height: 100dvh;
  }

  .sidebar {
    position: fixed;
    top: 0;
    bottom: 0;
    left: 0;
    width: 232px;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    overflow-y: auto;
    padding: 18px 12px;
  }
  .sidebar.drawer-open {
    transform: translateX(0);
    box-shadow: 4px 0 30px rgba(0, 0, 0, 0.35);
  }

  .drawer-mask {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(17, 24, 39, 0.45);
    z-index: 20;
  }

  .hamburger { display: flex; }

  .topbar {
    height: 54px;
    padding: 0 14px;
  }
  .page-title { font-size: 15px; }

  .content {
    padding: 14px 12px 28px;
  }
}

/* 超窄屏（≤400px）：顶栏再收一点，别把用户名挤掉了 */
@media (max-width: 400px) {
  .hello { display: none; }
  .content { padding: 12px 10px 24px; }
}
</style>
