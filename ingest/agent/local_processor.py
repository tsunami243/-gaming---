"""Deterministic local processor — normalizes a RawArticle without any paid AI call.

This module is deliberately offline and side-effect free: no network, no API key,
no retry/fallback path that can leak the raw page. It replaces the DeepSeek step
for sources whose body markup is understood, and it never *accepts* an article by
itself — `content_type` is left unset so `filters.editorial.editorial_decision`
can only ever promote a prefilter ACCEPT when the deterministic rule set already
agrees. When the rule set is ambiguous the result stays REVIEW.
"""
import json
import re
from typing import Optional

from bs4 import BeautifulSoup, Comment

from models.article import RawArticle, ProcessedArticle, CATEGORY_MAP

# Tags that carry no editorial meaning for a summary/teaser feed.
_STRIP_TAGS = ("script", "style", "noscript", "iframe", "form", "svg", "button",
               "nav", "footer", "header", "aside", "video", "audio", "canvas")
# Ancestor class/id hints of layout chrome rather than body copy.
_CHROME_HINTS = ("cookie", "consent", "breadcrumb", "subscribe", "newsletter",
                 "related", "share", "social", "table of contents", "toc",
                 "nav", "advert", "banner", "promo", "footer", "header")
_TEXT_BLOCKS = ("p", "li", "h2", "h3", "h4")
MIN_CONTENT_CHARS = 50

# `category_id` is retained only because the legacy outbox payload still carries
# it; the authoritative fields are game_codes/content_type/decision.
_GAME_TO_CATEGORY = {name: cid for name, cid in CATEGORY_MAP.items()}


def _clean_text(node) -> str:
    """Collapse whitespace and drop zero-width/soft-hyphen noise from scraped copy."""
    text = node.get_text(" ", strip=True)
    text = text.replace("\u200b", "").replace("\u00ad", "").replace("\xa0", " ")
    return re.sub(r"\s+", " ", text).strip()


def _is_chrome(tag) -> bool:
    """True when a container's own class/id marks it as page furniture."""
    tokens = " ".join(tag.get("class") or []) + " " + (tag.get("id") or "")
    tokens = tokens.lower()
    return any(hint in tokens for hint in _CHROME_HINTS)


def extract_body_nodes(raw_html: str):
    """Return the ordered text-bearing elements of an article body.

    Keeps heading hierarchy (h2/h3/h4) and list items so the cleaned HTML still
    reads like the original document instead of one flattened paragraph.
    """
    soup = BeautifulSoup(raw_html or "", "lxml")
    for tag in soup(list(_STRIP_TAGS)):
        tag.decompose()
    for comment in soup.find_all(string=lambda value: isinstance(value, Comment)):
        comment.extract()
    # Drop nested navigation/TOC blocks that EA renders with a "toc" anchor.
    for heading in soup.select("h4[id='toc'], [class*='toc' i]"):
        heading.decompose()

    nodes = []
    for element in soup.find_all(_TEXT_BLOCKS):
        if element.find_parent(_TEXT_BLOCKS) is not None:
            continue  # only leaf blocks; a <li> inside <ul> counts once
        if any(_is_chrome(ancestor) for ancestor in element.parents if getattr(ancestor, "name", None)):
            continue
        text = _clean_text(element)
        if not text:
            continue
        nodes.append((element.name, text))

    if not nodes:
        fallback = _clean_text(soup)
        if fallback:
            nodes.append(("p", fallback))
    return nodes


def cleaned_text(raw_html: str) -> str:
    """抽取文章正文并拼接为纯文本，段落之间以换行分隔。

    Args:
        raw_html: 原始 HTML。

    Returns:
        清洗后的纯文本正文。
    """
    return "\n".join(text for _, text in extract_body_nodes(raw_html))


def cleaned_html(raw_html: str) -> str:
    """Minimal, class-free HTML — safe to store and render without page styling."""
    import html as _html
    parts = []
    for name, text in extract_body_nodes(raw_html):
        safe = _html.escape(text)
        parts.append(f"<{name}>{safe}</{name}>" if name in ("h2", "h3", "h4") else f"<p>{safe}</p>")
    return "".join(parts)


def build_summary(text: str, limit: int = 160) -> str:
    """Extractive teaser: the opening prose, never invented facts.

    Leading section headings ("INTRO", "TL;DR", "Table of Contents") are skipped —
    they are navigation, not a summary. Numbered list fragments are skipped too.
    """
    if not text:
        return ""
    paragraphs = [part.strip() for part in text.split("\n") if part.strip()]

    body = ""
    for part in paragraphs:
        candidate = re.sub(r"^[A-Z][A-Z0-9 :'’\-]{1,39}$", "", part).strip()
        # Skip bare headings and short list stubs such as "Conduit Refresh".
        if len(candidate) < 60 or re.match(r"^[A-Z][^.!?]{0,50}$", candidate):
            continue
        body = candidate
        break
    if not body:
        body = paragraphs[0] if paragraphs else text

    if len(body) <= limit:
        return body
    cut = body[:limit]
    boundary = max(cut.rfind(". "), cut.rfind("。"), cut.rfind("! "), cut.rfind("? "))
    if boundary >= limit // 2:
        return body[: boundary + 1].strip()
    return cut.rstrip() + "…"


def build_tags(prefilter, processed_games=None) -> list[str]:
    """Deterministic keyword tags: game label plus the classified content type."""
    from filters.fps_rules import GAME_ALIASES
    labels = {
        "delta-force": "三角洲行动",
        "valorant": "无畏契约",
        "cs2": "Counter-Strike 2",
        "apex-legends": "Apex Legends",
    }
    types = {
        "announcement": "官方公告",
        "patch_notes": "版本更新",
        "event": "活动",
        "new_content": "新内容",
        "guide": "攻略",
        "esports": "电竞赛事",
    }
    games = tuple(processed_games) if processed_games else prefilter.game_codes
    tags = [labels[code] for code in games if code in labels]
    if prefilter.content_type in types:
        tags.append(types[prefilter.content_type])
    return tags


def _category_id(game_codes) -> int:
    """按游戏编码映射到历史 category_id，未命中时返回 0。

    Args:
        game_codes: 游戏编码序列。

    Returns:
        对应的 category_id，未匹配到则为 0。
    """
    names = {
        "delta-force": "三角洲行动",
        "valorant": "无畏契约",
        "cs2": "Counter-Strike",
        "apex-legends": "Apex Legends",
    }
    for code in game_codes:
        cid = _GAME_TO_CATEGORY.get(names.get(code, ""))
        if cid:
            return cid
    return 0


class LocalArticleProcessor:
    """Offline stand-in for `DeepSeekAgent.process` with the same output contract."""

    source_language = "en"
    # Signals main.py to pass the deterministic prefilter decision into process().
    accepts_prefilter = True

    def process(self, raw: RawArticle, prefilter=None) -> ProcessedArticle:
        """离线处理 RawArticle，输出与 DeepSeekAgent 相同契约的 ProcessedArticle。

        Args:
            raw: 原始文章数据。
            prefilter: 可选的确定性预过滤结果；为 None 时在内部自行分类。

        Returns:
            本地规则处理后的文章结果。
        """
        text = cleaned_text(raw.raw_html)
        if prefilter is None:
            from filters.fps_rules import classify
            prefilter = classify(raw.title, raw.raw_html)

        usable = len(text) >= MIN_CONTENT_CHARS
        # A usable body inherits the deterministic rule verdict; an unusable body
        # is forced to REVIEW so it can never be auto-accepted on a stub.
        decision = prefilter.decision if (usable and prefilter.decision == "ACCEPT") else (
            "REJECT" if prefilter.decision == "REJECT" else "REVIEW")
        # The local processor observes no extra evidence beyond the deterministic
        # rule set, so it echoes the rule's own game and type verdict. That keeps
        # `editorial_decision` agreement-based: ACCEPT only when the rules already
        # reached ACCEPT, REVIEW whenever the rules were ambiguous.
        content_type = prefilter.content_type if (usable and prefilter.decision == "ACCEPT") else None

        summary = build_summary(text)
        return ProcessedArticle(
            title=raw.title,
            content=cleaned_html(raw.raw_html) if usable else "",
            category_id=_category_id(prefilter.game_codes),
            cover_img=raw.images[0] if raw.images else "",
            tags=json.dumps(build_tags(prefilter), ensure_ascii=False),
            summary=summary,
            source=raw.source,
            original_url=raw.original_url,
            crawled_at=raw.crawled_at.isoformat(),
            original_published_at=raw.original_published_at.isoformat() if raw.original_published_at else "",
            original_published_date=raw.original_published_date.isoformat() if raw.original_published_date else "",
            confidence=1.0 if usable else 0.0,
            is_gaming=True,
            decision=decision,
            game_codes=list(prefilter.game_codes),
            content_type=content_type,
            reason_code=None if decision == "ACCEPT" else "LOCAL_REVIEW",
        )
