import unittest
from pathlib import Path
from bs4 import BeautifulSoup

from filters.fps_rules import classify
from main import build_ingest_event
from models.article import ProcessedArticle
from spiders.valorant_official import ValorantOfficialSpider

class ValorantVerticalSampleTest(unittest.TestCase):
    def test_offline_source_sample_to_versioned_event_preserves_required_editorial_fields(self):
        root = Path(__file__).parent / 'fixtures'
        spider = ValorantOfficialSpider()
        list_soup = BeautifulSoup((root / 'valorant_game_updates_2026-09-20.html').read_text(encoding='utf-8'), 'lxml')
        url = spider.fetch_urls(list_soup)[0]
        detail_soup = BeautifulSoup((root / 'valorant_patch_notes_13_05_2026-09-20.html').read_text(encoding='utf-8'), 'lxml')
        raw = spider.parse(detail_soup, url)
        processed = ProcessedArticle(title='无畏契约 13.05 版本更新说明', content=raw.raw_html,
            category_id=8, cover_img='', tags='["版本更新"]', summary='英文官方更新已生成中文摘要。',
            source=raw.source, original_url=raw.original_url, crawled_at='2026-09-20T12:30:00Z',
            original_published_at=raw.original_published_at.isoformat())
        event = build_ingest_event('valorant-official', spider, processed, classify(raw.title, raw.raw_html))
        self.assertEqual('valorant-official', event['sourceCode'])
        self.assertEqual('valorant', event['gameCodes'][0])
        self.assertEqual('patch_notes', event['contentType'])
        self.assertEqual(processed.title, event['title'])
        self.assertEqual(processed.summary, event['summary'])
        self.assertEqual(raw.original_url, event['originalUrl'])
        self.assertEqual(processed.original_published_at, event['originalPublishedAt'])
        self.assertEqual(['版本更新'], event['tags'])

if __name__ == '__main__':
    unittest.main()
