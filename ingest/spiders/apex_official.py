"""EA official Apex Legends news adapter."""
from datetime import datetime
from typing import List
import re
import time
from urllib.parse import urljoin
from bs4 import BeautifulSoup
from models.article import RawArticle
from settings import settings
from spiders.base import BaseSpider

class ApexOfficialSpider(BaseSpider):
    """EA 官方 Apex Legends 新闻公告适配器。"""

    SOURCE_CODE = "apex-official"
    LIST_URL = "https://www.ea.com/games/apex-legends/news"
    source_language = "en"
    @property
    def source_name(self):
        """返回该来源在系统中的展示名称。"""
        return "Apex Legends 官方公告（EA）"

    def fetch_urls(self, soup: BeautifulSoup) -> List[str]:
        """从 EA 新闻列表页提取 Apex Legends 文章详情页链接。

        Args:
            soup: 列表页解析得到的 BeautifulSoup 对象。

        Returns:
            去重后的文章 URL 列表。
        """
        urls=[]
        for anchor in soup.select('a[href*="/games/apex-legends/apex-legends/news/"]'):
            href=anchor.get('href','')
            if href.rstrip('/').endswith('/news'): continue
            urls.append(urljoin(self.LIST_URL,href))
        return list(dict.fromkeys(urls))

    def crawl(self, list_url: str) -> List[RawArticle]:
        """抓取 EA Apex Legends 新闻列表并按详情页逐篇解析。

        会从列表卡片中解析发布日期并回填到文章对象上，按抓取延迟逐篇请求。

        Args:
            list_url: 新闻列表页 URL。

        Returns:
            本次抓取到的 RawArticle 列表。
        """
        list_soup = self.get_soup(list_url)
        published = {}
        for anchor in list_soup.select('a[href*="/games/apex-legends/apex-legends/news/"]'):
            href = anchor.get('href', '')
            url = urljoin(self.LIST_URL, href)
            match = re.search(r'(January|February|March|April|May|June|July|August|September|October|November|December) \d{1,2}, \d{4}', anchor.get_text(' ', strip=True))
            if match:
                published[url] = datetime.strptime(match.group(0), '%B %d, %Y').date()
        articles = []
        for url in self.fetch_urls(list_soup)[:settings.MAX_ARTICLES_PER_RUN]:
            article = self._fetch_one_article(url)
            article.original_published_date = published.get(url)
            articles.append(article)
            time.sleep(settings.CRAWL_DELAY_SECONDS)
        return articles

    def parse(self,soup:BeautifulSoup,url:str)->RawArticle:
        """解析 EA Apex Legends 文章详情页，抽取标题、正文与图片。

        Args:
            soup: 文章详情页解析得到的 BeautifulSoup 对象。
            url: 文章原始 URL。

        Returns:
            结构化后的 RawArticle 对象。

        Raises:
            ValueError: 未找到文章正文时抛出。
        """
        title=(soup.select_one('main h1') or soup.select_one('h1')).get_text(' ',strip=True)
        sections=soup.select('main section')
        content=max(sections,key=lambda item:len(item.get_text(' ',strip=True)),default=None)
        if content is None: raise ValueError('EA article body not found')
        for node in content.select('script,style,iframe,nav') : node.decompose()
        # EA card listing carries the publication date; detail pages do not reliably expose one.
        return RawArticle(title=title,raw_html=str(content),images=[urljoin(url,img.get('src')) for img in content.select('img[src]')],source=self.source_name,original_url=url,original_published_at=None)
