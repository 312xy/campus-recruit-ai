<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPlans, getMyMembership, listMyOrders, createOrder, payOrder } from '../api/membership'
import AppLayout from '../components/AppLayout.vue'

// 会员中心页（DB 模拟支付）。
//   1) 加载套餐列表 + 我的会员状态 + 我的订单
//   2) 点套餐卡 → 创建订单（订单号 UUID） → 弹窗展示订单信息 → 点"确认支付" → 后端改 PAID + 自动开通/续期会员
//   3) 不接真实网关 = 演示够用，避免合规和资质成本

const plans = ref([])
const membership = ref(null)
const orders = ref([])
const loading = ref(false)

const payDialogVisible = ref(false)
const pendingOrder = ref(null)
const paying = ref(false)

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const [p, m, o] = await Promise.all([listPlans(), getMyMembership(), listMyOrders()])
    plans.value = p
    membership.value = m
    orders.value = o
  } catch {} finally {
    loading.value = false
  }
}

async function choosePlan(plan) {
  try {
    const order = await createOrder(plan.code)
    pendingOrder.value = order
    payDialogVisible.value = true
  } catch {}
}

async function confirmPay() {
  if (!pendingOrder.value) return
  paying.value = true
  try {
    const res = await payOrder(pendingOrder.value.orderNo)
    ElMessage.success(`支付成功！${res.membership?.planName || ''} 已开通`)
    payDialogVisible.value = false
    pendingOrder.value = null
    await loadAll()
  } catch {} finally {
    paying.value = false
  }
}

function fmt(t) { if (!t) return '-'; return String(t).replace('T', ' ').slice(0, 19) }
function statusLabel(s) {
  return { PENDING: '待支付', PAID: '已支付', REFUNDED: '已退款' }[s] || s
}
function statusType(s) {
  return { PENDING: 'warning', PAID: 'success', REFUNDED: 'info' }[s] || ''
}

// 价格显示辅助：把 priceYuan 转大字号视觉
function priceParts(priceYuan) {
  return { int: Math.floor(priceYuan), dec: Math.round((priceYuan % 1) * 100) }
}
</script>

<template>
  <AppLayout>
    <div class="mem-content">
      <!-- 当前会员状态 -->
      <section v-if="membership && membership.active" class="status-banner status-active">
        <div class="sb-bg">
          <div class="sb-circle c1"></div>
          <div class="sb-circle c2"></div>
        </div>
        <div class="sb-icon">👑</div>
        <div class="sb-info">
          <div class="sb-title">当前会员 · {{ membership.planName }}</div>
          <div class="sb-meta">
            有效期 {{ fmt(membership.startsAt) }} 至 {{ fmt(membership.expiresAt) }}
          </div>
          <div class="sb-days">
            <span class="big">{{ membership.daysLeft }}</span><span class="small">天</span>
            剩余
          </div>
        </div>
        <div class="sb-action">
          <div class="sb-tag">已开通</div>
          <div class="sb-sub">续期会自动顺延，不会浪费</div>
        </div>
      </section>

      <section v-else class="status-banner status-none">
        <div class="sb-icon">🔓</div>
        <div class="sb-info">
          <div class="sb-title">还未开通会员</div>
          <div class="sb-meta">订阅后解锁「高级 AI 评分 / 模拟面试终评 / 优先响应」等会员专享功能</div>
        </div>
        <div class="sb-action">
          <div class="sb-tag muted">未订阅</div>
        </div>
      </section>

      <!-- 套餐 -->
      <section class="plans-section">
        <div class="section-head">
          <div class="section-title-text">📦 选择套餐</div>
          <div class="section-sub">所有套餐均为 DB 模拟支付 · 不接真实网关</div>
        </div>

        <div class="plans-grid">
          <div
            v-for="p in plans"
            :key="p.code"
            class="plan-card pretty-card"
            :class="{ recommended: p.recommended }"
          >
            <div v-if="p.recommended" class="ribbon">⭐ 最受欢迎</div>
            <div class="plan-name">{{ p.name }}</div>
            <div class="plan-price">
              <span class="yuan">¥</span>
              <span class="big">{{ priceParts(p.priceYuan).int }}</span>
              <span v-if="priceParts(p.priceYuan).dec" class="dec">.{{ priceParts(p.priceYuan).dec }}</span>
              <span class="day">/ {{ p.days }} 天</span>
            </div>
            <div class="plan-daily">≈ 每天 ¥{{ (p.priceYuan / p.days).toFixed(2) }}</div>
            <ul class="feature-list">
              <li v-for="(f, i) in p.features" :key="i">
                <span class="check">✓</span> {{ f }}
              </li>
            </ul>
            <el-button
              :type="p.recommended ? 'primary' : ''"
              :plain="!p.recommended"
              :class="p.recommended ? 'btn-gradient' : ''"
              size="large"
              style="width:100%; margin-top: 8px; font-weight: 600;"
              @click="choosePlan(p)"
            >
              立即订阅
            </el-button>
          </div>
        </div>
      </section>

      <!-- 订单历史 -->
      <section class="pretty-card panel">
        <div class="panel-head">
          <div class="panel-title">📜 订单历史（{{ orders.length }}）</div>
        </div>
        <el-empty v-if="!orders.length && !loading" description="还没有订单" :image-size="70" />
        <el-table v-else :data="orders" size="small" style="width:100%">
          <el-table-column prop="orderNo" label="订单号" show-overflow-tooltip min-width="220" />
          <el-table-column prop="planName" label="套餐" width="120" />
          <el-table-column label="金额" width="100">
            <template #default="{ row }">
              <strong style="color:#EF4444">¥ {{ row.amountYuan }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" width="170">
            <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="支付时间">
            <template #default="{ row }">{{ fmt(row.paidAt) }}</template>
          </el-table-column>
        </el-table>
      </section>
    </div>

    <!-- 支付确认弹窗 -->
    <el-dialog v-model="payDialogVisible" title="确认订单" width="460px" :show-close="!paying" align-center>
      <div v-if="pendingOrder" class="pay-info">
        <div class="pay-plan">
          <div class="pp-name">{{ pendingOrder.planName }}</div>
          <div class="pp-price">¥ {{ pendingOrder.amountYuan }}</div>
        </div>

        <div class="pay-detail">
          <div class="pay-row">
            <span class="key">订单号</span>
            <span class="val mono">{{ pendingOrder.orderNo }}</span>
          </div>
          <div class="pay-row">
            <span class="key">状态</span>
            <el-tag size="small" type="warning" effect="plain">待支付</el-tag>
          </div>
          <div class="pay-row">
            <span class="key">支付方式</span>
            <span class="val">💳 DB 模拟（项目演示用）</span>
          </div>
        </div>

        <div class="pay-warn">
          <span class="emoji">⚠️</span>
          <span>点「确认支付」后，后端会把订单改为 PAID，并立即开通 / 续期会员 —— <b>不会调用任何真实网关</b>。</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="payDialogVisible = false" :disabled="paying">取消</el-button>
        <el-button
          type="primary"
          class="btn-gradient"
          :loading="paying"
          @click="confirmPay"
        >
          确认支付
        </el-button>
      </template>
    </el-dialog>
  </AppLayout>
</template>

<style scoped>
.mem-content {
  max-width: 1100px;
  margin: 0 auto;
  display: flex; flex-direction: column; gap: 22px;
}

/* ========== 当前会员状态 ========== */
.status-banner {
  position: relative;
  border-radius: 16px;
  padding: 22px 28px;
  display: flex;
  align-items: center;
  gap: 20px;
  overflow: hidden;
  box-shadow: 0 8px 30px rgba(0,0,0,0.06);
}
.status-active {
  background: linear-gradient(120deg, #FEF3C7 0%, #FDE68A 50%, #FBBF24 100%);
  color: #78350F;
}
.status-none {
  background: #fff;
  border: 1px solid var(--ink-100);
  color: var(--ink-700);
}
.sb-bg { position: absolute; inset: 0; pointer-events: none; }
.sb-circle { position: absolute; border-radius: 50%; background: rgba(255,255,255,0.2); }
.sb-circle.c1 { width: 200px; height: 200px; top: -60px; right: 10%; }
.sb-circle.c2 { width: 120px; height: 120px; bottom: -40px; left: 30%; }

.sb-icon { font-size: 56px; flex-shrink: 0; z-index: 1; }
.sb-info { flex: 1; z-index: 1; }
.sb-title { font-size: 18px; font-weight: 700; }
.sb-meta { font-size: 13px; opacity: 0.85; margin-top: 6px; }
.sb-days { margin-top: 12px; display: flex; align-items: baseline; gap: 4px; }
.sb-days .big { font-size: 36px; font-weight: 800; color: #B45309; }
.sb-days .small { font-size: 14px; margin-right: 4px; }

.sb-action { text-align: right; z-index: 1; }
.sb-tag {
  background: rgba(255,255,255,0.4);
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  color: #92400E;
}
.sb-tag.muted { color: var(--ink-500); background: var(--ink-100); }
.sb-sub { font-size: 11px; margin-top: 6px; opacity: 0.7; }

/* ========== 套餐 ========== */
.plans-section {
  display: flex; flex-direction: column; gap: 14px;
}
.section-head {
  display: flex; align-items: baseline; gap: 12px;
  margin-left: 4px;
}
.section-title-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--ink-900);
}
.section-sub { font-size: 12px; color: var(--ink-500); }

.plans-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
@media (max-width: 800px) {
  .plans-grid { grid-template-columns: 1fr; }
}

.plan-card {
  position: relative;
  padding: 24px 22px;
  text-align: center;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  flex-direction: column;
}
.plan-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 40px rgba(99, 102, 241, 0.18);
}
.plan-card.recommended {
  border: 2px solid var(--brand-500);
  box-shadow: 0 8px 30px rgba(99, 102, 241, 0.2);
  background: linear-gradient(180deg, #FAFBFF 0%, #FFFFFF 100%);
}

.ribbon {
  position: absolute;
  top: 14px;
  right: -8px;
  background: var(--grad-primary);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 4px 14px;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.plan-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink-900);
  margin-bottom: 16px;
}
.plan-card.recommended .plan-name {
  background: var(--grad-primary);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-size: 18px;
}

.plan-price {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 2px;
  margin-bottom: 6px;
}
.plan-price .yuan { font-size: 16px; color: #EF4444; }
.plan-price .big { font-size: 42px; font-weight: 800; color: #EF4444; line-height: 1; }
.plan-price .dec { font-size: 18px; color: #EF4444; font-weight: 600; }
.plan-price .day { font-size: 13px; color: var(--ink-500); margin-left: 4px; }
.plan-daily { font-size: 11px; color: var(--ink-500); margin-bottom: 18px; }

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0 0 18px;
  font-size: 13px;
  color: var(--ink-700);
  line-height: 1.9;
  text-align: left;
}
.feature-list li {
  display: flex;
  align-items: center;
  gap: 8px;
}
.check {
  color: var(--success-500);
  font-weight: 700;
}

/* ========== 订单面板 ========== */
.panel { padding: 18px 20px; }
.panel-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.panel-title { font-size: 15px; font-weight: 600; color: var(--ink-900); }

/* ========== 支付弹窗 ========== */
.pay-info { padding: 0 4px; }
.pay-plan {
  background: linear-gradient(135deg, #FFF7ED 0%, #FEF3C7 100%);
  border-radius: 12px;
  padding: 16px 20px;
  text-align: center;
  margin-bottom: 16px;
}
.pp-name { font-size: 14px; color: var(--ink-700); font-weight: 500; margin-bottom: 6px; }
.pp-price { font-size: 32px; font-weight: 800; color: #EF4444; }

.pay-detail {
  background: var(--ink-50);
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 14px;
}
.pay-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px dashed var(--ink-100);
}
.pay-row:last-child { border-bottom: none; }
.pay-row .key { color: var(--ink-500); }
.pay-row .val { color: var(--ink-900); font-weight: 500; }
.pay-row .mono { font-family: 'Consolas', 'Monaco', monospace; font-size: 12px; }

.pay-warn {
  background: rgba(245, 158, 11, 0.08);
  border-left: 3px solid #F59E0B;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 12px;
  color: #92400E;
  line-height: 1.6;
}
.pay-warn .emoji { margin-right: 6px; }

/* ============================================================
   移动端（≤768px）
   ============================================================ */
@media (max-width: 768px) {
  .mem-content { gap: 14px; }

  /* 会员状态横幅：图标 + 信息 + 右侧标签，窄屏改成换行 */
  .status-banner {
    flex-wrap: wrap;
    padding: 16px 16px;
    gap: 12px;
    border-radius: 14px;
  }
  .sb-icon { font-size: 40px; }
  .sb-title { font-size: 16px; }
  .sb-meta { font-size: 12px; line-height: 1.5; }
  .sb-days .big { font-size: 28px; }
  .sb-days .small { font-size: 13px; }
  /* 右侧"已开通"标签：整行靠左，不再挤在文字边上 */
  .sb-action {
    flex: 1 1 100%;
    text-align: left;
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .sb-tag { font-size: 13px; padding: 5px 14px; }
  .sb-sub { margin-top: 0; font-size: 11px; }

  .section-title-text { font-size: 16px; }
  .section-sub { font-size: 11px; }

  .plans-grid { gap: 12px; }
  .plan-card { padding: 18px 16px; }
  .plan-price .big { font-size: 36px; }

  .panel { padding: 14px 14px; }
  .panel-title { font-size: 14px; }

  /* 支付弹窗里的订单号很长，允许换行别把布局顶破 */
  .pay-row { flex-wrap: wrap; gap: 4px; }
  .pay-row .val { text-align: right; }
  .pay-row .mono { font-size: 11px; word-break: break-all; }
  .pp-price { font-size: 28px; }
}
</style>
