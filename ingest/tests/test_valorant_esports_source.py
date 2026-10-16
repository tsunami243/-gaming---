import unittest
from pathlib import Path
from bs4 import BeautifulSoup

from filters.fps_rules import classify
from spiders.valorant_esports import ValorantEsportsOfficialSpider


class ValorantEsportsOfficialSpiderTest(unittest.TestCase):
    def test_captured_official_event_news_preserves_title_url_and_date(self):
        root = Path(__file__).parent / "fixtures"
        spider = ValorantEsportsOfficialSpider()
        urls = spider.fetch_urls(BeautifulSoup((root / "valorant_esports_news_2026-09-22.html").read_text(encoding="utf-8"), "lxml"))
        target = "https://valorantesports.com/news/champions-shanghai-opening-day"
        self.assertIn(target, urls)
        article = spider.parse(BeautifulSoup((root / "valorant_esports_champions_opening_day_2026-09-22.html").read_text(encoding="utf-8"), "lxml"), target)
        self.assertEqual("Champions Shanghai Opening Day", article.title)
        self.assertEqual("2026-09-16", article.original_published_date.isoformat())
        self.assertGreater(len(article.raw_html), 1000)
        result = classify(article.title, article.raw_html)
        self.assertEqual(("valorant",), result.game_codes)
        self.assertEqual("esports", result.content_type)
        self.assertEqual("ACCEPT", result.decision)


if __name__ == "__main__":
    unittest.main()

