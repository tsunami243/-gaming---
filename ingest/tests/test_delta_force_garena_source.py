import json
import unittest
from pathlib import Path
from unittest.mock import patch

from filters.fps_rules import classify
from spiders.delta_force_garena import DeltaForceGarenaOfficialSpider


class DeltaForceOfficialFlowApiTest(unittest.TestCase):
    """离线回归：用受控请求捕获的真实 flow 响应验证解析与 URL 归属。"""

    def setUp(self):
        root = Path(__file__).parent / "fixtures"
        self.spider = DeltaForceGarenaOfficialSpider()
        self.list_payload = json.loads(
            (root / "delta_force_qq_newslist_flow.json").read_text(encoding="utf-8"))
        self.text_detail = json.loads(
            (root / "delta_force_qq_newsdetail_text_flow.json").read_text(encoding="utf-8"))
        self.image_detail = json.loads(
            (root / "delta_force_qq_newsdetail_flow.json").read_text(encoding="utf-8"))
        self.items = self.list_payload["jData"]["data"]["items"]
        self.item = self.items[0]

    def _post(self, url, data):
        """按 iChartId 返回列表/详情夹具，并断言请求打到第一方 flow 端点。"""
        self.assertEqual("https://comm.ams.game.qq.com/ide/", url)
        if data["iChartId"] == self.spider.NEWS_LIST_CHART_ID:
            return self.list_payload
        self.assertEqual(self.spider.NEWS_DETAIL_CHART_ID, data["iChartId"])
        # 详情接口按 id 取数：文本型文章用真实夹具，其余复用图片型夹具但校正归属字段，
        # 以便在固定夹具下仍能断言 article URL 由 iDocID 推导。
        for payload in (self.text_detail, self.image_detail):
            record = payload["jData"]["data"][0]
            if record["iDocID"] == data["id"]:
                return payload
        item = next(i for i in self.items if i["iDocID"] == data["id"])
        record = dict(self.image_detail["jData"]["data"][0])
        record.update({"iDocID": item["iDocID"], "sTitle": item["sTitle"],
                       "sIdxTime": item["sIdxTime"], "sRedirectURL": item["sRedirectURL"]})
        return {"ret": 0, "iRet": 0, "sMsg": "ok", "jData": {"iRet": "0", "sMsg": "ok", "data": [record]}}

    def test_list_flow_items_are_discovered_from_the_news_channel_only(self):
        seen = {}

        def recording_post(url, data):
            seen.update(data)
            return self._post(url, data)

        with patch.object(self.spider, "post_form_json", side_effect=recording_post):
            items = self.spider.fetch_news_items()
        self.assertEqual(6, len(items))
        self.assertEqual(self.item["iDocID"], items[0]["iDocID"])
        self.assertEqual("我来同你玩！——《三角洲行动》二洲年庆典圆满落幕！", items[0]["sTitle"])
        # 请求必须落在「新闻」栏目，且不混用其它栏目 id。
        self.assertEqual("6898", seen["chanid"])
        self.assertEqual(self.spider.NEWS_CHANNEL_ID, seen["chanid"])
        for item in items:
            self.assertIn(f"{self.spider.NEWS_CHANNEL_ID}|新闻", item["sChannelInfo"])

    def test_crawled_article_keeps_official_url_title_date_and_images(self):
        with patch.object(self.spider, "post_form_json", side_effect=self._post), \
                patch("spiders.delta_force_garena.settings.MAX_ARTICLES_PER_RUN", 6), \
                patch("spiders.delta_force_garena.settings.CRAWL_DELAY_SECONDS", 0):
            articles = self.spider.crawl(self.spider.LIST_URL)
        self.assertEqual(6, len(articles))
        for article, item in zip(articles, self.items):
            self.assertEqual(f"{self.spider.ARTICLE_URL_PREFIX}{item['iDocID']}", article.original_url)
            self.assertTrue(article.original_url.startswith(
                "https://df.qq.com/cp/a20240906main/newsdetail.html?id="))
            self.assertEqual(self.spider.source_name, article.source)
        article = articles[0]
        self.assertEqual("我来同你玩！——《三角洲行动》二洲年庆典圆满落幕！", article.title)
        self.assertEqual("2026-09-27", article.original_published_date.isoformat())
        self.assertIn("<img", article.raw_html)
        # 正文是长图文，图片应全部提升为绝对 https 地址。
        self.assertEqual(
            "https://static.gametalk.qq.com/image/423/1790492485_c48e0ca2bc76aa58f82119712cd34a15.png",
            article.images[0])
        self.assertGreater(len(article.images), 5)
        self.assertTrue(all(image.startswith("https://static.gametalk.qq.com/") for image in article.images))

    def test_text_announcement_is_classified_for_the_delta_force_whitelist(self):
        with patch.object(self.spider, "post_form_json", side_effect=self._post):
            article = self.spider.fetch_article(self.text_detail["jData"]["data"][0]["iDocID"])
        self.assertIn("三角洲行动", article.title)
        result = classify(article.title, article.raw_html)
        self.assertEqual(("delta-force",), result.game_codes)
        # 标题「二洲年庆典圆满落幕」是明确的标题级活动证据，按既有规则
        # (explicit headline type wins over body words) 判定为 event；
        # 这比此前退回正文命中「更新」而误判 patch_notes 更准确。
        self.assertEqual("event", result.content_type)
        self.assertEqual("ACCEPT", result.decision)

    def test_flow_api_errors_fail_closed(self):
        for payload in ({"iRet": -1, "sMsg": "ERROR_PARAMS"},
                        {"iRet": 0, "jData": {"iRet": -11009, "sMsg": "流程尚未发布"}},
                        {"iRet": 0, "jData": {"iRet": "0", "data": {}}}):
            with patch.object(self.spider, "post_form_json", return_value=payload):
                with self.assertRaises(ValueError):
                    self.spider.fetch_news_items()

    def test_only_the_registered_flow_origin_is_reachable(self):
        self.spider.validate_source_url(self.spider.FLOW_API_URL)
        self.spider.validate_source_url(self.spider.LIST_URL)
        for outside in ("https://www.google.com/ide/",
                        "https://comm.ams.game.qq.com.evil.example/ide/",
                        "http://comm.ams.game.qq.com/ide/",
                        "https://user:pw@comm.ams.game.qq.com/ide/",
                        "https://comm.ams.game.qq.com:8443/ide/"):
            with self.assertRaises(ValueError):
                self.spider.validate_source_url(outside)

    def test_extra_origins_must_be_https(self):
        self.assertEqual(frozenset(), DeltaForceGarenaOfficialSpider.__mro__[1].API_ORIGINS)
        with patch.object(DeltaForceGarenaOfficialSpider, "API_ORIGINS", frozenset({"http://comm.ams.game.qq.com/"})):
            with self.assertRaises(ValueError):
                self.spider.validate_source_url(self.spider.FLOW_API_URL)


if __name__ == "__main__":
    unittest.main()
