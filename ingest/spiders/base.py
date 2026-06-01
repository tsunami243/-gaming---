"""Abstract base spider — every website adapter must implement this interface."""
import time
import random
from abc import ABC, abstractmethod
from typing import List, Optional
import httpx
from bs4 import BeautifulSoup
from settings import settings
from models.article import RawArticle


_USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:126.0) Gecko/20100101 Firefox/126.0",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
]


class BaseSpider(ABC):
    """Every website adapter inherits this.

    Subclass must implement:
        source_name   — human-readable source label
        fetch_urls    — scrape the index/list page, return article URLs
        parse         — extract title, body HTML, images from an article page
    """

    def __init__(self):
        self._client: Optional[httpx.Client] = None

    # ============================================================
    # 子类必须实现
    # ============================================================

    @property
    @abstractmethod
    def source_name(self) -> str:
        """来源网站名，如 '游民星空'"""
        ...

    @abstractmethod
    def fetch_urls(self, soup: BeautifulSoup) -> List[str]:
        """从列表页 HTML 提取文章 URL 列表。只返回绝对 URL。"""
        ...

    @abstractmethod
    def parse(self, soup: BeautifulSoup, url: str) -> RawArticle:
        """从文章页 HTML 解析标题、正文、图片。"""
        ...

    # ============================================================
    # 公共方法 — 子类直接复用
    # ============================================================

    def crawl(self, list_url: str) -> List[RawArticle]:
        """Full crawl pipeline for one list page.

        1. GET list_url → extract article URLs
        2. For each article URL → dedup check → GET → parse → RawArticle
        3. Respect CRAWL_DELAY_SECONDS between requests
        """
        articles: List[RawArticle] = []
        article_urls = self._get_urls_from_list(list_url)

        for url in article_urls[: settings.MAX_ARTICLES_PER_RUN]:
            try:
                article = self._fetch_one_article(url)
                articles.append(article)
                time.sleep(settings.CRAWL_DELAY_SECONDS)
            except Exception as exc:
                print(f"[{self.source_name}] 跳过 {url}: {exc}")
                continue

        return articles

    def get_client(self) -> httpx.Client:
        """Lazy init httpx client with random UA and timeout."""
        if self._client is None:
            self._client = httpx.Client(
                headers={"User-Agent": random.choice(_USER_AGENTS)},
                timeout=30,
                follow_redirects=True,
            )
        return self._client

    def get_soup(self, url: str) -> BeautifulSoup:
        """GET a URL, return parsed BeautifulSoup."""
        resp = self.get_client().get(url)
        resp.raise_for_status()
        # 强制 utf-8，避免 GBK 页面乱码
        if resp.encoding and resp.encoding.lower() != "utf-8":
            resp.encoding = "utf-8"
        return BeautifulSoup(resp.text, "lxml")

    # ============================================================
    # 内部
    # ============================================================

    def _get_urls_from_list(self, list_url: str) -> List[str]:
        """Fetch list page, extract article URLs via subclass hook."""
        soup = self.get_soup(list_url)
        urls = self.fetch_urls(soup)
        print(f"[{self.source_name}] 列表页提取到 {len(urls)} 个URL")
        return urls

    def _fetch_one_article(self, url: str) -> RawArticle:
        """Fetch + parse a single article. Raises on failure."""
        soup = self.get_soup(url)
        article = self.parse(soup, url)
        return article
