<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { evaluateMatch, listMatches, deleteMatch, getMatch, levelTagType } from '../api/match'
import { listResumes } from '../api/resume'
import { listJobs } from '../api/job'
import AppLayout from '../components/AppLayout.vue'

// 匹配评分页：
//   1) 从简历下拉框选一份，从岗位下拉框选一个（都来自"我的"列表，避免越权）；
//   2) 点「开始评估」→ 后端调大模型算分 → 这里渲染大圆环分数 + 亮点/差距/建议；
//   3) 下方是历史记录表，可以点查看/删除。

const route = useRoute()

const resumes = ref([])
const jobs = ref([])
const resumeId = ref(null)
const jobId = ref(null)
const evaluating = ref(false)
const current = ref(null)
const history = ref([])
const historyLoading = ref(false)

onMounted(async () => {
  await Promise.all([loadResumes(), loadJobs(), loadHistory()])
  if (route.query.resumeId) resumeId.value = Number(route.query.resumeId)
  if (route.query.jobId) jobId.value = Number(route.query.jobId)
})

async function loadResumes() {
  try { resumes.value = (await listResumes()).filter(r => r.status === 'PARSED') } catch {}
}
async function loadJobs() {
  try { jobs.value = (await listJobs()).filter(j => j.status === 'PARSED') } catch {}
}
async function loadHistory() {
  historyLoading.value = true
  try { history.value = await listMatches() } catch {}
  finally { historyLoading.value = false }
}

async function handleEvaluate() {
  if (!resumeId.value) { ElMessage.warning('请先选一份简历'); return }
  if (!jobId.value) { ElMessage.warning('请先选一个岗位'); return }
  evaluating.value = true
  current.value = null
  try {
    current.value = await evaluateMatch({ resumeId: resumeId.value, jobId: jobId.value })
    ElMessage.success(`评估完成：${current.value.matchedScore} 分（${current.value.parsed?.level || '-'}）`)
    await loadHistory()
  } catch {
    // 拦截器已弹错
  } finally {
    evaluating.value = false
  }
}

async function viewMatch(row) {
  try {
    current.value = await getMatch(row.id)
    resumeId.value = row.resumeId
    jobId.value = row.jobId
    ElMessage.info('已加载该评估')
  } catch {}
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除这条评估记录吗？`, '删除确认', { type: 'warning' })
  } catch { return }
  try {
    await deleteMatch(row.id)
    ElMessage.success('已删除')
    if (current.value?.id === row.id) current.value = null
    await loadHistory()
  } catch {}
}

// 评分大圆环用 SVG，简单不依赖额外图表库
const scoreCircleProps = computed(() => {
  const score = current.value?.matchedScore ?? 0
  const circumference = 314.16
  const dash = (score / 100) * circumference
  let color = '#ef4444'
  if (score >= 80) color = '#10b981'
  else if (score >= 60) color = '#3b82f6'
  else if (score >= 40) color = '#f59e0b'
  return { dash, color, circumference }
})

const scoreHint = computed(() => {
  const s = current.value?.matchedScore ?? 0
  if (s >= 80) return { text: '建议投递 🎯', color: '#10B981' }
  if (s >= 60) return { text: '可尝试投递', color: '#3B82F6' }
  if (s >= 40) return { text: '需要补足关键技能', color: '#F59E0B' }
  return { text: '暂不建议', color: '#EF4444' }
})

function levelTag(level) {
  return levelTagType(level || '差')
}
function jobLabel(j) { return `${j.title} · ${j.company || '-'} · ${j.city || '-'}` }
function resumeLabel(r) { return r.fileName || '未命名简历' }
function fmt(t) { if (!t) return '-'; return String(t).replace('T', ' ').slice(0, 19) }
function scoreColor(s) {
  if (s >= 80) return '#10B981'
  if (s >= 60) return '#3B82F6'
  if (s >= 40) return '#F59E0B'
  return '#EF4444'
}
</script>

<template>
  <AppLayout>
    <div class="match-content">
      <!-- 顶部横幅 -->
      <section class="banner" style="background: linear-gradient(120deg, #CFFAFE 0%, #F0F9FF 100%);">
        <div class="banner-icon">🎯</div>
        <div class="banner-text">
          <div class="banner-title">AI 匹配评分</div>
          <div class="banner-sub">从「技能重合度」+「必备 / 加分项」+「差距分析」+「改进建议」四个维度评估匹配度</div>
        </div>
        <div class="banner-stats">
          <div class="bs-item">
            <div class="bs-value">{{ history.length }}</div>
            <div class="bs-label">历史评估</div>
          </div>
        </div>
      </section>

      <div class="match-grid">
        <!-- ========== 左：选择 + 历史 ========== -->
        <div>
          <div class="pretty-card panel">
            <div class="panel-head">
              <div class="panel-title">📋 选择简历与岗位</div>
            </div>

            <el-form label-position="top">
              <el-form-item label="我的简历（只显示已解析的）">
                <el-select v-model="resumeId" placeholder="请选择简历" style="width:100%" size="large">
                  <el-option v-for="r in resumes" :key="r.id" :label="resumeLabel(r)" :value="r.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="我的岗位（只显示已打标签的）">
                <el-select v-model="jobId" placeholder="请选择岗位" style="width:100%" size="large">
                  <el-option v-for="j in jobs" :key="j.id" :label="jobLabel(j)" :value="j.id" />
                </el-select>
              </el-form-item>
              <el-button
                type="primary"
                size="large"
                class="btn-gradient"
                :loading="evaluating"
                @click="handleEvaluate"
                style="width:100%; padding: 14px; font-weight: 600;"
              >
                {{ evaluating ? 'AI 评估中…请稍候' : '🚀 开始评估匹配度' }}
              </el-button>
            </el-form>

            <div class="hint-box">
              <span class="hint-emoji">💡</span>
              <span>同一份简历对不同岗位分数差异很大。本系统会把每次评估保存为独立记录，方便你横向比较。</span>
            </div>
          </div>

          <div class="pretty-card panel">
            <div class="panel-head">
              <div class="panel-title">📚 历史评估（{{ history.length }}）</div>
            </div>
            <el-empty v-if="!history.length && !historyLoading" description="还没有评估记录" :image-size="70" />
            <el-table v-else :data="history" size="small" v-loading="historyLoading" style="width:100%">
              <el-table-column label="分数" width="80">
                <template #default="{ row }">
                  <strong :style="{color: scoreColor(row.matchedScore), fontSize: '15px'}">
                    {{ row.matchedScore ?? '-' }}
                  </strong>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === 'PARSED' ? 'success' : (row.status === 'FAILED' ? 'danger' : 'info')">
                    {{ row.status === 'PARSED' ? '成功' : (row.status === 'FAILED' ? '失败' : '评估中') }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="时间" width="150">
                <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="130">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="viewMatch(row)">查看</el-button>
                  <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- ========== 右：评估结果 ========== -->
        <div class="pretty-card panel result-panel">
          <div class="panel-head">
            <div class="panel-title">🎯 评估结果</div>
          </div>

          <div v-if="!current" class="empty-result">
            <div class="er-icon">🎯</div>
            <div class="er-title">还没有评估结果</div>
            <div class="er-sub">左侧选择一份简历 + 一个岗位，点「开始评估」后这里会出现分数 + 详细分析</div>
          </div>

          <div v-else class="result">
            <div class="score-block">
              <svg class="score-ring" viewBox="0 0 120 120">
                <circle cx="60" cy="60" r="50" fill="none" stroke="#E5E7EB" stroke-width="10" />
                <circle cx="60" cy="60" r="50" fill="none"
                        :stroke="scoreCircleProps.color" stroke-width="10"
                        :stroke-dasharray="`${scoreCircleProps.dash} ${scoreCircleProps.circumference}`"
                        stroke-linecap="round" transform="rotate(-90 60 60)"
                        style="transition: stroke-dasharray 0.8s ease, stroke 0.3s ease;" />
                <text x="60" y="62" text-anchor="middle"
                      :fill="scoreCircleProps.color" font-size="32" font-weight="700">
                  {{ current.matchedScore ?? 0 }}
                </text>
                <text x="60" y="80" text-anchor="middle" fill="#6B7280" font-size="9">
                  / 100 分
                </text>
              </svg>
              <el-tag :type="levelTag(current.parsed?.level)" size="large" effect="dark" class="level-tag">
                {{ current.parsed?.level || '-' }}
              </el-tag>
              <div class="score-hint" :style="{color: scoreHint.color}">{{ scoreHint.text }}</div>
            </div>

            <div class="block">
              <div class="block-title">
                <span class="dot match"></span>已匹配的技能（{{ current.parsed?.matchingSkills?.length || 0 }}）
              </div>
              <div v-if="current.parsed?.matchingSkills?.length" class="tag-cloud">
                <el-tag v-for="s in current.parsed.matchingSkills" :key="s" type="success" effect="light" class="skill-tag" round>{{ s }}</el-tag>
              </div>
              <span v-else class="empty-text">未识别到重合技能</span>
            </div>

            <div class="block">
              <div class="block-title">
                <span class="dot missing"></span>岗位要求但简历没有（{{ current.parsed?.missingSkills?.length || 0 }}）
              </div>
              <div v-if="current.parsed?.missingSkills?.length" class="tag-cloud">
                <el-tag v-for="s in current.parsed.missingSkills" :key="s" type="danger" effect="light" class="skill-tag" round>{{ s }}</el-tag>
              </div>
              <span v-else class="empty-text">无</span>
            </div>

            <div class="block">
              <div class="block-title">
                <span class="dot extra"></span>简历有但岗位没提的亮点（{{ current.parsed?.extraSkills?.length || 0 }}）
              </div>
              <div v-if="current.parsed?.extraSkills?.length" class="tag-cloud">
                <el-tag v-for="s in current.parsed.extraSkills" :key="s" type="info" effect="light" class="skill-tag" round>{{ s }}</el-tag>
              </div>
              <span v-else class="empty-text">无</span>
            </div>

            <el-row :gutter="14" class="block">
              <el-col :span="8">
                <div class="mini-card hl">
                  <div class="mc-head">✨ 亮点</div>
                  <ul class="mc-list" v-if="current.parsed?.highlights?.length">
                    <li v-for="(h, i) in current.parsed.highlights" :key="i">{{ h }}</li>
                  </ul>
                  <span v-else class="empty-text">-</span>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="mini-card iss">
                  <div class="mc-head">🔧 差距</div>
                  <ul class="mc-list" v-if="current.parsed?.gaps?.length">
                    <li v-for="(g, i) in current.parsed.gaps" :key="i">{{ g }}</li>
                  </ul>
                  <span v-else class="empty-text">-</span>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="mini-card sug">
                  <div class="mc-head">💡 建议</div>
                  <ul class="mc-list" v-if="current.parsed?.suggestions?.length">
                    <li v-for="(s, i) in current.parsed.suggestions" :key="i">{{ s }}</li>
                  </ul>
                  <span v-else class="empty-text">-</span>
                </div>
              </el-col>
            </el-row>

            <div v-if="current.errorMessage" class="err-box">
              <div class="block-title" style="color:#991B1B">❌ 失败原因</div>
              <p class="err-text">{{ current.errorMessage }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
.match-content {
  max-width: 1280px;
  margin: 0 auto;
  display: flex; flex-direction: column; gap: 18px;
}

/* Banner */
.banner {
  background: linear-gradient(120deg, #EEF2FF 0%, #F0F9FF 100%);
  border: 1px solid rgba(99,102,241,0.1);
  border-radius: 14px;
  padding: 16px 20px;
  display: flex; align-items: center; gap: 14px;
}
.banner-icon { font-size: 32px; }
.banner-text { flex: 1; }
.banner-title { font-size: 16px; font-weight: 600; color: var(--ink-900); }
.banner-sub { font-size: 13px; color: var(--ink-500); margin-top: 2px; }
.banner-stats { display: flex; gap: 8px; }
.bs-item { text-align: center; background: #fff; padding: 8px 16px; border-radius: 8px; }
.bs-value { font-size: 18px; font-weight: 700; color: var(--ink-900); }
.bs-label { font-size: 11px; color: var(--ink-500); margin-top: 2px; }

/* 主体两栏 */
.match-grid {
  display: grid;
  grid-template-columns: 1fr 1.3fr;
  gap: 18px;
  align-items: start;
}
@media (max-width: 1100px) { .match-grid { grid-template-columns: 1fr; } }

.panel { padding: 18px 20px; margin-bottom: 16px; }
.panel-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 14px;
}
.panel-title { font-size: 15px; font-weight: 600; color: var(--ink-900); }

.hint-box {
  background: rgba(99, 102, 241, 0.06);
  border-left: 3px solid var(--brand-500);
  border-radius: 6px;
  padding: 10px 14px;
  font-size: 12px;
  color: var(--ink-700);
  margin-top: 12px;
  line-height: 1.6;
}
.hint-emoji { margin-right: 6px; }

/* 右侧 */
.result-panel { min-height: 600px; }
.empty-result {
  text-align: center;
  padding: 80px 20px;
  color: var(--ink-500);
}
.er-icon { font-size: 64px; margin-bottom: 16px; opacity: 0.4; }
.er-title { font-size: 16px; font-weight: 600; color: var(--ink-700); }
.er-sub { font-size: 13px; margin-top: 8px; max-width: 320px; margin-left: auto; margin-right: auto; line-height: 1.6; }

/* 评分大圆环 */
.score-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0 24px;
  border-bottom: 1px dashed var(--ink-100);
  margin-bottom: 18px;
}
.score-ring { width: 160px; height: 160px; }
.level-tag { margin-top: 14px; font-size: 14px; padding: 6px 16px; }
.score-hint {
  margin-top: 10px;
  font-size: 14px;
  font-weight: 600;
}

.block { margin-top: 18px; }
.block-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-700);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.dot {
  display: inline-block;
  width: 8px; height: 8px;
  border-radius: 50%;
}
.dot.match { background: #10B981; }
.dot.missing { background: #EF4444; }
.dot.extra { background: #06B6D4; }

.tag-cloud { display: flex; flex-wrap: wrap; gap: 6px; }
.skill-tag { margin: 0 !important; }

.mini-card {
  padding: 14px 16px;
  border-radius: 10px;
  height: 100%;
  border: 1px solid;
}
.mini-card.hl { background: linear-gradient(135deg, #ECFDF5 0%, #F0FDF4 100%); border-color: #A7F3D0; }
.mini-card.iss { background: linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 100%); border-color: #FDE68A; }
.mini-card.sug { background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%); border-color: #BFDBFE; }

.mc-head { font-size: 13px; font-weight: 600; color: var(--ink-700); margin-bottom: 8px; }
.mc-list { margin: 0; padding-left: 18px; font-size: 12px; color: var(--ink-700); line-height: 1.7; }
.empty-text { font-size: 13px; color: var(--ink-500); }

.err-box {
  margin-top: 18px;
  background: linear-gradient(135deg, #FEF2F2 0%, #FEE2E2 100%);
  border-left: 3px solid #EF4444;
  border-radius: 6px;
  padding: 10px 14px;
}
.err-text { color: #991B1B; font-size: 13px; margin: 0; }

/* ============================================================
   移动端（≤768px）
   ============================================================ */
@media (max-width: 768px) {
  .match-content { gap: 12px; }

  .banner {
    flex-wrap: wrap;
    padding: 14px 14px;
    gap: 10px;
    border-radius: 12px;
  }
  .banner-icon { font-size: 24px; }
  .banner-text { flex: 1 1 100%; order: 2; }
  .banner-title { font-size: 15px; }
  .banner-sub { font-size: 12px; line-height: 1.5; }
  /* 历史评估数字：跟着文字换行，别和标题挤一行 */
  .banner-stats { flex: 1 1 100%; order: 3; }
  .bs-item { flex: 1; padding: 6px 12px; }

  .panel { padding: 14px 14px; margin-bottom: 12px; }
  .panel-title { font-size: 14px; }

  .result-panel { min-height: auto; }
  .empty-result { padding: 40px 16px; }
  .er-icon { font-size: 48px; margin-bottom: 12px; }
  .er-title { font-size: 15px; }
  .er-sub { font-size: 12px; }

  /* 评分圆环在手机上缩小一点，否则占掉半屏 */
  .score-block { padding: 12px 0 18px; }
  .score-ring { width: 130px; height: 130px; }
  .level-tag { font-size: 13px; margin-top: 12px; }
  .score-hint { font-size: 13px; }

  /* 亮点 / 差距 / 建议 是 el-col :span=8 三列，手机上强制单列 */
  .block :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }
  .block :deep(.el-col + .el-col) { margin-top: 10px; }

  .mini-card { padding: 12px 14px; }
  .mc-list { font-size: 12px; padding-left: 16px; }

  .block-title { font-size: 12px; }
  .hint-box { font-size: 12px; padding: 10px 12px; }

  /* "开始评估"按钮已经 style="width:100%"，这里只是补回触控高度 */
  .panel :deep(.el-form-item__label) { font-size: 13px; }
}
</style>
