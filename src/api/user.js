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
