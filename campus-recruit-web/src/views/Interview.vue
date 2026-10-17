<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  startInterview, listInterviews, getInterview,
  submitAnswer, deleteInterview, typeLabel, typeTagType
} from '../api/interview'
import { listJobs } from '../api/job'
import AppLayout from '../components/AppLayout.vue'

// 模拟面试页：
//   1) 选岗位 → 点「开始面试」→ 后端 AI 一次性出 5 道题（约 10~20 秒）；
//   2) 一题一题作答 + 评分（每次约 10~15 秒），最后一题答完自动生成总结；
//   3) 下方是历史面试，可查看/删除。

const route = useRoute()

const jobs = ref([])
const jobId = ref(null)
const starting = ref(false)
const current = ref(null)
const currentIndex = ref(0)
const answerDraft = ref('')
const submitting = ref(false)
const history = ref([])
const historyLoading = ref(false)

onMounted(async () => {
  await loadJobs()
  await loadHistory()
  const sid = route.query.sessionId
  if (sid) {
    try {
      const s = await getInterview(Number(sid))
      current.value = s
      const nextIdx = s.questions.findIndex(q => !s.answers.find(a => a.questionIndex === q.index))
      currentIndex.value = nextIdx === -1 ? 0 : nextIdx
    } catch {}
  }
})

async function loadJobs() {
  try { jobs.value = (await listJobs()).filter(j => j.status === 'PARSED') } catch {}
}
async function loadHistory() {
  historyLoading.value = true
  try { history.value = await listInterviews() } catch {}
  finally { historyLoading.value = false }
}

async function handleStart() {
  if (!jobId.value) { ElMessage.warning('请先选一个岗位'); return }
  starting.value = true
  try {
    current.value = await startInterview(jobId.value)
    currentIndex.value = 0
    answerDraft.value = ''
    ElMessage.success('题目已生成，开始作答吧！')
    await loadHistory()
  } catch {
  } finally {
    starting.value = false
  }
}

async function handleSubmitAnswer() {
  if (!answerDraft.value.trim()) { ElMessage.warning('请先填写答案'); return }
  submitting.value = true
  try {
    const updated = await submitAnswer(current.value.id, {
      questionIndex: currentIndex.value,
      answer: answerDraft.value
    })
    current.value = updated
    if (updated.status === 'FINISHED') {
      ElMessage.success(`全部题目作答完成，综合分 ${updated.totalScore}`)
      await loadHistory()
    } else {
      ElMessage.success(`本题得分：${currentQuestionScore.value}`)
      currentIndex.value = Math.min(currentIndex.value + 1, updated.questions.length - 1)
      answerDraft.value = ''
    }
  } catch {} finally {
    submitting.value = false
  }
}

async function viewInterview(row) {
  try {
    current.value = await getInterview(row.id)
    const nextIdx = current.value.questions.findIndex(
      q => !current.value.answers.find(a => a.questionIndex === q.index)
    )
    currentIndex.value = nextIdx === -1 ? 0 : nextIdx
    answerDraft.value = ''
  } catch {}
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('这场面试的所有作答都会被删除，确认？', '删除确认', { type: 'warning' })
  } catch { return }
  try {
    await deleteInterview(row.id)
    ElMessage.success('已删除')
    if (current.value?.id === row.id) current.value = null
    await loadHistory()
  } catch {}
}

function goPrev() { if (currentIndex.value > 0) currentIndex.value-- }
function goNext() { if (currentIndex.value < current.value.questions.length - 1) currentIndex.value++ }

const currentQ = computed(() => current.value?.questions?.[currentIndex.value] || null)
const currentAnswer = computed(() => {
  if (!current.value || !currentQ.value) return null
  return current.value.answers.find(a => a.questionIndex === currentQ.value.index)
})
const currentQuestionScore = computed(() => currentAnswer.value?.score ?? '-')

const answeredCount = computed(() => current.value?.answers?.length || 0)
const totalQuestions = computed(() => current.value?.questions?.length || 0)
const progress = computed(() => {
  if (!totalQuestions.value) return 0
  return Math.round((answeredCount.value / totalQuestions.value) * 100)
})
const isFinished = computed(() => current.value?.status === 'FINISHED')

function typeText(t) { return typeLabel(t) }
function typeColor(t) { return typeTagType(t) }
function jobLabel(j) { return `${j.title} · ${j.company || '-'} · ${j.city || '-'}` }
function fmt(t) { if (!t) return '-'; return String(t).replace('T', ' ').slice(0, 19) }

function scoreColor(s) {
  if (s >= 80) return '#10B981'
  if (s >= 60) return '#3B82F6'
  if (s >= 40) return '#F59E0B'
  return '#EF4444'
}
function levelClass(level) {
  if (level === '优秀') return 'lvl-excellent'
  if (level === '良好') return 'lvl-good'
  if (level === '一般') return 'lvl-okay'
  return 'lvl-poor'
}
</script>

<template>
  <AppLayout>
    <div class="iv-content">
      <!-- 顶部横幅 -->
      <section class="banner" style="background: linear-gradient(120deg, #FEF3C7 0%, #FFFBEB 100%);">
        <div class="banner-icon">🎤</div>
        <div class="banner-text">
          <div class="banner-title">AI 模拟面试</div>
          <div class="banner-sub">基于岗位标签出题，逐题作答、AI 单题评分，全部答完自动出综合总结与录用建议</div>
        </div>
        <div class="banner-stats">
          <div class="bs-item">
            <div class="bs-value">{{ history.length }}</div>
            <div class="bs-label">历史面试</div>
          </div>
        </div>
      </section>

      <div class="iv-grid">
        <!-- ========== 左：开始 + 历史 ========== -->
        <div>
          <div class="pretty-card panel">
            <div class="panel-head">
              <div class="panel-title">🚀 开始面试</div>
            </div>
            <el-select v-model="jobId" placeholder="选择目标岗位" style="width:100%" size="large">
              <el-option v-for="j in jobs" :key="j.id" :label="jobLabel(j)" :value="j.id" />
            </el-select>
            <el-button
              type="primary"
              size="large"
              class="btn-gradient"
              :loading="starting"
              @click="handleStart"
              style="width:100%; margin-top: 12px; padding: 14px; font-weight: 600;"
            >
              {{ starting ? 'AI 出题中…' : '🎯 开始这场面试' }}
            </el-button>
            <div class="hint-box">
              <span class="hint-emoji">💡</span>
              <span>一次出 5 道题（技术基础 / 项目深挖 / 算法 / 沟通 / 反问），全部缓存到后端，题目生成后保持稳定。</span>
            </div>
          </div>

          <div class="pretty-card panel">
            <div class="panel-head">
              <div class="panel-title">📚 历史面试（{{ history.length }}）</div>
            </div>
            <el-empty v-if="!history.length && !historyLoading" description="还没开始过面试" :image-size="70" />
            <el-table v-else :data="history" size="small" v-loading="historyLoading" style="width:100%">
              <el-table-column label="岗位" show-overflow-tooltip>
                <template #default="{ row }">{{ row.jobTitle }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === 'FINISHED' ? 'success' : 'warning'">
                    {{ row.status === 'FINISHED' ? '已结束' : '进行中' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="总分" width="60">
                <template #default="{ row }">
                  <strong :style="{color: scoreColor(row.totalScore || 0)}">{{ row.totalScore ?? '-' }}</strong>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="viewInterview(row)">继续</el-button>
                  <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- ========== 右：当前面试 ========== -->
        <div class="pretty-card panel result-panel">
          <div class="panel-head">
            <div class="panel-title">🎙️ 当前面试</div>
            <span v-if="current" class="job-tag">📌 {{ current.jobTitle }}</span>
          </div>

          <div v-if="!current" class="empty-result">
            <div class="er-icon">🎤</div>
            <div class="er-title">还没有开始面试</div>
            <div class="er-sub">左侧选择岗位，点「开始这场面试」，AI 会一次性给你出 5 道题</div>
          </div>

          <div v-else>
            <!-- 进度条 -->
            <div class="progress-block">
              <div class="progress-num">
                <span class="big">{{ answeredCount }}</span>
                <span class="sep">/</span>
                <span class="small">{{ totalQuestions }}</span>
              </div>
              <div class="progress-track">
                <div class="progress-fill" :style="{width: progress + '%'}"></div>
              </div>
              <div class="progress-label">{{ progress }}% · 已答完 {{ answeredCount }} / 共 {{ totalQuestions }} 题</div>
            </div>

            <!-- 当前题（非结束态） -->
            <div v-if="currentQ && !isFinished" class="q-card pretty-card">
              <div class="q-meta">
                <el-tag :type="typeColor(currentQ.type)" effect="dark" size="large" class="q-type-tag">
                  第 {{ currentIndex + 1 }} 题 · {{ typeText(currentQ.type) }}
                </el-tag>
              </div>
              <div class="q-content">{{ currentQ.content }}</div>

              <el-input
                v-model="answerDraft"
                type="textarea"
                :rows="6"
                placeholder="在这里作答（手打或粘贴都行，越具体越好）..."
                class="answer-input"
              />
              <div class="char-count">已写 {{ answerDraft.length }} 字</div>

              <div class="q-actions">
                <el-button :disabled="currentIndex === 0" @click="goPrev">← 上一题</el-button>
                <el-button
                  type="primary"
                  size="large"
                  class="btn-gradient"
                  :loading="submitting"
                  @click="handleSubmitAnswer"
                  style="padding: 10px 24px; font-weight: 600;"
                >
                  提交本题 · AI 评分
                </el-button>
                <el-button :disabled="currentIndex === totalQuestions - 1" @click="goNext">下一题 →</el-button>
              </div>
            </div>

            <!-- 单题反馈 -->
            <div v-if="currentQ && currentAnswer && !isFinished" class="feedback-block pretty-card">
              <div class="fb-head">
                <strong>本题得分</strong>
                <el-tag :type="currentAnswer.score >= 80 ? 'success' : (currentAnswer.score >= 60 ? 'primary' : (currentAnswer.score >= 40 ? 'warning' : 'danger'))" size="large" effect="dark">
                  {{ currentAnswer.score }} 分
                </el-tag>
              </div>
              <p class="fb-text">{{ currentAnswer.feedback }}</p>
              <div v-if="currentAnswer.userAnswer" class="ans-block">
                <div class="ans-label">你的回答</div>
                <p class="ans-text">{{ currentAnswer.userAnswer }}</p>
              </div>
            </div>

            <!-- 综合总结（结束态） -->
            <div v-if="isFinished && current.summary" class="summary-block pretty-card"
                 :class="levelClass(current.summary.level)">
              <div class="summary-head">
                <div class="summary-icon">🏆</div>
                <div class="summary-info">
                  <div class="summary-title">面试结束 · 综合评价</div>
                  <div class="summary-meta">基于本场 {{ totalQuestions }} 道题的 AI 点评</div>
                </div>
              </div>

              <div class="summary-score-row">
                <div class="score-cell">
                  <div class="cell-label">总分</div>
                  <div class="cell-value score-big" :style="{color: scoreColor(current.totalScore || 0)}">
                    {{ current.totalScore ?? current.summary.overallScore ?? '-' }}
                  </div>
                </div>
                <div class="score-cell">
                  <div class="cell-label">评级</div>
                  <div class="cell-value">
                    <el-tag size="large" effect="dark"
                            :type="(current.summary.level === '优秀') ? 'success' : (current.summary.level === '良好') ? 'primary' : (current.summary.level === '一般') ? 'warning' : 'danger'">
                      {{ current.summary.level }}
                    </el-tag>
                  </div>
                </div>
                <div class="score-cell">
                  <div class="cell-label">录用建议</div>
                  <div class="cell-value">
                    <el-tag size="large" effect="plain"
                            :type="(current.summary.recommendation || '').includes('不推荐') ? 'danger' :
                                   (current.summary.recommendation || '').includes('待定') ? 'warning' : 'success'">
                      {{ (current.summary.recommendation || '').split(/[（(]/)[0] }}
                    </el-tag>
                  </div>
                </div>
              </div>

              <div v-if="current.summary.summary" class="sum-row">
                <div class="sum-label">📝 综合点评</div>
                <p>{{ current.summary.summary }}</p>
              </div>

              <div class="sum-row">
                <div class="sum-label">✅ 整体亮点</div>
                <ul class="mc-list" v-if="current.summary.strengths?.length">
                  <li v-for="(s, i) in current.summary.strengths" :key="i">{{ s }}</li>
                </ul>
                <span v-else class="empty-text">-</span>
              </div>

              <div class="sum-row">
                <div class="sum-label">🔧 整体短板</div>
                <ul class="mc-list" v-if="current.summary.weaknesses?.length">
                  <li v-for="(w, i) in current.summary.weaknesses" :key="i">{{ w }}</li>
                </ul>
                <span v-else class="empty-text">-</span>
              </div>

              <div class="sum-row">
                <div class="sum-label">💼 录用建议</div>
                <p class="rec">{{ current.summary.recommendation }}</p>
              </div>
            </div>

            <!-- 所有题目展开 -->
            <el-collapse class="all-qs">
              <el-collapse-item title="查看所有题目与作答" name="all">
                <div v-for="(q, i) in current.questions" :key="i" class="all-q">
                  <div class="all-q-head">
                    <strong>Q{{ i + 1 }}. {{ typeText(q.type) }}</strong>
                    <el-tag v-if="current.answers.find(a => a.questionIndex === q.index)" size="small" type="success">
                      {{ current.answers.find(a => a.questionIndex === q.index).score }} 分
                    </el-tag>
                  </div>
                  <div class="all-q-content">{{ q.content }}</div>
                  <div v-if="current.answers.find(a => a.questionIndex === q.index)" class="all-q-feedback">
                    💬 {{ current.answers.find(a => a.questionIndex === q.index).feedback }}
                  </div>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
.iv-content {
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

.iv-grid {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 18px;
  align-items: start;
}
@media (max-width: 1100px) { .iv-grid { grid-template-columns: 1fr; } }

.panel { padding: 18px 20px; margin-bottom: 16px; }
.panel-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 14px;
}
.panel-title { font-size: 15px; font-weight: 600; color: var(--ink-900); }
.job-tag {
  font-size: 12px;
  color: var(--ink-500);
  background: var(--ink-50);
  padding: 4px 10px;
  border-radius: 999px;
}

.hint-box {
  background: rgba(99, 102, 241, 0.06);
  border-left: 3px solid var(--brand-500);
  border-radius: 6px;
  padding: 10px 14px;
  font-size: 12px;
  color: var(--ink-700);
  margin-top: 14px;
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

/* 进度条 */
.progress-block {
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto;
  gap: 6px 14px;
  align-items: center;
  margin-bottom: 20px;
}
.progress-num { display: flex; align-items: baseline; gap: 4px; }
.progress-num .big { font-size: 30px; font-weight: 700; color: var(--brand-600); }
.progress-num .sep { font-size: 20px; color: var(--ink-300); margin: 0 2px; }
.progress-num .small { font-size: 14px; color: var(--ink-500); }
.progress-track {
  height: 8px;
  background: var(--ink-100);
  border-radius: 999px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #6366F1 0%, #8B5CF6 50%, #06B6D4 100%);
  border-radius: 999px;
  transition: width 0.5s ease;
}
.progress-label {
  grid-column: 1 / 3;
  font-size: 12px;
  color: var(--ink-500);
}

/* 题目卡 */
.q-card {
  padding: 18px 20px;
  border-left: 4px solid var(--brand-500);
  background: linear-gradient(135deg, #FAFBFF 0%, #F9FAFB 100%);
}
.q-meta { margin-bottom: 10px; }
.q-type-tag { padding: 6px 14px; font-size: 13px; }
.q-content {
  font-size: 16px;
  color: var(--ink-900);
  line-height: 1.75;
  margin-bottom: 14px;
  white-space: pre-wrap;
}
.answer-input :deep(textarea) {
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.7;
  padding: 12px 14px;
  background: #fff;
}
.char-count { font-size: 12px; color: var(--ink-500); margin-top: 6px; text-align: right; }
.q-actions { margin-top: 14px; display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }

/* 单题反馈 */
.feedback-block {
  margin-top: 14px;
  padding: 14px 18px;
  border-left: 4px solid #10B981;
  background: linear-gradient(135deg, #ECFDF5 0%, #F0FDF4 100%);
}
.fb-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 8px;
}
.fb-head strong { font-size: 13px; color: var(--ink-700); }
.fb-text { margin: 0; font-size: 14px; color: var(--ink-700); line-height: 1.7; white-space: pre-wrap; }
.ans-block { margin-top: 10px; padding-top: 10px; border-top: 1px dashed #A7F3D0; }
.ans-label { font-size: 11px; color: var(--ink-500); margin-bottom: 4px; }
.ans-text { font-size: 13px; color: var(--ink-500); margin: 0; line-height: 1.6; white-space: pre-wrap; }

/* 综合总结 */
.summary-block {
  margin-top: 14px;
  padding: 20px 22px;
  border-left: 6px solid var(--brand-500);
  background: linear-gradient(135deg, #FFFFFF 0%, #FAFBFF 100%);
}
.summary-block.lvl-excellent { border-left-color: #10B981; }
.summary-block.lvl-good { border-left-color: #3B82F6; }
.summary-block.lvl-okay { border-left-color: #F59E0B; }
.summary-block.lvl-poor { border-left-color: #EF4444; }

.summary-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.summary-icon { font-size: 28px; }
.summary-title { font-size: 16px; font-weight: 700; color: var(--ink-900); }
.summary-meta { font-size: 12px; color: var(--ink-500); margin-top: 2px; }

.summary-score-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1.4fr;
  gap: 10px;
  background: var(--ink-50);
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 16px;
}
.score-cell { text-align: center; }
.cell-label { font-size: 11px; color: var(--ink-500); margin-bottom: 6px; }
.cell-value { font-size: 16px; font-weight: 600; }
.score-big { font-size: 36px; font-weight: 700; }

.sum-row { margin-top: 12px; }
.sum-label { font-size: 13px; font-weight: 600; color: var(--ink-700); margin-bottom: 6px; }
.sum-row p { margin: 0; font-size: 14px; line-height: 1.7; white-space: pre-wrap; }

.mc-list { margin: 0; padding-left: 18px; font-size: 13px; color: var(--ink-700); line-height: 1.75; }
.rec {
  font-size: 14px;
  color: var(--ink-900);
  font-weight: 600;
  background: rgba(245, 158, 11, 0.1);
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 3px solid #F59E0B;
  margin: 0;
}

/* 全部题目 */
.all-qs { margin-top: 18px; }
.all-q {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--ink-100);
}
.all-q-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}
.all-q-content { font-size: 13px; color: var(--ink-700); margin-top: 4px; line-height: 1.6; white-space: pre-wrap; }
.all-q-feedback { font-size: 12px; color: var(--ink-500); margin-top: 6px; line-height: 1.6; }

.empty-text { font-size: 13px; color: var(--ink-500); }

/* ============================================================
   移动端（≤768px）
   这一页手机上最要命的是作答区：题目 + 输入框 + 三个操作按钮。
   关键点：按钮必须能一行放下或者干脆竖排，否则"提交本题"会被挤出屏幕。
   ============================================================ */
@media (max-width: 768px) {
  .iv-content { gap: 12px; }

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
  .banner-stats { flex: 1 1 100%; order: 3; }
  .bs-item { flex: 1; padding: 6px 12px; }

  .panel { padding: 14px 14px; margin-bottom: 12px; }
  .panel-title { font-size: 14px; }
  .job-tag { font-size: 11px; padding: 3px 8px; }

  .result-panel { min-height: auto; }
  .empty-result { padding: 40px 16px; }
  .er-icon { font-size: 48px; margin-bottom: 12px; }
  .er-title { font-size: 15px; }
  .er-sub { font-size: 12px; }

  /* 进度条：大数字缩小，别抢题目位置 */
  .progress-block {
    grid-template-columns: auto 1fr;
    gap: 4px 10px;
    margin-bottom: 14px;
  }
  .progress-num .big { font-size: 24px; }
  .progress-num .sep { font-size: 16px; }
  .progress-num .small { font-size: 13px; }

  /* 题目卡 */
  .q-card { padding: 14px 14px; border-left-width: 3px; }
  .q-type-tag { padding: 4px 10px; font-size: 12px; }
  .q-content { font-size: 15px; line-height: 1.7; margin-bottom: 12px; }
  .answer-input :deep(textarea) { font-size: 15px; padding: 10px 12px; }

  /* 三个按钮（上一题 / 提交 / 下一题）：改成两行
     主按钮占满一整行、上下题各占一半，手指不容易点错 */
  .q-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }
  .q-actions .el-button {
    width: 100%;
    margin-left: 0 !important;
  }
  /* 中间那个"提交本题 · AI 评分"跨两列 */
  .q-actions .el-button:nth-child(2) {
    grid-column: 1 / -1;
    order: -1;
  }

  .feedback-block { padding: 12px 14px; border-left-width: 3px; }
  .fb-text { font-size: 13px; }
  .ans-text { font-size: 12px; }

  /* 综合总结 */
  .summary-block { padding: 16px 14px; border-left-width: 4px; }
  .summary-icon { font-size: 22px; }
  .summary-title { font-size: 15px; }
  .summary-meta { font-size: 11px; }

  /* 总分 / 评级 / 录用建议 三列 → 一行三个太窄，改成单列堆叠 */
  .summary-score-row {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 12px;
  }
  .score-cell { display: flex; align-items: center; justify-content: space-between; }
  .cell-label { margin-bottom: 0; font-size: 12px; }
  .score-big { font-size: 28px; }
  .cell-value { font-size: 14px; }

  .sum-label { font-size: 12px; }
  .sum-row p { font-size: 13px; }
  .mc-list { font-size: 12px; padding-left: 16px; }
  .rec { font-size: 13px; padding: 8px 10px; }

  .all-q-head { font-size: 13px; }
  .all-q-content { font-size: 12px; }
  .hint-box { font-size: 12px; padding: 10px 12px; }
}
</style>
