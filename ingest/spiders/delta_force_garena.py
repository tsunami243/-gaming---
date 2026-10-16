"""三角洲行动国服官网公告（df.qq.com）适配器。

列表页 https://df.qq.com/cp/a20240906main/newslist.html 与详情页
newsdetail.html 都是 JS 壳，列表与正文由页面内联脚本通过 Milo flow 接口
（flow_1073310 列表、flow_1073326 详情）异步拉取，普通 GET 拿不到内容。
本适配器直接请求同一批第一方 JSON 接口，不引入 headless browser。

SOURCE_CODE 保持 `delta-force-garena-official` 不变：数据库 content_source
种子（V8 迁移）、后端来源策略与 Java 迁移测试都以该 code 为准。本轮只更换
采集入口，不改来源标识，避免破坏既有发布门禁。
"""
import random
import re
import string
import time
from datetime import datetime
from html import unescape
from typing import List
from urllib.parse import urljoin

from models.article import RawArticle
from spiders.base import BaseSpider
from settings import settings


class DeltaForceGarenaOfficialSpider(BaseSpider):
    """三角洲行动国服（腾讯）官网公告；列表与详情均取第一方 flow JSON。"""

    SOURCE_CODE = "delta-force-garena-official"

    # 对外页面入口（人工核对来源用）；取数走下面的 flow 接口。
    LIST_URL = "https://df.qq.com/cp/a20240906main/newslist.html"

    # flow POST 端点。与页面的取数地址一致，是该来源唯一的数据入口。
    FLOW_API_URL = "https://comm.ams.game.qq.com/ide/"
    # 取数接口与页面不同源，显式登记为额外允许来源；仍然只允许 https。
    API_ORIGINS = frozenset({"https://comm.ams.game.qq.com/"})

    ACTIVITY_ID = "670608"
    SERVICE_TYPE = "dfm"
    NEWS_LIST_CHART_ID = "329849"     # flow_1073310 交叉搜索
    NEWS_LIST_IDE_TOKEN = "o3Iw53"
    NEWS_DETAIL_CHART_ID = "329870"   # flow_1073326 根据内容 id 获取详情
    NEWS_DETAIL_IDE_TOKEN = "vDQriw"
    # 列表页栏目（index.js NewsArr：1=最新 6895、2=公告 6896、3=新闻 6898、4=赛事 7037）。
    # 已由产品确认：**只锁定「新闻」单一栏目**，不采集「最新」/「公告」/「赛事」；
    # 其余 id 仅保留作对照，不参与取数。
    NEWS_CHANNEL_ID = "6898"

    ARTICLE_URL_PREFIX = "https://df.qq.com/cp/a20240906main/newsdetail.html?id="

    source_language = "zh-CN"

    @property
    def source_name(self) -> str:
        """来源网站名：三角洲行动官方公告（国服官网）。"""
        return "三角洲行动官方公告（国服官网）"

    # ============================================================
    # 采集
    # ============================================================

    def crawl(self, list_url: str) -> List[RawArticle]:
        """采集流程：拉取 flow 列表并逐条抓取详情，单篇失败跳过。

        Args:
            list_url: 兼容 BaseSpider 接口的列表页地址，此处仅作占位不使用。

        Returns:
            成功解析出的文章列表。
        """
        items = self.fetch_news_items()
        articles: List[RawArticle] = []
        for item in items[: settings.MAX_ARTICLES_PER_RUN]:
            doc_id = self._text(item.get("iDocID"))
            title = self._text(item.get("sTitle"))
            if not doc_id or not title:
                continue
            try:
                article = self.fetch_article(doc_id, item)
            except Exception as exc:  # 单篇失败不影响整轮
                print(f"[{self.source_name}] 跳过 {title[:40]}: {exc}")
                continue
            articles.append(article)
            time.sleep(settings.CRAWL_DELAY_SECONDS)
        print(f"[{self.source_name}] 列表接口返回 {len(items)} 条，成功解析 {len(articles)} 篇")
        return articles

    def fetch_news_items(self) -> List[dict]:
        """请求 flow 列表接口并返回条目字典列表。

        Returns:
            列表接口返回的条目字典列表。

        Raises:
            ValueError: 响应中缺少 items 数组。
        """
        payload = self.post_form_json(self.FLOW_API_URL, self._news_list_payload())
        data = self._flow_data(payload)
        if not isinstance(data, dict) or not isinstance(data.get("items"), list):
            raise ValueError("Delta Force news list response has no item array")
        return [item for item in data["items"] if isinstance(item, dict)]

    def fetch_article(self, doc_id: str, list_item: dict | None = None) -> RawArticle:
        """按文档 id 请求 flow 详情接口并构造 RawArticle。

        Args:
            doc_id: 文章文档 id。
            list_item: 列表接口中的对应条目，用作补充字段。

        Returns:
            解析后的文章对象。

        Raises:
            ValueError: 详情响应中缺少记录。
        """
        payload = self.post_form_json(self.FLOW_API_URL, self._news_detail_payload(doc_id))
        records = self._flow_data(payload)
        if not isinstance(records, list) or not records or not isinstance(records[0], dict):
            raise ValueError("Delta Force news detail response has no record")
        return self._build_article(records[0], list_item or {})

    # ============================================================
    # 解析
    # ============================================================

    def _build_article(self, record: dict, list_item: dict) -> RawArticle:
        """由详情记录（列表项作补充）构造 RawArticle，并清理正文脚本标签。

        Args:
            record: flow 详情接口返回的记录。
            list_item: 列表接口返回的对应条目，用于兜底取字段。

        Returns:
            解析后的文章对象。

        Raises:
            ValueError: 标题或正文缺失。
        """
        title = self._text(record.get("sTitle")) or self._text(list_item.get("sTitle"))
        if not title:
            raise ValueError("Delta Force article title not found")
        body = self._text(record.get("sContent"))
        if not body:
            raise ValueError("Delta Force article body not found")
        for tag in ("script", "style", "iframe"):
            body = re.sub(fr"<{tag}\b.*?</{tag}>", "", body, flags=re.IGNORECASE | re.DOTALL)
        return RawArticle(
            title=title,
            raw_html=body,
            images=self._image_urls(body),
            source=self.source_name,
            original_url=self._resolve_article_url(record, list_item),
            original_published_date=self._published_date(record, list_item),
        )

    def _resolve_article_url(self, record: dict, list_item: dict) -> str:
        """确定原文链接：优先取跳转地址，否则用文档 id 拼接。

        Raises:
            ValueError: 记录与列表项都没有文档 id。
        """
        for candidate in (self._text(record.get("sRedirectURL")), self._text(list_item.get("sRedirectURL"))):
            if candidate.startswith(self.ARTICLE_URL_PREFIX):
                return candidate
        doc_id = self._text(record.get("iDocID")) or self._text(list_item.get("iDocID"))
        if not doc_id:
            raise ValueError("Delta Force article has no document id")
        return f"{self.ARTICLE_URL_PREFIX}{doc_id}"

    def _published_date(self, record: dict, list_item: dict):
        """从记录或列表项解析发布日期，均无法解析时返回 None。"""
        for value in (record.get("sIdxTime"), list_item.get("sIdxTime")):
            text = self._text(value)
            if not text:
                continue
            try:
                return datetime.strptime(text, "%Y-%m-%d %H:%M:%S").date()
            except ValueError:
                continue
        return None

    def _image_urls(self, body: str) -> List[str]:
        """从正文 HTML 中提取去重后的绝对图片 URL 列表。"""
        images: List[str] = []
        seen = set()
        for match in re.finditer(r"""<img\b[^>]*?\bsrc\s*=\s*["']([^"']+)["']""", body, re.IGNORECASE):
            url = self._absolute_image(match.group(1))
            if url and url not in seen:
                seen.add(url)
                images.append(url)
        return images

    @staticmethod
    def _absolute_image(src: str) -> str:
        """把图片 src 规范为绝对 https URL，空值返回空串。"""
        candidate = unescape(src).strip()
        if not candidate:
            return ""
        if candidate.startswith("//"):
            return "https:" + candidate
        if candidate.startswith(("http://", "https://")):
            return candidate
        return urljoin("https://df.qq.com/", candidate)

    @staticmethod
    def _text(value) -> str:
        """把任意值反转义并去首尾空白；None 返回空串。"""
        return unescape(str(value)).strip() if value is not None else ""

    # ============================================================
    # flow 请求
    # ============================================================

    def _news_list_payload(self) -> dict:
        """构造 flow 列表接口的请求体。"""
        return self._flow_payload(self.NEWS_LIST_CHART_ID, self.NEWS_LIST_IDE_TOKEN, {
            "chanid": self.NEWS_CHANNEL_ID,
            "typeids": "1",  # 1：资讯
            "limit": str(max(1, settings.MAX_ARTICLES_PER_RUN)),
            "start": "0",
        })

    def _news_detail_payload(self, doc_id: str) -> dict:
        """构造指定文档 id 的 flow 详情接口请求体。"""
        return self._flow_payload(self.NEWS_DETAIL_CHART_ID, self.NEWS_DETAIL_IDE_TOKEN, {
            "id": str(doc_id),
        })

    def _serial_num(self) -> str:
        """生成带时间戳与随机后缀的 flow 请求序列号。"""
        stamp = datetime.now().strftime("%m%d%H%M%S")
        suffix = "".join(random.choice(string.ascii_letters + string.digits) for _ in range(6))
        return f"AMS_{self.SERVICE_TYPE}_{stamp}_{suffix}"

    def _flow_payload(self, chart_id: str, ide_token: str, service_data: dict) -> dict:
        """组装 flow 接口的通用请求体，并合并业务字段。

        Args:
            chart_id: flow 图表 id（同时用作子图表 id）。
            ide_token: flow 接口的 ide token。
            service_data: 具体业务字段，将覆盖到通用字段之上。

        Returns:
            完整的 flow 请求体字典。
        """
        payload = {
            "iChartId": chart_id,
            "iSubChartId": chart_id,
            "sIdeToken": ide_token,
            "e_code": "0",
            "g_code": "0",
            "eas_url": self.LIST_URL.replace(":", "%3A").replace("/", "%2F"),
            "eas_refer": "",
            "sMiloTag": self._serial_num(),
            # 活动账号类型为 7（预注册），官方前端固定补这两个字段。
            "isPreengage": "1",
            "needGopenid": "1",
        }
        payload.update(service_data)
        return payload

    @staticmethod
    def _flow_data(payload: dict):
        """取出 flow 响应中的业务数据，并区分接口错误与业务错误。"""
        if payload.get("iRet") != 0:
            raise ValueError(f"Delta Force flow API rejected the request: {payload.get('sMsg')!r}")
        data = payload.get("jData")
        if isinstance(data, dict):
            if data.get("iRet") not in (0, "0", None):
                raise ValueError(f"Delta Force flow payload error: {data.get('sMsg')!r}")
            data = data.get("data")
        return data

    # ============================================================
    # BaseSpider 接口占位
    # ============================================================

    def fetch_urls(self, soup) -> List[str]:
        """占位实现：本适配器不走列表页 HTML，恒定返回空列表。"""
        return []

    def parse(self, soup, url: str) -> RawArticle:
        """占位实现：本适配器不走文章页 HTML，直接抛 NotImplementedError。"""
        raise NotImplementedError("DeltaForceGarenaOfficialSpider uses the first-party flow JSON API")
