import request from './request'

// 认证相关的接口调用，和后端 AuthController 一一对应。
//
// 人话讲解：
// - 把"接口地址和参数"集中在这里，页面组件里只管调用方法名，不去关心 URL 拼写。
//   以后后端接口改了路径，只改这一个文件，页面代码不用动。
// - 这些函数返回的是 Promise，页面里用 await 拿结果。

/** 注册：POST /api/auth/register → { id, username, email, role } */
export function register(data) {
  return request.post('/auth/register', data)
}

/** 登录：POST /api/auth/login → { token, tokenType } */
export function login(data) {
  return request.post('/auth/login', data)
}

/** 获取当前登录用户：GET /api/auth/me（需带 token）→ { username } */
export function getMe() {
  return request.get('/auth/me')
}
