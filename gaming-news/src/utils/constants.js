export const BASE_URL = '/api'
export const DEFAULT_PAGE_SIZE = 10
export const DEFAULT_ARTICLE_IMAGE = 'https://via.placeholder.com/120x80?text=Game+News'
export const DEFAULT_AVATAR = 'https://via.placeholder.com/60'

// 后端真值枚举，前端一律引用，禁止再出现中文混拼的字符串字面量。
// 见 src/main/java/org/example/bigevent/api/v1/admin/AdminModerationService.java 与 AdminOperationsMapper.java
export const ARTICLE_WORKFLOW_STATUS = Object.freeze({
  PENDING_REVIEW: 'PENDING_REVIEW',
  PUBLISHED: 'PUBLISHED',
  REJECTED: 'REJECTED',
  UNPUBLISHED: 'UNPUBLISHED',
})

export const SOURCE_VERIFICATION_STATUS = Object.freeze({
  CANDIDATE: 'CANDIDATE',
  SHADOW: 'SHADOW',
  FETCH_VERIFIED: 'FETCH_VERIFIED',
  PAUSED: 'PAUSED',
})
