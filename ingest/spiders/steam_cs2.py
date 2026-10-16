"""Valve Steam News API adapter for official Counter-Strike 2 announcements."""
import json
from datetime import datetime, timezone
from html import escape
from typing import List

from models.article import RawArticle
from spiders.base import BaseSpider
from settings import settings


class SteamCs2OfficialSpider(BaseSpider):
    """Counter-Strike 2 官方公告适配器，数据源为 Steam News JSON API。"""

    SOURCE_CODE = "cs2-official"
    LIST_URL = "https://api.steampowered.com/ISteamNews/GetNewsForApp/v0002/?appid=730&count=10&maxlength=5000&format=json"
    OFFICIAL_FEED_LABELS = frozenset(("Community Announcements", "steam_community_announcements"))
    OFFICIAL_URL_PREFIX = "https://steamstore-a.akamaihd.net/news/externalpost/steam_community_announcements/"
    source_language = "en"

    @property
    def source_name(self) -> str:
        """返回该来源在系统中的展示名称。"""
        return "Counter-Strike 2 官方公告（Steam）"

    def crawl(self, list_url: str) -> List[RawArticle]:
        """调用 Steam News API 抓取并过滤 CS2 第一方官方公告。

        仅保留 feedlabel 属于 Steam 社区公告且 URL 前缀匹配的条目，忽略第三方聚合内容。

        Args:
            list_url: Steam News API 的 JSON 接口地址。

        Returns:
            过滤后的 RawArticle 列表。
        """
        payload = json.loads(self.get_text(list_url))
        items = payload.get("appnews", {}).get("newsitems", [])
        articles = []
        for item in items:
            # Steam's app-news response mixes third-party feeds with Steam community announcements.
            # Only the first-party announcement feed may enter this official source.
            if item.get("feedlabel") not in self.OFFICIAL_FEED_LABELS:
                continue
            title = (item.get("title") or "").strip()
            contents = (item.get("contents") or "").strip()
            url = (item.get("url") or "").strip()
            timestamp = item.get("date")
            if not title or not contents or not url or not isinstance(timestamp, int) or not url.startswith(self.OFFICIAL_URL_PREFIX):
                continue
            articles.append(RawArticle(
                title=title,
                raw_html="<p>" + escape(contents).replace("\n", "<br/>") + "</p>",
                source=self.source_name,
                original_url=url,
                original_published_at=datetime.fromtimestamp(timestamp, tz=timezone.utc),
            ))
            if len(articles) >= settings.MAX_ARTICLES_PER_RUN:
                break
        print(f"[{self.source_name}] Steam News API 提取到 {len(articles)} 条第一方公告")
        return articles

    def fetch_urls(self, soup):
        """占位实现：本适配器走 JSON API，不解析 HTML 列表页。

        Args:
            soup: 未使用，保留以兼容基类接口。

        Returns:
            始终为空列表。
        """
        return []

    def parse(self, soup, url):
        """占位实现：本适配器走 JSON API，不解析 HTML 详情页。

        Args:
            soup: 未使用，保留以兼容基类接口。
            url: 未使用，保留以兼容基类接口。

        Raises:
            NotImplementedError: 本适配器不支持 HTML 解析路径。
        """
        raise NotImplementedError("SteamCs2OfficialSpider uses the Steam News JSON API")



