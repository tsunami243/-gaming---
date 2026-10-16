"""GameRes (游资网) spider adapter."""
from typing import List
from bs4 import BeautifulSoup
from spiders.base import BaseSpider
from models.article import RawArticle


class GameResSpider(BaseSpider):
    """游资网（GameRes）新闻抓取适配器。"""

    LIST_URL = "https://www.gameres.com/list/18"

    @property
    def source_name(self) -> str:
        """返回该来源在系统中的展示名称。"""
        return "游资网"

    # ============================================================
    # fetch_urls — 从列表页提取文章链接
    # ============================================================

    def fetch_urls(self, soup: BeautifulSoup) -> List[str]:
        """从游资网列表页按多组选择器提取文章链接并去重。

        Args:
            soup: 列表页解析得到的 BeautifulSoup 对象。

        Returns:
            去重后的文章 URL 列表。
        """
        urls = []
        # 游资网用 feed-item 模板，标题链接带 .feed-item-title-a
        selectors = [
            "article.feed-item a.feed-item-title-a",
            "a.feed-item-title-a",
            "[data-news-pane-id] a[href]",
        ]
        for sel in selectors:
            for a in soup.select(sel):
                href = a.get("href", "")
                if self._is_valid_article_url(href):
                    urls.append(self._abs_url(href))

        return list(dict.fromkeys(urls))

    # ============================================================
    # parse — 从文章页提取结构化数据
    # ============================================================

    def parse(self, soup: BeautifulSoup, url: str) -> RawArticle:
        """解析游资网文章详情页，抽取标题、正文与图片。

        Args:
            soup: 文章详情页解析得到的 BeautifulSoup 对象。
            url: 文章原始 URL。

        Returns:
            结构化后的 RawArticle 对象。
        """
        title = self._extract_title(soup)
        content_html, images = self._extract_content(soup)

        return RawArticle(
            title=title,
            raw_html=content_html,
            images=images,
            source=self.source_name,
            original_url=url,
        )

    # ============================================================
    # 内部提取方法
    # ============================================================

    def _extract_title(self, soup: BeautifulSoup) -> str:
        """按多组选择器依次尝试提取文章标题。

        Args:
            soup: 文章详情页解析得到的 BeautifulSoup 对象。

        Returns:
            标题文本；全部选择器均未命中时返回占位串 "(无标题)"。
        """
        for sel in ("h1", ".contentdiv h1", "title"):
            tag = soup.select_one(sel)
            if tag and tag.get_text(strip=True):
                return tag.get_text(strip=True)
        return "(无标题)"

    def _extract_content(self, soup: BeautifulSoup) -> tuple:
        """Return (content_html, image_urls)."""
        # 游资网正文容器 class = contentdiv
        content_div = soup.select_one(".contentdiv")
        if not content_div:
            return "", []

        images = []
        for img in content_div.select("img"):
            # 游资网用 data-original 做懒加载，src 是占位图
            src = img.get("data-original") or img.get("src") or ""
            if src:
                src = self._abs_url(src)
                images.append(src)

        return str(content_div), images

    # ============================================================
    # URL 工具
    # ============================================================

    @staticmethod
    def _is_valid_article_url(href: str) -> bool:
        """判断链接是否为游资网有效的文章详情页地址。

        Args:
            href: 待校验的原始链接。

        Returns:
            有效则返回 True，否则返回 False。
        """
        if not href:
            return False
        if href.startswith("javascript:") or href.startswith("#"):
            return False
        # 跳过 /wl?m=xxx 跳转链接，只抓 /xxxxxx.html 数字ID格式
        if href.startswith("/wl?"):
            return False
        return href.startswith("/") and href.endswith(".html")

    @staticmethod
    def _abs_url(href: str) -> str:
        """把相对或协议相对链接补全为游资网的绝对 URL。

        Args:
            href: 原始链接。

        Returns:
            补全后的绝对 URL。
        """
        if href.startswith("http"):
            return href
        if href.startswith("//"):
            return "https:" + href
        return "https://www.gameres.com" + href