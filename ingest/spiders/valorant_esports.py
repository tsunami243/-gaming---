"""Official VALORANT Esports news adapter."""
from datetime import datetime
from typing import List
from urllib.parse import urljoin

from models.article import RawArticle
from spiders.base import BaseSpider


class ValorantEsportsOfficialSpider(BaseSpider):
    """VALORANT Esports 官方赛事资讯适配器。"""

    SOURCE_CODE = "valorant-esports-official"
    LIST_URL = "https://valorantesports.com/en-US/news/"
    source_language = "en"

    @property
    def source_name(self) -> str:
        """返回该来源在系统中的展示名称。"""
        return "VALORANT Esports 官方赛事资讯"

    def fetch_urls(self, soup) -> List[str]:
        """从 VALORANT Esports 新闻列表页提取文章详情页链接。

        Args:
            soup: 列表页解析得到的 BeautifulSoup 对象。

        Returns:
            去重后的文章 URL 列表。
        """
        urls, seen = [], set()
        for anchor in soup.select('a[href^="/news/"]'):
            href = anchor.get("href", "")
            if href.rstrip("/") == "/news":
                continue
            url = urljoin(self.LIST_URL, href)
            if url not in seen:
                seen.add(url)
                urls.append(url)
        return urls

    def parse(self, soup, url: str) -> RawArticle:
        """解析 VALORANT Esports 文章详情页，抽取标题、正文、图片与发布时间。

        Args:
            soup: 文章详情页解析得到的 BeautifulSoup 对象。
            url: 文章原始 URL。

        Returns:
            结构化后的 RawArticle 对象。

        Raises:
            ValueError: 未找到文章标题或正文时抛出。
        """
        title_node = soup.select_one("h1")
        content = next((node for node in soup.find_all("div")
                        if "flex_1_1_100%" in (node.get("class") or [])
                        and len(node.get_text(" ", strip=True)) >= 100), None)
        time_node = soup.select_one("time")
        if title_node is None or content is None:
            raise ValueError("VALORANT Esports article title or body not found")
        for node in content.select("script,style,iframe,nav"):
            node.decompose()
        published_date = None
        if time_node and time_node.get_text(" ", strip=True):
            try:
                published_date = datetime.strptime(time_node.get_text(" ", strip=True), "%B %d, %Y").date()
            except ValueError:
                pass
        return RawArticle(
            title=title_node.get_text(" ", strip=True), raw_html=str(content),
            images=[urljoin(url, image.get("src")) for image in content.select("img[src]")],
            source=self.source_name, original_url=url, original_published_date=published_date,
        )


