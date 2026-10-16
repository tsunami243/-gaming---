import { describe, expect, it } from 'vitest'
import router from './index'

describe('disabled legacy routes', () => {
  it('redirects direct navigation back to the home page', async () => {
    await router.push('/publish')
    expect(router.currentRoute.value.path).toBe('/home')
  })

  it('redirects the demo message route back to the home page', async () => {
    await router.push('/message')
    expect(router.currentRoute.value.path).toBe('/home')
  })
})
