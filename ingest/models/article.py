"""Data models shared across the ingestion pipeline."""
from datetime import date, datetime, timezone
from typing import List, Optional, Literal
from pydantic import BaseModel, Field

class RawArticle(BaseModel):
    """Output of a source adapter."""
    title: str
    raw_html: str
    images: List[str] = Field(default_factory=list)
    source: str
    original_url: str
    crawled_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    original_published_at: Optional[datetime] = None
    original_published_date: Optional[date] = None

class ProcessedArticle(BaseModel):
    """Output of the editorial processor, ready for the durable outbox."""
    title: str
    content: str
    category_id: int
    cover_img: str
    tags: str = "[]"
    summary: str = ""
    source: str
    original_url: str
    crawled_at: str = ""
    original_published_at: str = ""
    original_published_date: str = ""
    confidence: float = Field(default=1.0, ge=0.0, le=1.0)
    is_gaming: bool = True
    decision: Literal['ACCEPT', 'REVIEW', 'REJECT'] = 'REVIEW'
    game_codes: List[str] = Field(default_factory=list)
    content_type: Optional[str] = None
    reason_code: Optional[str] = None

    def to_mq_json(self) -> str:
        """将文章模型序列化为用于 MQ 投递的 JSON 字符串。

        Returns:
            序列化后的 JSON 字符串。
        """
        return self.model_dump_json()

CATEGORY_MAP = {
    "三角洲行动": 5,
    "无畏契约": 8,
    "Counter-Strike": 9,
    "Apex Legends": 10,
}
