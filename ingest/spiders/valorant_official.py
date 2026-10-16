"""Official VALORANT game-update adapter."""
from datetime import datetime
from typing import List
from urllib.parse import urljoin

from bs4 import BeautifulSoup
from models.article import RawArticle
from spiders.base import BaseSpider

class ValorantOfficialSpider(BaseSpider):
    """VALORANT 官方游戏更新公告适配器。"""

    SOURCE_CODE = "valorant-official"
    LIST_URL = "https://playvalorant.com/en-us/news/game-updates/"
    source_language = "en"

    @property
    def source_name(self) -> str:
        """返回该来源在系统中的展示名称。"""
        return "VALORANT 官方更新公告"

    def fetch_urls(self, soup: BeautifulSoup) -> List[str]:
        """从 VALORANT 官方更新列表页提取文章详情页链接。

        Args:
            soup: 列表页解析得到的 BeautifulSoup 对象。

        Returns:
            去重后的文章 URL 列表。
        """
        urls = []
        for anchor in soup.select('a[href*="/en-us/news/game-updates/"]'):
            href = anchor.get("href", "")
            if "/en-us/news/game-updates/" in href and href.rstrip("/") != "/en-us/news/game-updates":
                urls.append(urljoin(self.LIST_URL, href))
        return list(dict.fromkeys(urls))

    def parse(self, soup: BeautifulSoup, url: str) -> RawArticle:
        """解析 VALORANT 官方文章详情页，抽取标题、正文、图片与发布时间。

        Args:
            soup: 文章详情页解析得到的 BeautifulSoup 对象。
            url: 文章原始 URL。

        Returns:
            结构化后的 RawArticle 对象。

        Raises:
            ValueError: 未找到文章正文时抛出。
        """
        title_tag = soup.select_one("main h1") or soup.select_one("h1") or soup.select_one("title")
        title = title_tag.get_text(" ", strip=True) if title_tag else ""
        content_candidates = soup.select("main .richText")
        content = max(content_candidates, key=lambda item: len(item.get_text(" ", strip=True)), default=None)
        if content is None:
            raise ValueError("official VALORANT article body not found")
        for node in content.select("script, style, iframe"):
            node.decompose()
        time_tag = soup.select_one("main time[datetime]") or soup.select_one("time[datetime]")
        published_at = datetime.fromisoformat(time_tag["datetime"].replace("Z", "+00:00")) if time_tag else None
        images = [urljoin(url, image.get("src")) for image in content.select("img[src]")]
        return RawArticle(title=title, raw_html=str(content), images=images, source=self.source_name,
                          original_url=url, original_published_at=published_at)
