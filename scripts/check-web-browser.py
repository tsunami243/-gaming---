"""Production Vue bundle in real Chromium, with an explicit local fixture API.
Run after npm run build. This proves UI flows, NOT a live Spring/MySQL integration.
Uses installed Playwright/Chromium only; never downloads browsers or accesses real APIs.
"""
import json
import threading
from collections import Counter
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse
from playwright.sync_api import sync_playwright, expect

ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / 'gaming-news' / 'dist'
GAMES = [dict(code=code, nameZh=name, nameOriginal=name, aliases=[], enabled=True) for code, name in [
    ('delta-force', '三角洲行动'), ('valorant', '无畏契约'), ('cs2', 'CS2'), ('apex-legends', 'Apex Legends')]]
requests = Counter()
mode = {'error': False, 'empty': False, 'games_error': False}

def item(id, game='cs2'):
    return dict(id=id, title=f'测试资讯 {id}', summary='仅显示中文摘要', gameCodes=[game], tags=[], contentType='patch_notes',
                readingMode='FULL_TEXT', workflowStatus='PUBLISHED', sourceLanguage='en',
                originalPublishedAt=None, publishedAt='2026-09-20T12:00:00Z', coverImg=None,
                originalUrl='https://example.invalid/original', content='第一段正文\n\n第二段正文')

class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs): super().__init__(*args, directory=str(DIST), **kwargs)
    def log_message(self, *args): pass
    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path.startswith('/api/'):
            requests[self.path] += 1
            query = parse_qs(parsed.query)
            status, data = 200, None
            if parsed.path == '/api/v1/games':
                data = GAMES
                if mode['games_error']: status = 503
            elif parsed.path == '/api/v1/articles':
                page = int(query.get('pageNum', ['1'])[0]); game = query.get('gameCode', ['cs2'])[0]
                data = dict(items=[] if mode['empty'] else [item(page, game)], total=0 if mode['empty'] else 2, pageNum=page, pageSize=20)
                if mode['error']: status = 503
            elif parsed.path == '/api/v1/articles/1/comments': data = dict(items=[],total=0,pageNum=1,pageSize=20)
            elif parsed.path == '/api/v1/articles/1': data = item(1)
            else: status = 404
            body = json.dumps(dict(code=0 if status == 200 else 1, message='fixture error', data=data, traceId='fixture')).encode()
            self.send_response(status); self.send_header('Content-Type', 'application/json'); self.end_headers(); self.wfile.write(body)
            return
        path = DIST / parsed.path.lstrip('/')
        if not path.is_file(): self.path = '/index.html'
        super().do_GET()

def run():
    if not (DIST / 'index.html').exists(): raise RuntimeError('Build the frontend first')
    server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
    thread = threading.Thread(target=server.serve_forever, daemon=True); thread.start()
    base = f'http://127.0.0.1:{server.server_port}'
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            try:
                for viewport in ({'width': 1440, 'height': 900}, {'width': 390, 'height': 844}):
                    context = browser.new_context(viewport=viewport)
                    # Font/CDN requests are intentionally blocked: this test must remain offline.
                    context.route('**/*', lambda route: route.continue_() if route.request.url.startswith(base) else route.abort())
                    page = context.new_page(); errors = []
                    page.on('pageerror', lambda error: errors.append(str(error)))
                    requests.clear(); mode.update(error=False, empty=False, games_error=False)
                    page.goto(base + '/home'); expect(page.locator('.article-card')).to_have_count(1)
                    assert sum(n for url, n in requests.items() if url.startswith('/api/v1/articles?')) == 1
                    expect(page.locator('.game-tabs button')).to_have_count(5)
                    page.locator('.game-tabs button', has_text='无畏契约').click()
                    page.get_by_label('内容类型筛选').select_option('patch_notes')
                    expect(page.locator('.meta-tag')).to_have_text('无畏契约')
                    page.locator('.load-more').click(); expect(page.locator('.article-card')).to_have_count(2)
                    assert any('gameCode=valorant' in url and 'contentType=patch_notes' in url and 'pageNum=2' in url for url in requests)
                    page.locator('.article-card').first.click()
                    expect(page.locator('.title')).to_have_text('测试资讯 1')
                    expect(page.locator('.content')).to_contain_text('第一段正文')
                    expect(page.locator('.content')).to_contain_text('第二段正文')
                    expect(page.locator('.original-link')).to_have_attribute('href', 'https://example.invalid/original')
                    assert page.evaluate('document.documentElement.scrollWidth <= window.innerWidth'), 'horizontal overflow'
                    page.goto(base + '/article/999'); expect(page.get_by_text('文章不存在或尚未发布')).to_be_visible()
                    mode['error'] = True
                    page.goto(base + '/home'); expect(page.locator('.retry-btn')).to_be_visible()
                    mode.update(error=False, empty=True)
                    page.locator('.retry-btn').click(); expect(page.get_by_text('暂无资讯')).to_be_visible()
                    page.goto(base + '/message'); expect(page).to_have_url(base + '/home')
                    mode.update(error=False, empty=False, games_error=True)
                    page.goto(base + '/home')
                    expect(page.get_by_text('游戏列表加载失败：', exact=False)).to_be_visible()
                    mode['games_error'] = False
                    page.get_by_role('button', name='重试', exact=True).click()
                    expect(page.locator('.games-error')).to_have_count(0)
                    expect(page.locator('.game-tabs button')).to_have_count(5)
                    assert not errors, errors
                    context.close()
                    print(f"PASS: Chromium {viewport['width']}px filtering/paging/detail/404/retry/empty/disabled route")
            finally: browser.close()
    finally:
        server.shutdown(); server.server_close(); thread.join(timeout=5)

if __name__ == '__main__': run()
