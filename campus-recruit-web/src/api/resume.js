import request from './request'

// 简历相关接口，和后端 ResumeController 一一对应。
//
// 【重要】AI 解析很慢：大模型要"读"完整份简历再生成 JSON，
// 实测通常 10~40 秒。而 request.js 里全局超时设的是 15 秒，
// 如果这里不单独放宽，会出现"后端其实解析成功了，但前端已经报超时"的尴尬情况。
// 所以凡是走 AI 的接口，都单独把 timeout 传长一点。
//
// 面试必懂点：这是典型的"超时时间要按最慢的下游来定"。
// 同一个前端应用里，普通接口 15 秒足够，AI 接口要 120 秒，
// 一刀切设成 120 秒会让普通接口挂了也不报错、用户白等，所以要分开配置。
const AI_TIMEOUT = 120000

/** 粘贴文本解析：POST /api/resumes/parse */
export function parseResume(data) {
  return request.post('/resumes/parse', data, { timeout: AI_TIMEOUT })
}

/** 上传 .txt/.md 文件解析：POST /api/resumes/upload（multipart/form-data） */
export function uploadResume(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/resumes/upload', form, {
    timeout: AI_TIMEOUT,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 我的简历列表：GET /api/resumes */
export function listResumes() {
  return request.get('/resumes')
}

/** 简历详情：GET /api/resumes/{id} */
export function getResume(id) {
  return request.get(`/resumes/${id}`)
}

/** 删除简历：DELETE /api/resumes/{id} */
export function deleteResume(id) {
  return request.delete(`/resumes/${id}`)
}
