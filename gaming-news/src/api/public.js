import request from '../utils/request'

export const getGames = () => request({ url: '/v1/games', method: 'get' })
export const getPublicArticlePage = (params) => request({ url: '/v1/articles', method: 'get', params })
export const getPublicArticleDetail = (id) => request({ url: `/v1/articles/${id}`, method: 'get' })
