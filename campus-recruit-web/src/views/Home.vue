<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMe } from '../api/auth'
import { listResumes } from '../api/resume'
import { listJobs } from '../api/job'
import { listMatches } from '../api/match'
import { listInterviews } from '../api/interview'
import { getMyMembership } from '../api/membership'
import { useUserStore } from '../stores/user'
import AppLayout from '../components/AppLayout.vue'

// 首页（登录后可见）。
//
// 人话讲解：
// - onMounted 是 Vue 的"生命周期钩子"：组件挂载到页面后自动执行。
//   我们在这里调 /api/auth/me 验证 token 是否真的有效 —— 这一步很关键：
//   token 存在 localStorage 里不代表它没过期/没被改，只有后端点头才算真登录。
// - 顶部 4 个统计数字是用 Promise.all 并行调 4 个接口，
//   比"串行调用 4 次"快约 75% —— 演示并发请求的常见写法。
const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)

// 顶部统计
const stats = ref({ resumes: 0, jobs: 0, matches: 0, interviews: 0 })
const membership = ref(null)

// 模块卡片
const modules = [
  { title: '简历管理', desc: '上传简历 · AI 自动解析成结构化字段', tag: '已上线', route: '/resume',
    icon: '📄', color: 'blue' },
  { title: '岗位管理', desc: '粘贴 JD · AI 自动打技能/城市/薪资/坑点标签', tag: '已上线', route: '/job',
    icon: '💼', color: 'purple' },
  { title: 'AI 匹配评分', desc: '简历 × 岗位 → 0-100 匹配度 + 差距分析', tag: '已上线', route: '/match',
    icon: '🎯', color: 'cyan' },
  { title: '模拟面试', desc: '按岗位出题 → 作答 → AI 逐题点评 + 终评', tag: '已上线', route: '/interview',
    icon: '🎤', color: 'amber' },
  { title: '会员中心', desc: '订阅解锁高级功能（数据库模拟支付）', tag: '已上线', route: '/membership',
    icon: '👑', color: 'rose' }
]

function goModule(m) {
  if (m.route) router.push(m.route)
}

onMounted(async () => {
  // 几个"看起来独立"的接口用 Promise.all 并发，比串行快多了
  const [meRes, resumes, jobs, matches, interviews, ms] = await Promise.allSettled([
    getMe(),
    listResumes(),
    listJobs(),
    listMatches(),
    listInterviews(),
    getMyMembership()
  ])
  if (meRes.status === 'fulfilled') {
    userStore.username = meRes.value.username
  }
  stats.value.resumes = resumes.status === 'fulfilled' ? (resumes.value.length || 0) : 0
  stats.value.jobs = jobs.status === 'fulfilled' ? (jobs.value.length || 0) : 0
  stats.value.matches = matches.status === 'fulfilled' ? (matches.value.length || 0) : 0
  stats.value.interviews = interviews.status === 'fulfilled' ? (interviews.value.length || 0) : 0
  membership.value = ms.status === 'fulfilled' ? ms.value : null
  loading.value = false
})
</script>

<template>
  <AppLayout>
    <div class="home-content">
      <!-- ========== Hero 横幅 ========== -->
      <section class="hero">
        <div class="hero-bg">
          <div class="hero-circle c1"></div>
          <div class="hero-circle c2"></div>
          <div class="hero-circle c3"></div>
        </div>

        <div class="hero-text">
          <div class="hero-eyebrow">⚡ AI 驱动的求职加速器</div>
          <h1 class="hero-title">
            你好，<span class="gradient-text">{{ userStore.username || '同学' }}</span> 👋<br/>
            今天离 offer 又近一步
          </h1>
          <p class="hero-sub">把简历和岗位交给我们，AI 帮你匹配最优目标、出最贴题的模拟面试题。</p>
          <div class="hero-actions">
            <el-button type="primary" size="large" class="btn-gradient" @click="router.push('/resume')">
              🚀 上传简历开始
            </el-button>
            <el-button size="large" plain @click="router.push('/match')">查看匹配评分</el-button>
          </div>
        </div>

        <!-- 装饰：右侧"漂浮"的卡片 -->
        <div class="hero-art">
          <div class="art-card art-1">
            <div class="art-icon">📄</div>
            <div class="art-line">AI 已解析 · 命中 12 项技能</div>
          </div>
          <div class="art-card art-2">
            <div class="art-icon">🎯</div>
            <div class="art-line">匹配度 <b style="color:#10B981">92</b> 分</div>
          </div>
          <div class="art-card art-3">
            <div class="art-icon">🎤</div>
            <div class="art-line">AI 终评 <b style="color:#6366F1">良好</b></div>
          </div>
        </div>
      </section>

      <!-- ========== 4 个统计卡片 ========== -->
      <section class="stats">
        <div class="stat-card stat-1">
          <div class="stat-icon">📄</div>
          <div class="stat-value">{{ stats.resumes }}</div>
          <div class="stat-label">我的简历</div>
        </div>
        <div class="stat-card stat-2">
          <div class="stat-icon">💼</div>
          <div class="stat-value">{{ stats.jobs }}</div>
          <div class="stat-label">关注岗位</div>
        </div>
        <div class="stat-card stat-3">
          <div class="stat-icon">🎯</div>
          <div class="stat-value">{{ stats.matches }}</div>
          <div class="stat-label">匹配评估</div>
        </div>
        <div class="stat-card stat-4">
          <div class="stat-icon">🎤</div>
          <div class="stat-value">{{ stats.interviews }}</div>
          <div class="stat-label">模拟面试</div>
        </div>
      </section>

      <!-- ========== 会员状态（如有） ========== -->
      <section v-if="membership && membership.active" class="member-banner">
        <div class="mb-icon">👑</div>
        <div class="mb-info">
          <div class="mb-title">当前会员：{{ membership.planName }}</div>
          <div class="mb-sub">剩余 {{ membership.daysLeft }} 天 · 到期 {{ (membership.expiresAt || '').slice(0,10) }}</div>
        </div>
        <el-button class="btn-gradient" @click="router.push('/membership')">会员中心</el-button>
      </section>

      <!-- ========== 模块卡片群 ========== -->
      <section class="modules">
        <h2 class="section-title">
          <span>功能模块</span>
          <span class="muted section-sub">点击卡片进入</span>
        </h2>

        <div class="module-grid">
          <div
            v-for="m in modules"
            :key="m.title"
            class="module-card pretty-card"
            :class="['mc-' + m.color]"
            @click="goModule(m)"
          >
            <div class="mc-icon">{{ m.icon }}</div>
            <div class="mc-body">
              <div class="mc-title">{{ m.title }}</div>
              <div class="mc-desc">{{ m.desc }}</div>
            </div>
            <div class="mc-arrow">→</div>
          </div>
        </div>
      </section>
    </div>
  </AppLayout>
</template>

<style scoped>
.home-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

/* ========== Hero ========== */
.hero {
  position: relative;
  background: linear-gradient(120deg, #6366F1 0%, #8B5CF6 60%, #06B6D4 100%);
  border-radius: 20px;
  padding: 36px 40px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  overflow: hidden;
  box-shadow: 0 10px 40px rgba(99, 102, 241, 0.25);
}

.hero-bg { position: absolute; inset: 0; pointer-events: none; }
.hero-circle { position: absolute; border-radius: 50%; background: rgba(255,255,255,0.08); }
.hero-circle.c1 { width: 300px; height: 300px; top: -120px; right: 280px; }
.hero-circle.c2 { width: 200px; height: 200px; bottom: -80px; left: 40%; }
.hero-circle.c3 { width: 140px; height: 140px; top: 30%; right: 30%; }

.hero-text { position: relative; z-index: 1; max-width: 560px; }
.hero-eyebrow {
  display: inline-block;
  background: rgba(255,255,255,0.18);
  padding: 4px 14px;
  border-radius: 999px;
  font-size: 12px;
  margin-bottom: 12px;
}
.hero-title {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.3;
  margin: 0 0 12px;
  letter-spacing: 0.5px;
}
.hero-sub { font-size: 14px; color: rgba(255,255,255,0.9); margin: 0 0 22px; }
.hero-actions { display: flex; gap: 12px; }
.hero-actions .el-button {
  background: rgba(255,255,255,0.95) !important;
  color: var(--brand-600) !important;
  border: none !important;
  font-weight: 600;
}
.hero-actions .el-button.plain {
  background: transparent !important;
  color: #fff !important;
  border: 1px solid rgba(255,255,255,0.4) !important;
}

/* Hero 右侧装饰卡 */
.hero-art { position: relative; width: 260px; height: 220px; flex-shrink: 0; }
.art-card {
  position: absolute;
  background: #fff;
  color: var(--ink-900);
  padding: 12px 14px;
  border-radius: 12px;
  box-shadow: 0 8px 28px rgba(0,0,0,0.18);
  display: flex; align-items: center; gap: 10px;
  font-size: 13px;
  animation: float 4s ease-in-out infinite;
}
.art-icon { font-size: 22px; }
.art-1 { top: 10px; right: 0; animation-delay: 0s; }
.art-2 { top: 90px; left: 0; animation-delay: 0.5s; }
.art-3 { bottom: 0; right: 30px; animation-delay: 1s; }

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

/* ========== 统计卡片 ========== */
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px 20px;
  display: flex; align-items: center; gap: 14px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(99,102,241,0.12); }
.stat-icon {
  width: 48px; height: 48px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px;
}
.stat-1 .stat-icon { background: linear-gradient(135deg, #DBEAFE, #BFDBFE); }
.stat-2 .stat-icon { background: linear-gradient(135deg, #EDE9FE, #DDD6FE); }
.stat-3 .stat-icon { background: linear-gradient(135deg, #CFFAFE, #A5F3FC); }
.stat-4 .stat-icon { background: linear-gradient(135deg, #FEF3C7, #FDE68A); }
.stat-value { font-size: 26px; font-weight: 700; color: var(--ink-900); line-height: 1; }
.stat-label { font-size: 12px; color: var(--ink-500); margin-top: 4px; }

/* ========== 会员横幅 ========== */
.member-banner {
  background: linear-gradient(120deg, #FFF7ED 0%, #FFEDD5 100%);
  border: 1px solid #FED7AA;
  border-radius: 14px;
  padding: 16px 22px;
  display: flex; align-items: center; gap: 14px;
}
.mb-icon { font-size: 32px; }
.mb-info { flex: 1; }
.mb-title { font-weight: 600; color: var(--ink-900); }
.mb-sub { font-size: 12px; color: var(--ink-500); margin-top: 2px; }

/* ========== 模块卡片 ========== */
.section-title {
  display: flex; align-items: baseline; gap: 12px;
  margin: 0 0 14px;
  font-size: 18px; font-weight: 700;
}
.section-sub { font-size: 12px; font-weight: normal; }

.module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}
.module-card {
  padding: 18px 18px;
  display: flex; align-items: center; gap: 14px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  position: relative;
}
.module-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 30px rgba(99, 102, 241, 0.18);
}
.mc-icon {
  font-size: 28px;
  width: 52px; height: 52px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.mc-blue   .mc-icon { background: #EEF2FF; }
.mc-purple .mc-icon { background: #F3E8FF; }
.mc-cyan   .mc-icon { background: #ECFEFF; }
.mc-amber  .mc-icon { background: #FFFBEB; }
.mc-rose   .mc-icon { background: #FFE4E6; }

.mc-body { flex: 1; min-width: 0; }
.mc-title { font-size: 15px; font-weight: 600; color: var(--ink-900); margin-bottom: 4px; }
.mc-desc { font-size: 12px; color: var(--ink-500); line-height: 1.5; }
.mc-arrow {
  font-size: 18px;
  color: var(--ink-300);
  transition: transform 0.2s ease, color 0.2s ease;
}
.module-card:hover .mc-arrow { transform: translateX(4px); color: var(--brand-500); }

/* ============================================================
   移动端（≤768px）
   核心三件事：① hero 从左右两栏改成上下堆叠 ② 装饰卡从"绝对定位漂浮"
   改成一行平铺（绝对定位在窄屏必然重叠）③ 统计卡 4 列 → 2 列
   ============================================================ */
@media (max-width: 768px) {
  .home-content { gap: 14px; }

  .hero {
    flex-direction: column;
    align-items: stretch;
    /* 手机上横向 padding 一定要小，40px 会把文字挤成每行 6 个字 */
    padding: 20px 16px;
    border-radius: 14px;
    gap: 16px;
  }

  .hero-text { max-width: none; }

  .hero-eyebrow { font-size: 11px; padding: 3px 10px; margin-bottom: 10px; }

  .hero-title {
    font-size: 21px;
    line-height: 1.45;
    margin-bottom: 10px;
  }

  .hero-sub { font-size: 13px; margin-bottom: 16px; }

  /* 两个按钮改成上下排列 + 各占满宽：手指点起来最舒服 */
  .hero-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .hero-actions .el-button { width: 100%; margin-left: 0 !important; }

  /* 装饰卡：改成 3 张卡横向一排、可横向滑动，不再绝对定位 */
  .hero-art {
    position: static;
    width: 100%;
    height: auto;
    display: flex;
    gap: 8px;
    overflow-x: auto;
    padding-bottom: 2px;
    /* 三张卡加起来略宽于屏幕，用横向滚动兜住 */
    scrollbar-width: none;
  }
  .hero-art::-webkit-scrollbar { display: none; }

  .art-card {
    position: static;
    flex: 0 0 auto;
    padding: 8px 10px;
    font-size: 11px;
    border-radius: 8px;
    white-space: nowrap;
    /* 关掉漂浮动画：手机上一堆元素同时动容易掉帧 */
    animation: none;
  }
  .art-icon { font-size: 16px; }

  /* 装饰圆在窄屏没有美感可言，直接隐藏省渲染 */
  .hero-circle.c1 { width: 180px; height: 180px; top: -80px; right: -40px; }
  .hero-circle.c2,
  .hero-circle.c3 { display: none; }

  /* 统计卡：4 列 → 2 列 */
  .stats {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
  .stat-card {
    padding: 12px 12px;
    gap: 10px;
    border-radius: 12px;
  }
  .stat-icon { width: 38px; height: 38px; font-size: 17px; border-radius: 10px; }
  .stat-value { font-size: 20px; }
  .stat-label { font-size: 11px; }

  /* 会员横幅：按钮换行到下面，别和文字抢宽度 */
  .member-banner {
    flex-wrap: wrap;
    padding: 14px 16px;
    gap: 10px;
    border-radius: 12px;
  }
  .mb-icon { font-size: 26px; }
  .mb-title { font-size: 14px; }
  .mb-sub { font-size: 11px; }
  .member-banner .el-button { width: 100%; margin-left: 0; }

  .section-title { font-size: 16px; margin-bottom: 10px; }

  /* 模块卡：手机上单列更整齐（220px 的 minmax 在两列时会挤成一团） */
  .module-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .module-card { padding: 14px; gap: 12px; }
  .mc-icon { width: 42px; height: 42px; font-size: 22px; }
  .mc-title { font-size: 14px; }
  .mc-desc { font-size: 12px; }
}
</style>
