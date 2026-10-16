"""All configuration, loaded from environment variables with sensible defaults."""
import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    """从环境变量加载全部运行配置并带有合理默认值。"""
    # ---- DeepSeek API ----
    DEEPSEEK_API_KEY: str = os.getenv("DEEPSEEK_API_KEY", "")
    DEEPSEEK_BASE_URL: str = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com")
    DEEPSEEK_MODEL: str = os.getenv("DEEPSEEK_MODEL", "deepseek-v4-flash")

    # ---- Community-source summary guard (disabled until B04 approval) ----
    # These limits are deliberately source-specific: community candidates may never
    # inherit the general agent retry/fallback behavior that can expose raw content.
    XIAOHEIHE_SUMMARY_ENABLED: bool = os.getenv("XIAOHEIHE_SUMMARY_ENABLED", "false") == "true"
    XIAOHEIHE_SUMMARY_DAILY_LIMIT: int = int(os.getenv("XIAOHEIHE_SUMMARY_DAILY_LIMIT", "20"))
    XIAOHEIHE_SUMMARY_INPUT_MAX_CHARS: int = int(os.getenv("XIAOHEIHE_SUMMARY_INPUT_MAX_CHARS", "1200"))
    XIAOHEIHE_SUMMARY_OUTPUT_MAX_CHARS: int = int(os.getenv("XIAOHEIHE_SUMMARY_OUTPUT_MAX_CHARS", "220"))
    XIAOHEIHE_SUMMARY_MAX_ATTEMPTS: int = int(os.getenv("XIAOHEIHE_SUMMARY_MAX_ATTEMPTS", "1"))

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
    MAX_CANDIDATES_PER_DAY: int = int(os.getenv("MAX_CANDIDATES_PER_DAY", "80"))
    RUNTIME_STATE_PATH: str = os.getenv("RUNTIME_STATE_PATH", "runtime/ingest_state.sqlite3")
    OUTBOX_PATH: str = os.getenv("OUTBOX_PATH", "runtime/outbox.sqlite3")
    MAX_OUTBOX_PENDING: int = int(os.getenv("MAX_OUTBOX_PENDING", "200"))
    MAX_RUNTIME_STORAGE_BYTES: int = int(os.getenv("MAX_RUNTIME_STORAGE_BYTES", "50000000"))
    CAPACITY_SNAPSHOT_PATH: str = os.getenv("CAPACITY_SNAPSHOT_PATH", "")
    DISK_WARN_BYTES: int = int(os.getenv("DISK_WARN_BYTES", "7000000000"))
    DISK_STOP_BYTES: int = int(os.getenv("DISK_STOP_BYTES", "8000000000"))
    DISK_HARD_BYTES: int = int(os.getenv("DISK_HARD_BYTES", "10000000000"))
    INGEST_INTERNAL_BASE_URL: str = os.getenv("INGEST_INTERNAL_BASE_URL", "")
    INGEST_SERVICE_TOKEN: str = os.getenv("INGEST_SERVICE_TOKEN", "")


settings = Settings()
