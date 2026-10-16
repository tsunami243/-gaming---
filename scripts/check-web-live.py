"""Real production Vue -> loopback proxy -> Spring HTTP -> isolated MySQL.
Invoked by the integration test while its published fixture is committed.
"""
import argparse
import http.client
import threading
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse
from playwright.sync_api import sync_playwright, expect

ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / 'gaming-news' / 'dist'

def run(port, article_id):
    class Handler(SimpleHTTPRequestHandler):
        def __init__(self, *args, **kwargs): super().__init__(*args, directory=str(DIST), **kwargs)
        def log_message(self, *args): pass
        def do_GET(self):
            parsed = urlparse(self.path)
            if parsed.path.startswith('/api/v1/'):
                upstream = http.client.HTTPConnection('127.0.0.1', port, timeout=10)
                try:
                    upstream.request('GET', self.path)
                    response = upstream.getresponse(); body = response.read()
                    self.send_response(response.status)
                    self.send_header('Content-Type', response.getheader('Content-Type', 'application/json'))
                    self.end_headers(); self.wfile.write(body)
                finally: upstream.close()
                return
            if not (DIST / parsed.path.lstrip('/')).is_file(): self.path = '/index.html'
            super().do_GET()
    if not (DIST / 'index.html').is_file(): raise RuntimeError('Build the Vue app first')
    server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
    thread = threading.Thread(target=server.serve_forever, daemon=True); thread.start()
    base = f'http://127.0.0.1:{server.server_port}'
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            try:
                for width in (1440, 390):
                    context = browser.new_context(viewport={'width': width, 'height': 900})
                    context.route('**/*', lambda route: route.continue_() if route.request.url.startswith(base) else route.abort())
                    page = context.new_page(); errors = []
                    page.on('pageerror', lambda error: errors.append(str(error)))
                    page.goto(base + '/home')
                    expect(page.locator('.article-card')).not_to_have_count(0)
                    page.locator('.game-tabs button', has_text='Apex Legends').click()
                    page.get_by_label('内容类型筛选').select_option('patch_notes')
                    expect(page.locator('.article-card')).to_have_count(1)
                    expect(page.locator('.meta-tag')).to_contain_text('Apex Legends')
                    page.locator('.article-card').click()
                    expect(page).to_have_url(base + f'/article/{article_id}')
                    expect(page.locator('.content')).to_contain_text('Staged content only')
                    expect(page.locator('.meta')).to_contain_text('VALORANT Official Test')
                    expect(page.locator('.meta')).to_contain_text('Apex Legends')
                    expect(page.locator('.original-link')).to_have_attribute('href', 'https://example.invalid/http-verified')
                    assert page.locator('.content').inner_html().count('<p>') == 0, 'stored markup must be rendered as text'
                    assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
                    expect(page.locator('.page')).to_have_css('opacity', '1')
                    page.screenshot(path=str(ROOT / 'target' / f'live-web-{width}.png'), full_page=True)
                    assert not errors, errors
                    context.close()
                    print(f'PASS real Spring/MySQL browser path at {width}px', flush=True)
            finally: browser.close()
    finally:
        server.shutdown(); server.server_close(); thread.join(5)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(); parser.add_argument('--port', type=int, required=True); parser.add_argument('--article-id', type=int, required=True)
    args = parser.parse_args()
    if not 1024 <= args.port <= 65535 or args.article_id < 1: parser.error('Invalid local test port or article id')
    run(args.port, args.article_id)
