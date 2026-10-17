<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { parseJob, listJobs, deleteJob, getJob } from '../api/job'
import AppLayout from '../components/AppLayout.vue'

// 岗位打标签页。
//
// 人话讲解（数据怎么流动的）：
//   你从招聘网站复制一整段 JD 粘进文本框 → 点"AI 解析并入库"
//   → 前端 POST /api/jobs → 后端存原文、发给 DeepSeek 打标签 → 返回结构化字段
//   → 前端渲染成"必备技能/加分技能/注意事项"等卡片。
const rawText = ref('')
const loading = ref(false)
const parsed = ref(null)
const currentId = ref(null)
const jobs = ref([])
const listLoading = ref(false)

// 一键填入的示例 JD（演示用；故意选泰州的实习岗，和第 3 步"城市筛选"呼应）
const SAMPLE = `【Java 后端开发实习生】— 泰州某科技有限公司

岗位职责：
1. 参与公司 SaaS 产品后端接口的开发与维护，使用 Spring Boot + MyBatis 完成业务模块；
2. 配合产品与前端完成需求对接、接口联调与自测；
3. 参与数据库表设计与 SQL 优化，协助排查线上问题；
4. 编写必要的接口文档与单元测试。

任职要求：
1. 本科及以上学历，计算机相关专业，2027 届优先，可实习 3 个月以上；
2. 熟悉 Java 基础，掌握集合、IO、多线程等常用知识；
3. 熟悉 Spring Boot、MyBatis-Plus，了解 MySQL 常用 SQL 与索引；
4. 了解 Redis、消息队列者优先；
5. 有个人项目或实习经历者优先；熟悉 Vue 等前端技术者优先。

其他说明：
- 实习薪资 150-200 元/天，提供免费午餐；
- 工作地点：江苏省泰州市医药高新区；
- 表现优秀者可转正。`

onMounted(loadList)

async function loadList() {
  listLoading.value = true
  try {
    jobs.value = await listJobs()
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    listLoading.value = false
  }
}

function fillSample() {
  rawText.value = SAMPLE
  ElMessage.success('已填入示例 JD，点击下方按钮开始 AI 打标签')
}

async function handleParse() {
  const text = rawText.value.trim()
  if (text.length < 30) {
    ElMessage.warning('请粘贴完整的岗位 JD（至少 30 个字符）')
    return
  }
  loading.value = true
  parsed.value = null
  currentId.value = null
  try {
    const res = await parseJob({ rawText: text })
    parsed.value = res.parsed
    currentId.value = res.id
    ElMessage.success('AI 打标签完成！')
    await loadList()
  } catch {
    // 失败原因（重复提交 / 未配 Key / 余额不足）由拦截器弹出提示
  } finally {
    loading.value = false
  }
}

async function viewJob(row) {
  try {
    const res = await getJob(row.id)
    parsed.value = res.parsed
    currentId.value = res.id
    ElMessage.info('已加载该岗位的标签')
  } catch {}
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」这个岗位吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return // 用户点了取消
  }
  try {
    await deleteJob(row.id)
    ElMessage.success('已删除')
    if (currentId.value === row.id) {
      parsed.value = null
      currentId.value = null
    }
    await loadList()
  } catch {}
}

function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

const stepIndex = computed(() => {
  if (parsed.value) return 3
  if (loading.value) return 2
  if (rawText.value.length >= 30) return 1
  return 0
})
</script>

<template>
  <AppLayout>
    <div class="job-content">
      <!-- 顶部横幅 -->
      <section class="banner" style="background: linear-gradient(120deg, #F3E8FF 0%, #ECFEFF 100%);">
        <div class="banner-icon">💼</div>
        <div class="banner-text">
          <div class="banner-title">岗位智能打标签</div>
          <div class="banner-sub">从招聘页复制整段 JD，AI 帮你整理出岗位名/公司/城市/薪资/必备技能/坑点</div>
        </div>
        <div class="banner-steps">
          <div class="step" :class="{ on: stepIndex >= 0, done: stepIndex > 0 }">① 粘贴 JD</div>
          <div class="step" :class="{ on: stepIndex >= 1, done: stepIndex > 1 }">② AI 打标签</div>
          <div class="step" :class="{ on: stepIndex >= 2, done: stepIndex > 2 }">③ 结构化展示</div>
        </div>
      </section>

      <div class="job-grid">
        <!-- ========== 左：输入区 + 岗位列表 ========== -->
        <div>
          <div class="pretty-card panel">
            <div class="panel-head">
              <div class="panel-title">📋 粘贴岗位 JD</div>
              <el-button link type="primary" @click="fillSample">⚡ 一键填入示例</el-button>
            </div>

            <el-input
              v-model="rawText"
              type="textarea"
              :rows="14"
              resize="vertical"
              placeholder="把岗位 JD 全文粘贴到这里（招聘页面上从岗位名到任职要求整段复制）..."
              class="resume-textarea"
            />
            <div class="counter">已输入 {{ rawText.length }} 字符</div>

            <el-button
              type="primary"
              size="large"
              class="btn-gradient"
              :loading="loading"
              @click="handleParse"
              style="padding: 12px 24px; font-weight: 600;"
            >
              {{ loading ? 'AI 打标签中…请稍候' : '🚀 AI 解析并入库' }}
            </el-button>

            <div class="hint-box">
              <span class="hint-emoji">🤖</span>
              <span><b>为什么不爬取招聘网站？</b>爬虫涉及合规风险（违反网站条款、被反爬、数据版权），本项目用「用户自行粘贴」，既合规又避免数据版权问题。</span>
            </div>
          </div>

          <div class="pretty-card panel">
            <div class="panel-head">
              <div class="panel-title">📚 我录入的岗位（{{ jobs.length }}）</div>
            </div>
            <el-empty v-if="!jobs.length && !listLoading" description="还没有岗位，先粘贴一份 JD 吧" :image-size="70" />
            <el-table v-else :data="jobs" size="small" v-loading="listLoading" style="width: 100%">
              <el-table-column prop="title" label="岗位" show-overflow-tooltip />
              <el-table-column prop="company" label="公司" show-overflow-tooltip />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === 'PARSED' ? 'success' : (row.status === 'FAILED' ? 'danger' : 'info')">
                    {{ row.status === 'PARSED' ? '成功' : (row.status === 'FAILED' ? '失败' : '解析中') }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="110">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="viewJob(row)">查看</el-button>
                  <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- ========== 右：标签结果 ========== -->
        <div class="pretty-card panel result-panel">
          <div class="panel-head">
            <div class="panel-title">🎯 AI 岗位标签</div>
          </div>

          <div v-if="!parsed" class="empty-result">
            <div class="er-icon">📭</div>
            <div class="er-title">还没有结果哦</div>
            <div class="er-sub">左侧提交 JD 后，AI 会把岗位名/公司/城市/薪资/必备技能等结构化数据展示在这里</div>
          </div>

          <template v-else>
            <div class="job-hero">
              <div class="job-hero-icon">💼</div>
              <div class="job-hero-body">
                <div class="job-hero-title">{{ parsed.title || '未识别岗位名' }}</div>
                <div class="job-hero-meta">
                  <span v-if="parsed.company" class="meta-item">🏢 {{ parsed.company }}</span>
                  <span v-if="parsed.city" class="meta-item">📍 {{ parsed.city }}</span>
                  <span v-if="parsed.salary" class="meta-item">💰 {{ parsed.salary }}</span>
                </div>
              </div>
              <el-tag :type="parsed.jobType === '实习' ? 'success' : 'info'" effect="dark">
                {{ parsed.jobType || '类型未知' }}
              </el-tag>
            </div>

            <div class="info-grid">
              <div class="info-item">
                <span class="ii-label">学历要求</span>
                <span class="ii-value">{{ parsed.educationRequirement || '不限' }}</span>
              </div>
              <div class="info-item">
                <span class="ii-label">面向届别</span>
                <span class="ii-value">
                  <template v-if="parsed.graduationYears && parsed.graduationYears.length">
                    <el-tag v-for="y in parsed.graduationYears" :key="y" size="small" class="skill-tag" effect="plain">{{ y }} 届</el-tag>
                  </template>
                  <span v-else>JD 未提及</span>
                </span>
              </div>
            </div>

            <div class="block" v-if="parsed.summary">
              <div class="block-title">📌 一句话概括</div>
              <div class="summary-box">{{ parsed.summary }}</div>
            </div>

            <!-- 必备技能（硬性条件，权重最高） -->
            <div class="block">
              <div class="block-title required">🔴 必备技能</div>
              <div v-if="parsed.requiredSkills && parsed.requiredSkills.length" class="tag-cloud">
                <el-tag v-for="s in parsed.requiredSkills" :key="s" class="skill-tag required-tag" effect="dark" round>{{ s }}</el-tag>
              </div>
              <span v-else class="empty-text">未识别到必备技能</span>
            </div>

            <!-- 加分技能（有更好，权重低） -->
            <div class="block">
              <div class="block-title preferred">🟡 加分技能</div>
              <div v-if="parsed.preferredSkills && parsed.preferredSkills.length" class="tag-cloud">
                <el-tag v-for="s in parsed.preferredSkills" :key="s" class="skill-tag preferred-tag" effect="light" round>{{ s }}</el-tag>
              </div>
              <span v-else class="empty-text">JD 未提及加分项</span>
            </div>

            <!-- 注意事项：AI 帮应届生避坑 -->
            <div class="block" v-if="parsed.warnings && parsed.warnings.length">
              <div class="mini-card warn">
                <div class="mc-head">⚠️ 投递前注意</div>
                <ul class="mc-list">
                  <li v-for="(w, i) in parsed.warnings" :key="i">{{ w }}</li>
                </ul>
              </div>
            </div>

            <div class="block" v-if="parsed.responsibilities && parsed.responsibilities.length">
              <div class="block-title">📝 岗位职责（{{ parsed.responsibilities.length }}）</div>
              <ul class="mc-list">
                <li v-for="(r, i) in parsed.responsibilities" :key="i">{{ r }}</li>
              </ul>
            </div>

            <div class="block" v-if="parsed.keywords && parsed.keywords.length">
              <div class="block-title">🏷️ 关键词</div>
              <div class="tag-cloud">
                <el-tag v-for="k in parsed.keywords" :key="k" class="skill-tag" type="info" effect="plain" size="small" round>{{ k }}</el-tag>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
.job-content {
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
.banner-title { font-size: 16px; font-weight: 600; color: var(--ink-900); }
.banner-sub { font-size: 13px; color: var(--ink-500); margin-top: 2px; }
.banner-text { flex: 1; }
.banner-steps { display: flex; gap: 8px; }
.step {
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  background: #fff;
  color: var(--ink-300);
  border: 1px solid var(--ink-100);
}
.step.on { color: var(--brand-600); border-color: var(--brand-200); }
.step.done { color: #fff; background: var(--brand-500); border-color: var(--brand-500); }

/* 主体两栏 */
.job-grid {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 18px;
  align-items: start;
}
@media (max-width: 1100px) {
  .job-grid { grid-template-columns: 1fr; }
}

.panel { padding: 18px 20px; margin-bottom: 16px; }
.panel-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 14px;
}
.panel-title { font-size: 15px; font-weight: 600; color: var(--ink-900); }

.resume-textarea :deep(textarea) {
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.6;
  padding: 12px 14px;
  background: var(--ink-50);
  border-color: var(--ink-100);
}
.resume-textarea :deep(textarea:focus) { background: #fff; }

.counter { font-size: 12px; color: var(--ink-500); margin: 8px 0 14px; text-align: right; }

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

/* 右侧结果 */
.result-panel { min-height: 600px; }
.empty-result {
  text-align: center;
  padding: 80px 20px;
  color: var(--ink-500);
}
.er-icon { font-size: 64px; margin-bottom: 16px; opacity: 0.4; }
.er-title { font-size: 16px; font-weight: 600; color: var(--ink-700); }
.er-sub { font-size: 13px; margin-top: 8px; max-width: 320px; margin-left: auto; margin-right: auto; line-height: 1.6; }

.job-hero {
  display: flex;
  align-items: center;
  gap: 14px;
  background: linear-gradient(120deg, #F9FAFB 0%, #F3F4F6 100%);
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 14px;
}
.job-hero-icon {
  width: 52px; height: 52px;
  border-radius: 12px;
  background: var(--grad-primary);
  display: flex; align-items: center; justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}
.job-hero-body { flex: 1; min-width: 0; }
.job-hero-title { font-size: 18px; font-weight: 700; color: var(--ink-900); margin-bottom: 6px; }
.job-hero-meta { display: flex; flex-wrap: wrap; gap: 12px; }
.meta-item { font-size: 12px; color: var(--ink-500); }

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  background: var(--ink-50);
  border-radius: 12px;
  padding: 14px;
}
.info-item { display: flex; flex-direction: column; gap: 4px; }
.ii-label { font-size: 11px; color: var(--ink-500); }
.ii-value { font-size: 14px; font-weight: 600; color: var(--ink-900); }

.summary-box {
  background: linear-gradient(135deg, #FEF3C7 0%, #FFFBEB 100%);
  border-left: 3px solid var(--warn-500);
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--ink-700);
  line-height: 1.6;
}

.block { margin-top: 18px; }
.block-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-700);
  margin-bottom: 10px;
  display: flex; align-items: center; gap: 4px;
}
.block-title.required { color: #DC2626; }
.block-title.preferred { color: #D97706; }

.tag-cloud { display: flex; flex-wrap: wrap; gap: 6px; }
.skill-tag { margin: 0 !important; }
.required-tag { background: #EF4444 !important; border-color: #EF4444 !important; color: #fff !important; }
.preferred-tag { background: #FEF3C7 !important; border-color: #FDE68A !important; color: #92400E !important; }

.mini-card {
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid;
}
.mini-card.warn {
  background: linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 100%);
  border-color: #FDE68A;
}
.mc-head { font-size: 13px; font-weight: 600; color: #92400E; margin-bottom: 8px; }
.mc-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: var(--ink-700);
  line-height: 1.75;
}
.empty-text { font-size: 13px; color: var(--ink-500); }

/* ============================================================
   移动端（≤768px）
   ============================================================ */
@media (max-width: 768px) {
  .job-content { gap: 12px; }

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
  .banner-steps {
    flex: 1 1 100%;
    order: 3;
    flex-wrap: wrap;
    gap: 6px;
  }
  .step { font-size: 11px; padding: 3px 10px; }

  .panel { padding: 14px 14px; margin-bottom: 12px; }
  .panel-title { font-size: 14px; }

  .result-panel { min-height: auto; }
  .empty-result { padding: 40px 16px; }
  .er-icon { font-size: 48px; margin-bottom: 12px; }
  .er-title { font-size: 15px; }
  .er-sub { font-size: 12px; }

  /* 岗位头部：图标 + 标题 + 类型标签，窄屏改成竖向 */
  .job-hero {
    flex-wrap: wrap;
    padding: 12px 14px;
    gap: 10px;
    border-radius: 10px;
  }
  .job-hero-icon { width: 42px; height: 42px; font-size: 20px; border-radius: 10px; }
  .job-hero-title { font-size: 16px; }
  .job-hero-meta { gap: 8px; }
  .meta-item { font-size: 11px; }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 8px;
    padding: 12px;
  }
  .ii-value { font-size: 13px; }

  .summary-box { font-size: 12px; padding: 10px 12px; }

  .resume-textarea :deep(textarea) { font-size: 14px; }
  .hint-box { font-size: 12px; padding: 10px 12px; }

  /* 结果面板里的按钮（如果有）撑满宽 */
  .panel > .el-button { width: 100%; margin-left: 0; }
}
</style>
