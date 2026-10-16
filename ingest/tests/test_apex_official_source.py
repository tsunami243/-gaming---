import unittest
import httpx
from pathlib import Path
from unittest.mock import patch

from bs4 import BeautifulSoup
from filters.fps_rules import classify
from spiders.apex_official import ApexOfficialSpider

class ApexOfficialSpiderTest(unittest.TestCase):
    def test_captured_official_patch_note_is_discovered_and_classified(self):
        root = Path(__file__).parent / 'fixtures'; spider = ApexOfficialSpider()
        urls = spider.fetch_urls(BeautifulSoup((root / 'apex_news_2026-09-20.html').read_text(encoding='utf-8'), 'lxml'))
        target = 'https://www.ea.com/games/apex-legends/apex-legends/news/marked-midseason-patch-notes'
        self.assertIn(target, urls)
        article = spider.parse(BeautifulSoup((root / 'apex_marked_midseason_patch_notes_2026-09-20.html').read_text(encoding='utf-8'), 'lxml'), target)
        self.assertEqual('Apex Legends™: Marked Midseason Patch Notes', article.title)
        self.assertGreater(len(article.raw_html), 1000)
        result = classify(article.title, article.raw_html)
        self.assertEqual(('apex-legends',), result.game_codes)
        self.assertEqual('patch_notes', result.content_type)
        self.assertEqual('ACCEPT', result.decision)

    def test_crawl_carries_list_publication_date_into_article(self):
        root = Path(__file__).parent / 'fixtures'
        list_html = (root / 'apex_news_2026-09-20.html').read_text(encoding='utf-8')
        detail_html = (root / 'apex_marked_midseason_patch_notes_2026-09-20.html').read_text(encoding='utf-8')
        class Response:
            def __init__(self, text): self.text, self.encoding = text, 'utf-8'
            def raise_for_status(self): pass
        class Client:
            def get(self, url): return Response(detail_html if 'marked-midseason-patch-notes' in url else list_html)
        spider = ApexOfficialSpider()
        spider._client = httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(200, text=detail_html if 'marked-midseason-patch-notes' in str(request.url) else list_html)))
        from settings import settings
        original = settings.MAX_ARTICLES_PER_RUN; settings.MAX_ARTICLES_PER_RUN = 1
        try:
            with patch('spiders.apex_official.time.sleep'):
                article = spider.crawl(spider.LIST_URL)[0]
            self.assertEqual('2026-09-15', article.original_published_date.isoformat())
            self.assertIsNone(article.original_published_at)
        finally:
            settings.MAX_ARTICLES_PER_RUN = original

if __name__ == '__main__':
    unittest.main()
