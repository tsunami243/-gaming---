import request from '../utils/request'

export const login = (data) => {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

export const register = (data) => {
  return request({
    url: '/user/register',
    method: 'post',
    data
  })
}

export const getUserInfo = () => {
  return request({
    url: '/user/userInfo',
    method: 'get'
  })
}

export const updateUser = (data) => {
  return request({
    url: '/user/update',
    method: 'put',
    data
  })
}

export const updateAvatar = (avatarUrl) => {
  return request({
    url: '/user/updateAvatar',
    method: 'patch',
    params: { avatarUrl }
  })
}

export const updatePwd = (data) => {
  return request({
    url: '/user/updatePwd',
    method: 'patch',
    data
  })
}

// 让服务端同步作废当前令牌；只清本地 localStorage 的话，旧 token 在过期前仍可被重放。
// 退出走独立短超时：本地状态本来就要清，不该因为后端不可达让用户等满全局的 10 秒。
export const logout = () => {
  return request({
    url: '/user/logout',
    method: 'post',
    timeout: 3000
  })
}
