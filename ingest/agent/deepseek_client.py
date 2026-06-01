"""DeepSeek AI Agent — turns RawArticle into ProcessedArticle."""
import json
import time
from openai import OpenAI
from settings import settings
from models.article import RawArticle, ProcessedArticle, CATEGORY_MAP
from agent.prompts import SYSTEM_PROMPT, build_user_prompt


class DeepSeekAgent:
    """Calls DeepSeek API (OpenAI-compatible) to process a raw article."""

    def __init__(self):
        self._client = OpenAI(
            api_key=settings.DEEPSEEK_API_KEY,
            base_url=settings.DEEPSEEK_BASE_URL,
        )
        self._model = settings.DEEPSEEK_MODEL

    def process(self, raw: RawArticle) -> ProcessedArticle:
        """RawArticle → AI 处理 → ProcessedArticle.

        Retries once on network / JSON parse failure.
        """

        user_prompt = build_user_prompt(
            raw_html=raw.raw_html,
            source=raw.source,
            original_url=raw.original_url,
            category_map=CATEGORY_MAP,
        )

        for attempt in (1, 2):
            try:
                return self._call(raw, user_prompt)
            except Exception as exc:
                if attempt == 2:
                    # 最终失败 → 返回一个最低处理的版本（原始数据兜底）
                    return ProcessedArticle(
                        title=raw.title,
                        content=raw.raw_html,
                        category_id=0,
                        cover_img=raw.images[0] if raw.images else "",
                        tags="[]",
                        summary="",
                        source=raw.source,
                        original_url=raw.original_url,
                        crawled_at=raw.crawled_at.isoformat(),
                        confidence=0.0,
                    )
                print(f"[DeepSeek] 第 {attempt} 次调用失败: {exc}, 1秒后重试...")
                time.sleep(1)

    # ============================================================
    # 内部
    # ============================================================

    def _call(self, raw: RawArticle, user_prompt: str) -> ProcessedArticle:
        resp = self._client.chat.completions.create(
            model=self._model,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_prompt},
            ],
            temperature=0.3,   # 低温度 → 更稳定的结构化输出
            max_tokens=4096,
        )

        content = resp.choices[0].message.content
        data = self._parse_json(content)

        tags = data.get("tags", "[]")
        if isinstance(tags, list):
            tags = json.dumps(tags, ensure_ascii=False)  # DeepSeek 可能返回 list

        is_gaming = data.get("is_gaming", True)
        if isinstance(is_gaming, str):
            is_gaming = is_gaming.lower() in ("true", "yes", "1")

        return ProcessedArticle(
            title=data.get("title", raw.title),
            content=data.get("content", raw.raw_html),
            category_id=data.get("category_id") or 0,
            cover_img=data.get("cover_img", ""),
            tags=tags,
            summary=data.get("summary", ""),
            source=raw.source,
            original_url=raw.original_url,
            crawled_at=raw.crawled_at.isoformat(),
            confidence=float(data.get("confidence", 0.5)),
            is_gaming=bool(is_gaming),
        )

    @staticmethod
    def _parse_json(raw: str) -> dict:
        """Parse AI response, handling markdown code block wrapping."""
        text = raw.strip()
        # 去掉可能包裹的 ```json ... ``` 标记
        if text.startswith("```"):
            text = text.split("\n", 1)[-1]
            if text.endswith("```"):
                text = text[:-3]
        return json.loads(text)
