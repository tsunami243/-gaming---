"""Data models shared across the ingestion pipeline."""
from datetime import datetime, timezone
from typing import List, Optional
from pydantic import BaseModel, Field


class RawArticle(BaseModel):
    """Output of a Spider — raw data extracted from a web page."""

    title: str = Field(..., description="原始标题")
    raw_html: str = Field(..., description="正文HTML（未清洗）")
    images: List[str] = Field(default_factory=list, description="正文中所有图片URL")
    source: str = Field(..., description="来源网站名，如 游民星空")
    original_url: str = Field(..., description="原文链接")
    crawled_at: datetime = Field(
        default_factory=lambda: datetime.now(timezone.utc),
        description="爬取时间",
    )


class ProcessedArticle(BaseModel):
    """Output of the AI Agent — cleaned, classified, ready for MQ/API."""

    title: str = Field(..., description="清洗后的标题")
    content: str = Field(..., description="清洗后的HTML正文（图片URL已替换为OSS地址）")
    category_id: int = Field(..., description="分类ID（匹配DB category表）")
    cover_img: str = Field(..., description="封面图OSS URL")
    tags: str = Field(default="[]", description='JSON数组字符串，如 ["标签1","标签2"]')
    summary: str = Field(default="", description="100字以内摘要")
    source: str = Field(..., description="来源网站名")
    original_url: str = Field(..., description="原文链接")
    crawled_at: str = Field(default="", description="ISO格式爬取时间")
    confidence: float = Field(default=1.0, ge=0.0, le=1.0, description="AI置信度")
    is_gaming: bool = Field(default=True, description="是否为游戏相关文章")

    def to_mq_json(self) -> str:
        """Serialise to JSON for RocketMQ / Redis queue."""
        return self.model_dump_json()


# Mapping: category alias → DB category id
CATEGORY_MAP = {
    "三角洲行动": 5,
    "无畏契约": 8,
    "Counter-Strike": 9,
    "Apex Legends": 10,
}
