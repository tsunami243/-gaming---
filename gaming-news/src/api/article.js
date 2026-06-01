import request from '../utils/request'

export const getArticleList = (params) => {
  return request({
    url: '/article',
    method: 'get',
    params
  })
}

export const getArticleDetail = (id) => {
  return request({
    url: '/article/detail',
    method: 'get',
    params: { id }
  })
}

export const addArticle = (data) => {
  return request({
    url: '/article',
    method: 'post',
    data
  })
}

export const deleteArticle = (id) => {
  return request({
    url: '/article',
    method: 'delete',
    params: { id }
  })
}

export const getCarouselList = (limit) => {
  return request({
    url: '/article/carousel',
    method: 'get',
    params: { limit }
  })
}

export const getArticleStats = (articleId) => {
  return request({
    url: '/article/stats',
    method: 'get',
    params: { articleId }
  })
}

export const getArticleHeat = (articleId) => {
  return request({
    url: '/article/heat',
    method: 'get',
    params: { articleId }
  })
}

export const getCommentList = (articleId) => {
  return request({
    url: `/comment/${articleId}`,
    method: 'get'
  })
}

export const addComment = (data) => {
  return request({
    url: '/comment',
    method: 'post',
    data
  })
}

export const deleteComment = (id) => {
  return request({
    url: `/comment/${id}`,
    method: 'delete'
  })
}

export const likeArticle = (articleId) => {
  return request({
    url: '/article-like',
    method: 'post',
    params: { articleId }
  })
}

export const unlikeArticle = (articleId) => {
  return request({
    url: '/article-like',
    method: 'delete',
    params: { articleId }
  })
}

export const checkArticleLiked = (articleId) => {
  return request({
    url: '/article-like/check',
    method: 'get',
    params: { articleId }
  })
}

export const getArticleLikeCount = (articleId) => {
  return request({
    url: '/article-like/count',
    method: 'get',
    params: { articleId }
  })
}

export const likeComment = (commentId) => {
  return request({
    url: '/comment-like',
    method: 'post',
    params: { commentId }
  })
}

export const unlikeComment = (commentId) => {
  return request({
    url: '/comment-like',
    method: 'delete',
    params: { commentId }
  })
}

export const favArticle = (articleId) => request({ url: '/article-favorite', method: 'post', params: { articleId } })
export const unfavArticle = (articleId) => request({ url: '/article-favorite', method: 'delete', params: { articleId } })
export const checkFaved = (articleId) => request({ url: '/article-favorite/check', method: 'get', params: { articleId } })
export const recordView = (articleId) => request({ url: '/article-view', method: 'post', params: { articleId } })

export const checkCommentLiked = (commentId) => {
  return request({
    url: '/comment-like/check',
    method: 'get',
    params: { commentId }
  })
}

export const getCommentLikeCount = (commentId) => {
  return request({
    url: '/comment-like/count',
    method: 'get',
    params: { commentId }
  })
}
