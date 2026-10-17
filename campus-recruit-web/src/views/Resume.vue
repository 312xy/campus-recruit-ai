<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { parseResume, uploadResume, listResumes, deleteResume, getResume } from '../api/resume'
import AppLayout from '../components/AppLayout.vue'

// 简历解析页。
//
// 人话讲解（数据怎么流动的）：
//   你在文本框里粘贴简历 → 点"开始 AI 解析" → 前端发 POST /api/resumes/parse
//   → 后端存原文并发给 DeepSeek → 大模型返回 JSON → 后端存库并返回
//   → 前端把 parsed 里的字段渲染成一堆漂亮卡片。
const rawText = ref('')
const fileName = ref('')
const loading = ref(false)
const parsed = ref(null)
const currentId = ref(null)
const resumes = ref([])
const listLoading = ref(false)

// 一键填入的示例简历（演示用）
const SAMPLE = `张三 | 男 | 138-0000-0000 | zhangsan@example.com

教育背景
2022.09 - 2026.06  XX大学  软件工程  本科
主修课程：数据结构、操作系统、计算机网络、数据库原理；GPA 3.6/4.0（专业前 15%）

专业技能
- 熟悉 Java 基础，掌握集合、IO、多线程、JVM 内存模型；
- 熟悉 Spring Boot、MyBatis-Plus 开发 Web 后端接口，了解 Spring Security + JWT 鉴权；
- 熟悉 MySQL 常用 SQL 与索引优化，了解 Redis 缓存；
- 熟悉 Vue 3 + Element Plus 前端开发，能独立完成前后端联调；
- 了解 Git、Maven、Linux 常用命令。

实习经历
2025.07 - 2025.09  某科技有限公司  后端开发实习生
- 参与公司内部工单系统的接口开发，使用 Spring Boot + MyBatis 完成 8 个 CRUD 接口；
- 优化了一个慢查询接口，通过添加联合索引把响应时间从 1.2s 降到 200ms；
- 使用 Git 参与团队协作，平均每周提交 10+ 次代码。

项目经历
校园二手交易平台（个人项目）
- 技术栈：Spring Boot + MyBatis-Plus + MySQL + Vue 3 + Element Plus
- 负责后端全部接口设计与开发，实现用户、商品、订单模块，使用 JWT 完成登录鉴权；
- 使用 Redis 对商品列表做缓存，压测下单接口 QPS 从 300 提升到 800。

AI 校招助手（个人项目）
- 技术栈：Spring Boot 3 + MyBatis-Plus + H2/MySQL + Vue 3 + DeepSeek 大模型 API
- 独立完成简历解析、岗位匹配评分、模拟面试等模块；
- 通过提示词工程 + JSON 模式让大模型稳定输出结构化数据，并用 Jackson 反序列化校验；
- 会员订阅使用数据库模拟支付，设计了订单状态机与幂等处理。

自我评价
学习能力较强，愿意钻研底层原理，能承受一定强度的学习节奏。`

onMounted(loadList)

async function loadList() {
  listLoading.value = true
  try {
    resumes.value = await listResumes()
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    listLoading.value = false
  }
}

function fillSample() {
  rawText.value = SAMPLE
  fileName.value = '示例简历.txt'
  ElMessage.success('已填入示例简历，点击下方按钮开始解析')
}

async function handleParse() {
  const text = rawText.value.trim()
  if (text.length < 20) {
    ElMessage.warning('请粘贴完整简历内容（至少 20 个字符）')
    return
  }
  loading.value = true
  parsed.value = null
  currentId.value = null
  try {
    const res = await parseResume({ fileName: fileName.value || '手动粘贴', rawText: text })
    parsed.value = res.parsed
    currentId.value = res.id
    ElMessage.success('AI 解析完成！')
    await loadList()
  } catch {
    // 失败原因（比如没配 API Key、余额不足）由拦截器弹出提示
  } finally {
    loading.value = false
  }
}

// el-upload 的 http-request：接管上传动作，用我们自己的 axios（这样才会自动带 token）
async function handleUpload(option) {
  const file = option.file
  loading.value = true
  parsed.value = null
  try {
    const res = await uploadResume(file)
    parsed.value = res.parsed
    currentId.value = res.id
    ElMessage.success('文件解析完成！')
    await loadList()
  } catch {
    // 同上，统一由拦截器提示
  } finally {
    loading.value = false
  }
}

async function viewResume(row) {
  try {
    const res = await getResume(row.id)
    parsed.value = res.parsed
    currentId.value = res.id
    ElMessage.info('已加载该简历的解析结果')
  } catch {}
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.fileName}」这份简历吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return // 用户点了取消
  }
  try {
    await deleteResume(row.id)
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

// 解析状态条（步骤指示）
const stepIndex = computed(() => {
  if (parsed.value) return 3
  if (loading.value) return 2
  if (rawText.value.length >= 20) return 1
  return 0
})
</script>

<template>
  <AppLayout>
    <div class="resume-content">
      <!-- 顶部横幅 -->
      <section class="banner">
        <div class="banner-icon">📄</div>
        <div class="banner-text">
          <div class="banner-title">智能简历解析</div>
          <div class="banner-sub">把简历粘进来，AI 帮你拆出技能、项目、亮点与待改进项</div>
        </div>
        <div class="banner-steps">
          <div class="step" :class="{ on: stepIndex >= 0, done: stepIndex > 0 }">① 输入</div>
          <div class="step" :class="{ on: stepIndex >= 1, done: stepIndex > 1 }">② 调用 AI</div>
          <div class="step" :class="{ on: stepIndex >= 2, done: stepIndex > 2 }">③ 结构化</div>
        </div>
      </section>

      <div class="resume-grid">
        <!-- ========== 左：输入区 ========== -->
        <div>
          <div class="pretty-card panel">
            <div class="panel-head">
              <div class="panel-title">📝 输入简历内容</div>
              <el-button link type="primary" @click="fillSample">⚡ 一键填入示例</el-button>
            </div>

            <el-input
              v-model="rawText"
              type="textarea"
              :rows="14"
              resize="vertical"
              placeholder="把简历全文粘贴到这里（PDF/Word 里全选复制即可）..."
              class="resume-textarea"
            />
            <div class="counter">已输入 {{ rawText.length }} 字符</div>

            <div class="actions">
              <el-button
                type="primary"
                size="large"
                class="btn-gradient"
                :loading="loading"
                @click="handleParse"
              >
                {{ loading ? 'AI 解析中…请稍候' : '🚀 开始 AI 解析' }}
              </el-button>
              <el-upload
                :show-file-list="false"
                :http-request="handleUpload"
                accept=".txt,.md"
                :disabled="loading"
              >
                <el-button size="large" plain :disabled="loading">📤 上传 .txt 文件</el-button>
              </el-upload>
            </div>

            <div class="hint-box">
              <span class="hint-emoji">💡</span>
              <span>面试演示时直接点「一键填入示例」+「开始 AI 解析」两下就能稳定复现效果。</span>
            </div>
          </div>

          <div class="pretty-card panel">
            <div class="panel-head">
              <div class="panel-title">📚 我的简历（{{ resumes.length }}）</div>
            </div>
            <el-empty v-if="!resumes.length && !listLoading" description="还没有简历，先解析一份吧" :image-size="70" />
            <el-table v-else :data="resumes" size="small" v-loading="listLoading" style="width: 100%">
              <el-table-column prop="fileName" label="文件名" show-overflow-tooltip />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === 'PARSED' ? 'success' : (row.status === 'FAILED' ? 'danger' : 'info')">
                    {{ row.status === 'PARSED' ? '成功' : (row.status === 'FAILED' ? '失败' : '解析中') }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="时间" width="150">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="110">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="viewResume(row)">查看</el-button>
                  <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- ========== 右：解析结果 ========== -->
        <div class="pretty-card panel result-panel">
          <div class="panel-head">
            <div class="panel-title">🎯 AI 解析结果</div>
          </div>

          <div v-if="!parsed" class="empty-result">
            <div class="er-icon">📭</div>
            <div class="er-title">还没有结果哦</div>
            <div class="er-sub">左侧提交简历后，AI 会把姓名/技能/亮点/项目等结构化数据展示在这里</div>
          </div>

          <template v-else>
            <div class="info-grid">
              <div class="info-item"><span class="ii-label">姓名</span><span class="ii-value">{{ parsed.name || '未识别' }}</span></div>
              <div class="info-item"><span class="ii-label">学历</span><span class="ii-value">{{ parsed.degree || '未识别' }}</span></div>
              <div class="info-item"><span class="ii-label">学校</span><span class="ii-value">{{ parsed.school || '未识别' }}</span></div>
              <div class="info-item"><span class="ii-label">专业</span><span class="ii-value">{{ parsed.major || '未识别' }}</span></div>
              <div class="info-item"><span class="ii-label">毕业年份</span><span class="ii-value">{{ parsed.graduationYear || '未识别' }}</span></div>
              <div class="info-item"><span class="ii-label">联系方式</span><span class="ii-value">{{ parsed.phone || parsed.email || '未识别' }}</span></div>
            </div>

            <div class="block">
              <div class="block-title">🛠️ 技能关键词</div>
              <div v-if="parsed.skills && parsed.skills.length" class="tag-cloud">
                <el-tag v-for="s in parsed.skills" :key="s" class="skill-tag" effect="light" round>{{ s }}</el-tag>
              </div>
              <span v-else class="empty-text">未识别到技能</span>
            </div>

            <el-row :gutter="14" class="block">
              <el-col :span="12">
                <div class="mini-card hl">
                  <div class="mc-head">✨ 简历亮点</div>
                  <ul v-if="parsed.highlights && parsed.highlights.length" class="mc-list">
                    <li v-for="(h, i) in parsed.highlights" :key="i">{{ h }}</li>
                  </ul>
                  <span v-else class="empty-text">-</span>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="mini-card iss">
                  <div class="mc-head">🔧 待改进项</div>
                  <ul v-if="parsed.issues && parsed.issues.length" class="mc-list">
                    <li v-for="(it, i) in parsed.issues" :key="i">{{ it }}</li>
                  </ul>
                  <span v-else class="empty-text">-</span>
                </div>
              </el-col>
            </el-row>

            <div class="block" v-if="parsed.projects && parsed.projects.length">
              <div class="block-title">💼 项目经历（{{ parsed.projects.length }}）</div>
              <div v-for="(p, i) in parsed.projects" :key="i" class="item-card pretty-card">
                <div class="item-head">
                  <strong>{{ p.name }}</strong>
                  <span class="item-role">{{ p.role }}</span>
                </div>
                <div class="tech-row" v-if="p.technologies && p.technologies.length">
                  <el-tag v-for="t in p.technologies" :key="t" size="small" type="info" class="skill-tag" round>{{ t }}</el-tag>
                </div>
                <p class="item-desc">{{ p.description }}</p>
              </div>
            </div>

            <div class="block" v-if="parsed.internships && parsed.internships.length">
              <div class="block-title">🏢 实习经历（{{ parsed.internships.length }}）</div>
              <div v-for="(it, i) in parsed.internships" :key="i" class="item-card pretty-card">
                <div class="item-head">
                  <strong>{{ it.company }}</strong>
                  <span class="item-role">{{ it.position }}</span>
                </div>
                <div class="item-time">{{ it.startDate }} ~ {{ it.endDate }}</div>
                <p class="item-desc">{{ it.description }}</p>
              </div>
            </div>

            <div class="block" v-if="parsed.educations && parsed.educations.length">
              <div class="block-title">🎓 教育经历</div>
              <div v-for="(e, i) in parsed.educations" :key="i" class="item-card pretty-card">
                <div class="item-head">
                  <strong>{{ e.school }}</strong>
                  <span class="item-role">{{ e.major }} · {{ e.degree }}</span>
                </div>
                <div class="item-time">{{ e.startDate }} ~ {{ e.endDate }}</div>
                <p class="item-desc">{{ e.description }}</p>
              </div>
            </div>

            <div class="block" v-if="parsed.selfEvaluation">
              <div class="block-title">💬 自我评价</div>
              <p class="item-desc">{{ parsed.selfEvaluation }}</p>
            </div>
          </template>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
.resume-content {
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
.step.on { color: var(--brand-600); border-color: var(--brand-200); background: #fff; }
.step.done { color: #fff; background: var(--brand-500); border-color: var(--brand-500); }

/* 主体两栏 */
.resume-grid {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 18px;
  align-items: start;
}
@media (max-width: 1100px) {
  .resume-grid { grid-template-columns: 1fr; }
}

/* 卡片化面板 */
.panel {
  padding: 18px 20px;
  margin-bottom: 16px;
}
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

.actions { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-bottom: 14px; }
.actions .el-button { padding: 12px 24px; font-weight: 600; }

.hint-box {
  background: rgba(99, 102, 241, 0.06);
  border-left: 3px solid var(--brand-500);
  border-radius: 6px;
  padding: 10px 14px;
  font-size: 13px;
  color: var(--ink-700);
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

.block { margin-top: 18px; }
.block-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-700);
  margin-bottom: 10px;
  display: flex; align-items: center; gap: 4px;
}

.tag-cloud { display: flex; flex-wrap: wrap; gap: 6px; }
.skill-tag { margin: 0 !important; }

.mini-card {
  padding: 14px 16px;
  border-radius: 10px;
  height: 100%;
  border: 1px solid;
}
.mini-card.hl {
  background: linear-gradient(135deg, #ECFDF5 0%, #F0FDF4 100%);
  border-color: #A7F3D0;
}
.mini-card.iss {
  background: linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 100%);
  border-color: #FDE68A;
}
.mc-head {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-700);
  margin-bottom: 8px;
}
.mc-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: var(--ink-700);
  line-height: 1.75;
}

.item-card { padding: 14px 16px; margin-bottom: 10px; }
.item-head {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 14px;
  color: var(--ink-900);
}
.item-role { font-size: 12px; color: var(--brand-600); font-weight: 500; }
.item-time { font-size: 12px; color: var(--ink-500); margin-top: 4px; }
.tech-row { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 4px; }
.item-desc {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--ink-700);
  line-height: 1.7;
  white-space: pre-wrap;
}
.empty-text { font-size: 13px; color: var(--ink-500); }

/* ============================================================
   移动端（≤768px）
   ============================================================ */
@media (max-width: 768px) {
  .resume-content { gap: 12px; }

  /* Banner 原来是一行：图标 + 文字 + 三个步骤胶囊。
     手机上步骤胶囊会挤成竖排单字，改成换行布局。 */
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

  /* 结果面板的 min-height 在手机上没意义（内容本来就要滚） */
  .result-panel { min-height: auto; }
  .empty-result { padding: 40px 16px; }
  .er-icon { font-size: 48px; margin-bottom: 12px; }
  .er-title { font-size: 15px; }
  .er-sub { font-size: 12px; }

  /* 姓名/学历/学校…六项信息：单列，避免"毕业年份"折成两行 */
  .info-grid {
    grid-template-columns: 1fr;
    gap: 8px;
    padding: 12px;
  }
  .ii-value { font-size: 13px; }

  /* 亮点 / 待改进用 el-row+el-col :span=12，手机上强制单列 */
  .block :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }
  .block :deep(.el-col + .el-col) { margin-top: 10px; }

  .mini-card { padding: 12px 14px; }
  .mc-list { font-size: 12px; padding-left: 16px; line-height: 1.7; }

  .item-card { padding: 12px 14px; }
  .item-head { flex-direction: column; align-items: flex-start; gap: 2px; }
  .item-desc { font-size: 12px; }

  /* textarea 的 14 行在手机上太高，把按钮都顶到首屏外了 */
  .resume-textarea :deep(textarea) { font-size: 14px; }

  .actions { gap: 10px; }
  .actions .el-button { width: 100%; margin-left: 0 !important; padding: 10px 16px; }
  .actions :deep(.el-upload) { width: 100%; }

  .hint-box { font-size: 12px; padding: 10px 12px; }
}
</style>
