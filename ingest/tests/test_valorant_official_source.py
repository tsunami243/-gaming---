import unittest
from pathlib import Path
from bs4 import BeautifulSoup
from filters.fps_rules import classify
from spiders.valorant_official import ValorantOfficialSpider

class ValorantOfficialSpiderTest(unittest.TestCase):
    def test_live_captured_patch_notes_keep_url_title_and_original_time(self):
        root = Path(__file__).parent / 'fixtures'
        spider = ValorantOfficialSpider()
        list_soup = BeautifulSoup((root / 'valorant_game_updates_2026-09-20.html').read_text(encoding='utf-8'), 'lxml')
        urls = spider.fetch_urls(list_soup)
        self.assertGreaterEqual(len(urls), 1)
        self.assertEqual('https://playvalorant.com/en-us/news/game-updates/valorant-patch-notes-13-05', urls[0])
        detail_soup = BeautifulSoup((root / 'valorant_patch_notes_13_05_2026-09-20.html').read_text(encoding='utf-8'), 'lxml')
        article = spider.parse(detail_soup, urls[0])
        self.assertEqual('VALORANT Patch Notes 13.05', article.title)
        self.assertIsNotNone(article.original_published_at)
        self.assertGreater(len(article.raw_html), 1000)
        result = classify(article.title, article.raw_html)
        self.assertEqual(('valorant',), result.game_codes)
        self.assertEqual('patch_notes', result.content_type)
        self.assertEqual('ACCEPT', result.decision)

if __name__ == '__main__':
    unittest.main()
