// Actual HTTP proxy integration; no production API, database or credentials are used.
import assert from 'node:assert/strict'
import { createServer as createHttpServer } from 'node:http'
import { createServer as createViteServer } from 'vite'
import { fileURLToPath } from 'node:url'

const seen = []
const backend = createHttpServer((request, response) => {
  seen.push(request.url)
  response.writeHead(request.url.includes('/admin/') ? 403 : 200, { 'Content-Type': 'application/json' })
  response.end(JSON.stringify({ path: request.url, method: request.method }))
})
await new Promise(resolve => backend.listen(0, '127.0.0.1', resolve))
const previousTarget = process.env.GAMEBOI_DEV_API_TARGET
process.env.GAMEBOI_DEV_API_TARGET = `http://127.0.0.1:${backend.address().port}`
let vite
try {
  const root = fileURLToPath(new URL('../', import.meta.url))
  vite = await createViteServer({ root, configFile: `${root}/vite.config.js`, server: { host: '127.0.0.1', port: 0, open: false } })
  await vite.listen()
  const base = `http://127.0.0.1:${vite.httpServer.address().port}`
  const cases = [
    ['/api/v1/games', '/api/v1/games', 'GET', 200],
    ['/api/v1/articles?gameCode=valorant&pageNum=2', '/api/v1/articles?gameCode=valorant&pageNum=2', 'GET', 200],
    ['/api/v1/articles/7', '/api/v1/articles/7', 'GET', 200],
    ['/api/v1/admin/articles', '/api/v1/admin/articles', 'GET', 403],
    ['/api/user/login', '/user/login', 'POST', 200],
    ['/api/article-like?articleId=7', '/article-like?articleId=7', 'POST', 200]
  ]
  for (const [path, expected, method, status] of cases) {
    const response = await fetch(base + path, { method })
    assert.equal(response.status, status)
    assert.deepEqual(await response.json(), { path: expected, method })
  }
  const count = seen.length
  for (const path of ['/internal/v1/ingest/events', '/api/internal/v1/ingest/events', '/api/push/demo']) {
    await fetch(base + path, { method: 'POST' })
  }
  assert.equal(seen.length, count, 'internal/demo endpoints must not be forwarded')
  console.log('PASS: 6 HTTP proxy routes/statuses and 3 internal/demo non-forwarding checks')
} finally {
  if (vite) await vite.close()
  await new Promise(resolve => backend.close(resolve))
  if (previousTarget === undefined) delete process.env.GAMEBOI_DEV_API_TARGET
  else process.env.GAMEBOI_DEV_API_TARGET = previousTarget
}
