import json
import httpx
import unittest
from pathlib import Path

from filters.fps_rules import classify
from spiders.steam_cs2 import SteamCs2OfficialSpider


class SteamCs2OfficialSpiderTest(unittest.TestCase):
    def test_live_captured_official_api_sample_preserves_title_url_and_time(self):
        fixture = Path(__file__).parent / 'fixtures' / 'cs2_steam_news_api_2026-09-20.json'
        spider = SteamCs2OfficialSpider()
        spider._client = httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(200, text=fixture.read_text(encoding='utf-8'))))
        articles = spider.crawl(spider.LIST_URL)
        self.assertGreaterEqual(len(articles), 1)
        first = articles[0]
        self.assertEqual('Counter-Strike 2 Update', first.title)
        self.assertTrue(first.original_url.startswith('https://steamstore-a.akamaihd.net/news/externalpost/steam_community_announcements/'))
        self.assertIsNotNone(first.original_published_at)
        result = classify(first.title, first.raw_html)
        self.assertEqual('patch_notes', result.content_type)
        self.assertEqual(('cs2',), result.game_codes)

    def test_respects_per_run_candidate_limit_for_api_items(self):
        fixture = Path(__file__).parent / 'fixtures' / 'cs2_steam_news_api_2026-09-20.json'
        spider = SteamCs2OfficialSpider()
        spider._client = httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(200, text=fixture.read_text(encoding='utf-8'))))
        from settings import settings
        previous = settings.MAX_ARTICLES_PER_RUN; settings.MAX_ARTICLES_PER_RUN = 1
        try:
            self.assertEqual(1, len(spider.crawl(spider.LIST_URL)))
        finally:
            settings.MAX_ARTICLES_PER_RUN = previous


    def test_third_party_feed_items_are_never_mislabeled_as_official_announcements(self):
        payload = {
            'appnews': {'newsitems': [
                {'feedlabel': 'PlayGround.ru - official group', 'title': 'Counter-Strike 2 external rumor', 'contents': 'CS2 update',
                 'url': 'https://steamstore-a.akamaihd.net/news/externalpost/PlayGround.ru/123', 'date': 1},
                {'feedlabel': 'steam_community_announcements', 'title': 'Counter-Strike 2 Update', 'contents': 'Official update notes',
                 'url': 'https://steamstore-a.akamaihd.net/news/externalpost/steam_community_announcements/456', 'date': 2},
            ]}
        }
        spider = SteamCs2OfficialSpider()
        spider._client = httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(200, text=json.dumps(payload))))
        articles = spider.crawl(spider.LIST_URL)
        self.assertEqual(1, len(articles))
        self.assertEqual('Counter-Strike 2 Update', articles[0].title)
        self.assertNotIn('external rumor', articles[0].title.lower())


if __name__ == '__main__':
    unittest.main()
