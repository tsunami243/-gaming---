import { describe, expect, it } from 'vitest'
import { stripHtml } from './format'

describe('stripHtml', () => {
  it('returns an empty string for missing content', () => {
    expect(stripHtml('')).toBe('')
    expect(stripHtml(null)).toBe('')
  })

  it('removes element markup and normalizes encoded text to spaces', () => {
    expect(stripHtml('<p>CS2&nbsp;<strong>更新</strong></p>')).toBe('CS2 更新')
  })
})
