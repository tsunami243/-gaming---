"""All configuration, loaded from environment variables with sensible defaults."""
import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    # ---- DeepSeek API ----
    DEEPSEEK_API_KEY: str = os.getenv("DEEPSEEK_API_KEY", "")
    DEEPSEEK_BASE_URL: str = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com")
    DEEPSEEK_MODEL: str = os.getenv("DEEPSEEK_MODEL", "deepseek-v4-flash")

    # ---- Redis ----
    REDIS_HOST: str = os.getenv("REDIS_HOST", "localhost")
    REDIS_PORT: int = int(os.getenv("REDIS_PORT", "6379"))
    REDIS_PASSWORD: str = os.getenv("REDIS_PASSWORD", "") or None
    REDIS_DB: int = int(os.getenv("REDIS_DB", "0"))

    # Redis keys
    REDIS_QUEUE_KEY: str = os.getenv("REDIS_QUEUE_KEY", "ingest:article_queue")
    REDIS_DEDUP_KEY: str = os.getenv("REDIS_DEDUP_KEY", "ingest:crawled_urls")
    REDIS_STATS_KEY: str = os.getenv("REDIS_STATS_KEY", "ingest:stats")

    # ---- Alibaba OSS ----
    OSS_ENDPOINT: str = os.getenv("OSS_ENDPOINT", "oss-cn-beijing.aliyuncs.com")
    OSS_ACCESS_KEY_ID: str = os.getenv("OSS_ACCESS_KEY_ID", "")
    OSS_ACCESS_KEY_SECRET: str = os.getenv("OSS_ACCESS_KEY_SECRET", "")
    OSS_BUCKET_NAME: str = os.getenv("OSS_BUCKET_NAME", "")
    OSS_ARTICLE_DIR: str = os.getenv("OSS_ARTICLE_DIR", "articles")

    # ---- Crawler ----
    CRAWL_DELAY_SECONDS: float = float(os.getenv("CRAWL_DELAY_SECONDS", "3"))
    MAX_ARTICLES_PER_RUN: int = int(os.getenv("MAX_ARTICLES_PER_RUN", "10"))


settings = Settings()
