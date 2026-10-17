import request from './request'

// 匹配评分接口，与后端 MatchController 一一对应。
//
// 【AI 调用通常要 10~30 秒】全局 axios 超时是 15 秒会先超时，
// 所以这里的 POST 必须单独把超时拉到 120 秒（与简历/岗位解析保持一致）。
const AI_TIMEOUT = 120000

/** 发起匹配评估：POST /api/matches → { matchedScore, parsed: {...}, status } */
export function evaluateMatch(data) {
  return request.post('/matches', data, { timeout: AI_TIMEOUT })
}

/** 我的评估记录列表：GET /api/matches */
export function listMatches() {
  return request.get('/matches')
}

/** 评估详情：GET /api/matches/{id} */
export function getMatch(id) {
  return request.get(`/matches/${id}`)
}

/** 删除评估：DELETE /api/matches/{id} */
export function deleteMatch(id) {
  return request.delete(`/matches/${id}`)
}

/** 给前端做"评分等级"显示用 */
export function levelTagType(level) {
  switch (level) {
    case '优秀': return 'success'
    case '良好': return 'primary'
    case '一般': return 'warning'
    default: return 'danger'
  }
}