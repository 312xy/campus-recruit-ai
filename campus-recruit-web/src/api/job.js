import request from './request'

// 岗位相关接口，和后端 JobController 一一对应。
//
// 【和简历模块同样的坑】：AI 给 JD 打标签也要十几秒，
// 所以 POST 接口必须单独放宽超时（全局是 15 秒），否则后端还没返回前端就先超时了。
const AI_TIMEOUT = 120000

/** JD 打标签：POST /api/jobs */
export function parseJob(data) {
  return request.post('/jobs', data, { timeout: AI_TIMEOUT })
}

/** 我录入的岗位列表：GET /api/jobs */
export function listJobs() {
  return request.get('/jobs')
}

/** 岗位详情：GET /api/jobs/{id} */
export function getJob(id) {
  return request.get(`/jobs/${id}`)
}

/** 删除岗位：DELETE /api/jobs/{id} */
export function deleteJob(id) {
  return request.delete(`/jobs/${id}`)
}
