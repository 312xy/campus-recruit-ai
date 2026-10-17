<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../api/auth'
import { useUserStore } from '../stores/user'

// 登录 / 注册页。
//
// 人话讲解：
// - activeTab 记录当前是"登录"还是"注册"，el-tabs 用它来切换。
// - 两个表单都绑定到 reactive 对象（loginForm / registerForm），
//   这叫"双向绑定"：输入框内容变了，JS 对象里的值跟着变；反之亦然。
// - rules 是校验规则（Element Plus 的表单校验），点提交前先做前端校验，
//   减少无意义的网络请求。注意：前端校验只是体验优化，后端也必须校验（我们后端有 @Valid）。
// - 登录成功后：把 token 存进 Pinia → 跳到首页。之后所有请求会自动带上这个 token。
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)

const loginFormRef = ref()
const registerFormRef = ref()

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', email: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度 3-20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

async function handleLogin() {
  // validate() 返回 Promise，校验不通过会抛错，用 try/catch 兜住
  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const res = await login(loginForm)
    userStore.setLogin(res.token, loginForm.username)
    ElMessage.success('登录成功')
    router.push('/home')
  } catch {
    // 错误提示已在 axios 响应拦截器里统一弹过了，这里不用重复处理
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  try {
    await registerFormRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await register(registerForm)
    ElMessage.success('注册成功，请登录')
    // 注册成功后自动切到登录页，并顺手把用户名带过去
    loginForm.username = registerForm.username
    loginForm.password = ''
    activeTab.value = 'login'
  } catch {
    // 同上，错误提示由拦截器统一处理
  } finally {
    loading.value = false
  }
}

// 品牌区三个价值点（呼应"AI 校招助手"的卖点）
const valuePoints = [
  { icon: '📄', title: 'AI 简历解析', desc: '上传即得结构化字段' },
  { icon: '🎯', title: '匹配度评分', desc: '简历 × 岗位，看准了再投' },
  { icon: '🎤', title: '模拟面试', desc: '按岗位出题，AI 点评打分' }
]
</script>

<template>
  <div class="login-page">
    <!-- ====== 左半：品牌区 ====== -->
    <div class="brand-side">
      <div class="brand-bg-circle circle-1"></div>
      <div class="brand-bg-circle circle-2"></div>
      <div class="brand-bg-circle circle-3"></div>

      <div class="brand-content">
        <div class="brand-logo">
          <div class="logo-block">AI</div>
          <span class="logo-name">校招助手</span>
        </div>

        <h1 class="brand-slogan">让 AI 帮你拿下<br/>心仪 Offer</h1>
        <p class="brand-sub">深度学习大模型驱动 · 为 2026 届同学量身定制</p>

        <div class="value-list">
          <div class="value-item" v-for="(v, i) in valuePoints" :key="i">
            <div class="value-icon">{{ v.icon }}</div>
            <div>
              <div class="value-title">{{ v.title }}</div>
              <div class="value-desc">{{ v.desc }}</div>
            </div>
          </div>
        </div>

        <div class="brand-footer">
          © 2026 AI 校招助手 · Made by a senior student
        </div>
      </div>
    </div>

    <!-- ====== 右半：表单 ====== -->
    <div class="form-side">
      <el-card class="login-card" shadow="always">
        <div class="form-header">
          <h2>欢迎回来 👋</h2>
          <p class="muted">{{ activeTab === 'login' ? '使用账号密码继续你的求职之旅' : '30 秒创建账号，开始你的求职加速' }}</p>
        </div>

        <el-tabs v-model="activeTab" class="auth-tabs" stretch>
          <!-- ========== 登录 ========== -->
          <el-tab-pane label="登录" name="login">
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              label-position="top"
              @submit.prevent="handleLogin"
            >
              <el-form-item label="用户名" prop="username">
                <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入密码"
                  size="large"
                  show-password
                  @keyup.enter="handleLogin"
                />
              </el-form-item>
              <el-button
                type="primary"
                size="large"
                class="submit-btn btn-gradient"
                :loading="loading"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form>
          </el-tab-pane>

          <!-- ========== 注册 ========== -->
          <el-tab-pane label="注册" name="register">
            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              label-position="top"
              @submit.prevent="handleRegister"
            >
              <el-form-item label="用户名" prop="username">
                <el-input v-model="registerForm.username" placeholder="3-20 个字符" size="large" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  placeholder="至少 6 位"
                  size="large"
                  show-password
                />
              </el-form-item>
              <el-form-item label="邮箱（选填）" prop="email">
                <el-input v-model="registerForm.email" placeholder="用于接收求职提醒" size="large" />
              </el-form-item>
              <el-button
                type="primary"
                size="large"
                class="submit-btn btn-gradient"
                :loading="loading"
                @click="handleRegister"
              >
                注 册
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <div class="form-foot muted">
          登录即同意 <span class="text-brand">用户协议</span> 和 <span class="text-brand">隐私政策</span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  overflow: hidden;
  background: #FAFAFB;
}

/* ========== 左半：品牌区 ========== */
.brand-side {
  flex: 1.2;
  position: relative;
  background: linear-gradient(135deg, #6366F1 0%, #8B5CF6 50%, #06B6D4 100%);
  color: #fff;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

/* 背景装饰圆 */
.brand-bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}
.circle-1 { width: 400px; height: 400px; top: -120px; right: -100px; }
.circle-2 { width: 280px; height: 280px; bottom: -60px; left: -80px; background: rgba(255,255,255,0.05); }
.circle-3 { width: 180px; height: 180px; top: 40%; right: 15%; background: rgba(255,255,255,0.06); }

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 460px;
}

.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 14px 6px 6px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 999px;
  backdrop-filter: blur(10px);
  margin-bottom: 24px;
}
.logo-block {
  width: 30px; height: 30px;
  border-radius: 50%;
  background: #fff;
  color: var(--brand-600);
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 13px;
}
.logo-name { font-size: 14px; font-weight: 500; }

.brand-slogan {
  font-size: 38px;
  font-weight: 700;
  line-height: 1.3;
  margin: 0 0 14px;
  letter-spacing: 0.5px;
}
.brand-sub {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 36px;
}

.value-list { display: flex; flex-direction: column; gap: 14px; }
.value-item {
  display: flex;
  align-items: center;
  gap: 14px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(10px);
  padding: 12px 16px;
  border-radius: 12px;
  transition: transform 0.2s ease;
}
.value-item:hover { transform: translateX(4px); }

.value-icon {
  font-size: 24px;
  width: 44px; height: 44px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.18);
  display: flex; align-items: center; justify-content: center;
}
.value-title { font-size: 15px; font-weight: 600; margin-bottom: 2px; }
.value-desc { font-size: 12px; color: rgba(255,255,255,0.78); }

.brand-footer {
  position: absolute;
  bottom: -50px;
  left: 0; right: 0;
  text-align: center;
  font-size: 12px;
  color: rgba(255,255,255,0.6);
}

/* ========== 右半：表单区 ========== */
.form-side {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 8px 12px 18px;
  border: none;
}

.form-header { text-align: center; margin-bottom: 22px; }
.form-header h2 {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 700;
  background: var(--grad-primary);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.form-header p { margin: 0; font-size: 13px; }

.auth-tabs :deep(.el-tabs__nav-wrap::after) { background: transparent; }
.auth-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
}
.auth-tabs :deep(.el-tabs__item.is-active) {
  color: var(--brand-600);
  font-weight: 600;
}
.auth-tabs :deep(.el-tabs__active-bar) {
  background: var(--brand-500) !important;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  font-weight: 600;
  letter-spacing: 2px;
}

.form-foot {
  margin-top: 18px;
  text-align: center;
  font-size: 12px;
}

@media (max-width: 900px) {
  .brand-side { display: none; }
  .form-side { flex: 1; padding: 20px 16px; }
}

/* 移动端（≤768px）补充：900px 那条只是隐藏品牌区，
   手机上还要解决"卡片竖向居中 + 键盘弹出时挤爆"的问题 */
@media (max-width: 768px) {
  .login-page {
    /* 关键：原来 height:100vh + overflow:hidden，
       键盘一弹起来，登录按钮就被压到看不见的地方且无法滚动。
       先写 100vh 兜底（老安卓不认 dvh），再写 dvh 覆盖它；
       同时允许纵向滚动，键盘顶上来时用户能滑到按钮。 */
    height: 100vh;
    height: 100dvh;
    min-height: 100vh;
    min-height: 100dvh;
    overflow-y: auto;
    overflow-x: hidden;
  }
  .form-side {
    padding: 16px 14px;
    align-items: flex-start;
    /* 顶部留 6vh 而不是硬居中，键盘弹出时内容不会被顶出屏幕 */
    padding-top: 6vh;
  }
  .login-card { max-width: 100%; padding: 4px 8px 14px; }
  .form-header { margin-bottom: 16px; }
  .form-header h2 { font-size: 22px; }
  .form-header p { font-size: 12px; }
  .auth-tabs :deep(.el-tabs__item) { font-size: 14px; }
  .submit-btn { letter-spacing: 1px; }
  .form-foot { margin-top: 14px; font-size: 11px; }
}
</style>
