"""Abstract base spider — every website adapter must implement this interface."""
import json
import time
import random
import socket
import ipaddress
from urllib.parse import urlsplit, urljoin
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
        """初始化蜘蛛实例；HTTP 客户端惰性创建，初始为 None。"""
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
                follow_redirects=False,
                max_redirects=3,
            )
        return self._client

    MAX_RESPONSE_BYTES = 2_000_000

    # Extra first-party hosts a subclass may call besides its LIST_URL origin
    # (e.g. a JSON API on a sibling host). Empty by default: nothing is added
    # implicitly, and every extra host is still https-only and DNS-guarded.
    API_ORIGINS: frozenset[str] = frozenset()

    @staticmethod
    def _origin_parts(url: str):
        """拆分 URL 并校验其 scheme、主机与端口是否符合来源约束。

        Args:
            url: 待校验的 URL。

        Returns:
            解析后的 URL 分段结果。

        Raises:
            ValueError: URL 缺少协议/主机，或携带用户名密码，或使用非标准端口。
        """
        target = urlsplit(url)
        if (target.scheme not in ('http', 'https') or not target.hostname
                or target.username is not None or target.password is not None
                or target.port not in (None, 443 if target.scheme == 'https' else 80)):
            raise ValueError("URL is outside the configured source origin")
        return target

    def _allowed_origins(self) -> set[tuple[str, str]]:
        """返回允许的来源集合，含 LIST_URL 与 API_ORIGINS 中的额外主机。

        Raises:
            ValueError: 额外来源未使用 https。
        """
        configured = self._origin_parts(self.LIST_URL)
        origins = {(configured.scheme, configured.hostname)}
        for extra in self.API_ORIGINS:
            candidate = self._origin_parts(extra)
            if candidate.scheme != 'https':
                raise ValueError("extra source origin must use https")
            origins.add((candidate.scheme, candidate.hostname))
        return origins

    def validate_source_url(self, url: str) -> None:
        """校验 URL 属于允许来源，且协议不低于 LIST_URL 的协议。

        Args:
            url: 待校验的 URL。

        Raises:
            ValueError: URL 不在允许来源内，或降级为 http。
        """
        configured = self._origin_parts(self.LIST_URL)
        target = self._origin_parts(url)
        if (target.scheme, target.hostname) not in self._allowed_origins():
            raise ValueError("URL is outside the configured source origin")
        if configured.scheme == 'https' and target.scheme != 'https':
            raise ValueError("URL is outside the configured source origin")

    def _lookup_addresses(self, hostname: str, port: int) -> set[str]:
        """解析主机名对应的 IP 地址集合。

        Args:
            hostname: 目标主机名。
            port: 目标端口。

        Returns:
            解析得到的 IP 地址字符串集合。

        Raises:
            ValueError: 主机名解析失败。
        """
        try:
            return {item[4][0] for item in socket.getaddrinfo(hostname, port, type=socket.SOCK_STREAM)}
        except socket.gaierror as exc:
            raise ValueError("source hostname resolution failed") from exc

    def resolve_public_ips(self, hostname: str, port: int) -> set[str]:
        """Resolve every request hop and reject private/reserved addresses before transport."""
        addresses = self._lookup_addresses(hostname, port)
        if not addresses:
            raise ValueError("source hostname resolved to no addresses")
        for address in addresses:
            if not ipaddress.ip_address(address).is_global:
                raise ValueError("source hostname resolved to a non-public address")
        return addresses


    def get_text(self, url: str) -> str:
        """Validate each redirect before issuing another request; bound decoded text."""
        for hop in range(4):
            self.validate_source_url(url)
            target = urlsplit(url)
            self.resolve_public_ips(target.hostname, target.port or (443 if target.scheme == "https" else 80))
            with self.get_client().stream("GET", url, follow_redirects=False) as response:
                if response.status_code in (301, 302, 303, 307, 308):
                    if hop == 3 or not response.headers.get('location'):
                        raise ValueError("source redirect limit or missing destination")
                    url = urljoin(url, response.headers['location'])
                    continue
                response.raise_for_status()
                data = bytearray()
                for chunk in response.iter_bytes(chunk_size=16_384):
                    if len(data) + len(chunk) > self.MAX_RESPONSE_BYTES:
                        raise ValueError("source response exceeds decoded 2 MB limit")
                    data.extend(chunk)
                encoding = response.encoding or "utf-8"
            return bytes(data).decode(encoding, errors="replace")
        raise ValueError("source redirect limit")

    def get_soup(self, url: str) -> BeautifulSoup:
        """获取 URL 文本并用 lxml 解析为 BeautifulSoup。"""
        return BeautifulSoup(self.get_text(url), "lxml")

    def post_form_json(self, url: str, data: dict) -> dict:
        """POST a form body to a first-party JSON API.

        Applies the same source-origin validation, DNS guard and decoded-size
        bound as get_text. Needed by sources whose list/detail content is served
        only through a JSON endpoint instead of a rendered page.
        """
        self.validate_source_url(url)
        target = urlsplit(url)
        self.resolve_public_ips(target.hostname, target.port or (443 if target.scheme == "https" else 80))
        with self.get_client().stream("POST", url, data=data, follow_redirects=False) as response:
            response.raise_for_status()
            declared = response.headers.get("content-length")
            if declared is not None and declared.isdigit() and int(declared) > self.MAX_RESPONSE_BYTES:
                raise ValueError("source response exceeds decoded 2 MB limit")
            body = bytearray()
            for chunk in response.iter_bytes(chunk_size=16_384):
                if len(body) + len(chunk) > self.MAX_RESPONSE_BYTES:
                    raise ValueError("source response exceeds decoded 2 MB limit")
                body.extend(chunk)
            encoding = response.encoding or "utf-8"
        return json.loads(bytes(body).decode(encoding, errors="replace"))

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

