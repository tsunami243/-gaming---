"""Official Apex Legends Global Series news adapter."""
from typing import List
from urllib.parse import urljoin

from models.article import RawArticle
from spiders.base import BaseSpider


class AlgsOfficialSpider(BaseSpider):
    """Apex Legends Global Series（ALGS）官方赛事资讯适配器。"""

    SOURCE_CODE = "algs-official"
    LIST_URL = "https://algs.ea.com/en/news"
    source_language = "en"

    @property
    def source_name(self) -> str:
        """返回该来源在系统中的展示名称。"""
        return "Apex Legends Global Series 官方赛事资讯"

    def fetch_urls(self, soup) -> List[str]:
        """从 ALGS 新闻列表页提取文章详情页链接。

        Args:
            soup: 列表页解析得到的 BeautifulSoup 对象。

        Returns:
            去重后的文章 URL 列表。
        """
        urls, seen = [], set()
        for anchor in soup.select('a[href^="/en/news/"]'):
            href = anchor.get("href", "")
            if href.rstrip("/") == "/en/news":
                continue
            url = urljoin(self.LIST_URL, href)
            if url not in seen:
                seen.add(url)
                urls.append(url)
        return urls

    def parse(self, soup, url: str) -> RawArticle:
        """解析 ALGS 文章详情页，抽取标题、正文与图片。

        Args:
            soup: 文章详情页解析得到的 BeautifulSoup 对象。
            url: 文章原始 URL。

        Returns:
            结构化后的 RawArticle 对象。

        Raises:
            ValueError: 未找到文章标题或正文时抛出。
        """
        title = soup.select_one("h1")
        content = next((node for node in soup.find_all("div")
                        if "max-w-[616px]" in (node.get("class") or [])
                        and len(node.get_text(" ", strip=True)) >= 500), None)
        if title is None or content is None:
            raise ValueError("ALGS article title or body not found")
        for node in content.select("script,style,iframe,nav"):
            node.decompose()
        return RawArticle(title=title.get_text(" ", strip=True), raw_html=str(content),
                          images=[urljoin(url, image.get("src")) for image in content.select("img[src]")],
                          source=self.source_name, original_url=url)
