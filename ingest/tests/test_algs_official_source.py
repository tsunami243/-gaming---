import unittest
from pathlib import Path
from bs4 import BeautifulSoup

from filters.fps_rules import classify
from spiders.algs_official import AlgsOfficialSpider


class AlgsOfficialSpiderTest(unittest.TestCase):
    def test_captured_algs_pro_league_article_is_an_apex_esports_candidate(self):
        root = Path(__file__).parent / "fixtures"
        spider = AlgsOfficialSpider()
        urls = spider.fetch_urls(BeautifulSoup((root / "algs_news_2026-09-22.html").read_text(encoding="utf-8"), "lxml"))
        target = "https://algs.ea.com/en/news/algs-split-2-pro-league"
        self.assertIn(target, urls)
        article = spider.parse(BeautifulSoup((root / "algs_split_2_pro_league_2026-09-22.html").read_text(encoding="utf-8"), "lxml"), target)
        self.assertEqual("ALGS Split 2 Pro League Kicks Off!", article.title)
        self.assertGreater(len(article.raw_html), 1000)
        result = classify(article.title, article.raw_html)
        self.assertEqual(("apex-legends",), result.game_codes)
        self.assertEqual("esports", result.content_type)
        self.assertEqual("ACCEPT", result.decision)


if __name__ == "__main__":
    unittest.main()
