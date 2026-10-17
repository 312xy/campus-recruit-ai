import request from './request'

// 模拟面试接口，与后端 InterviewController 对应。
//
// 一次出题 + 每题评分都可能跑十几秒，所以都设 120 秒超时。
const AI_TIMEOUT = 120000

/** 开始一场面试（AI 一次性出 5 道题） */
export function startInterview(jobId) {
  return request.post('/interviews/start', { jobId }, { timeout: AI_TIMEOUT })
}

/** 我的所有面试 */
export function listInterviews() {
  return request.get('/interviews')
}

/** 单场面试详情 */
export function getInterview(id) {
  return request.get(`/interviews/${id}`)
}

/** 提交一题答案（AI 单题评分） */
export function submitAnswer(id, payload) {
  return request.post(`/interviews/${id}/answer`, payload, { timeout: AI_TIMEOUT })
}

/** 删除 */
export function deleteInterview(id) {
  return request.delete(`/interviews/${id}`)
}

/** 给前端做"题型"显示 */
export function typeLabel(type) {
  const map = {
    technical_baseline: '技术基础',
    project_dive: '项目深挖',
    algorithm: '算法/编码',
    communication: '沟通表达',
    candidate_question: '候选人提问'
  }
  return map[type] || '面试题'
}

export function typeTagType(type) {
  const map = {
    technical_baseline: 'primary',
    project_dive: 'success',
    algorithm: 'warning',
    communication: 'info',
    candidate_question: ''
  }
  return map[type] || ''
}