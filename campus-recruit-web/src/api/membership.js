import request from './request'

// 会员/订单接口，对应后端 MembershipController。
//
// 注意：前端 axios 全局超时 15 秒，但"模拟支付"包含两步事务处理，
// 偶尔会慢一点，所以单独放宽到 30 秒（不会真调用 AI，所以不用 120s）。
const PAY_TIMEOUT = 30000

/** 套餐 */
export function listPlans() {
  return request.get('/memberships/plans')
}

/** 我的会员 */
export function getMyMembership() {
  return request.get('/memberships/me')
}

/** 我的所有订单 */
export function listMyOrders() {
  return request.get('/memberships/orders')
}

/** 创建订单 */
export function createOrder(plan) {
  return request.post('/memberships/orders', { plan })
}

/** 模拟支付（DB 模拟版） */
export function payOrder(orderNo) {
  return request.post(`/memberships/orders/${orderNo}/pay`, {}, { timeout: PAY_TIMEOUT })
}