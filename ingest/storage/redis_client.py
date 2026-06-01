"""Redis client — URL dedup, crawl stats, connection management."""
import hashlib
import redis
from redis import ConnectionPool
from settings import settings


class RedisClient:
    """Single Redis DB for three concerns: dedup, queue, stats."""

    def __init__(self):
        pool = ConnectionPool(
            host=settings.REDIS_HOST,
            port=settings.REDIS_PORT,
            password=settings.REDIS_PASSWORD,
            db=settings.REDIS_DB,
            decode_responses=True,
            max_connections=10,
            protocol=2,  # RESP2，兼容旧版 Redis (< 6.0)
        )
        self._r = redis.Redis(connection_pool=pool)

    # ============================================================
    # URL 去重 — Set
    # ============================================================

    def is_crawled(self, url: str) -> bool:
        """O(1) 检查 URL 是否已爬取。只存 MD5，不存完整 URL，节省内存。"""
        return bool(self._r.sismember(settings.REDIS_DEDUP_KEY, self._hash(url)))

    def mark_crawled(self, url: str) -> None:
        """标记 URL 为已爬取。"""
        self._r.sadd(settings.REDIS_DEDUP_KEY, self._hash(url))

    def dedup_size(self) -> int:
        """已爬取 URL 去重集合的大小。"""
        return self._r.scard(settings.REDIS_DEDUP_KEY)

    # ============================================================
    # 统计计数器 — Hash
    # ============================================================

    def incr_stat(self, field: str, amount: int = 1) -> int:
        """原子递增统计字段。字段名如 total_crawled, total_processed, total_errors。"""
        return self._r.hincrby(settings.REDIS_STATS_KEY, field, amount)

    def set_stat(self, field: str, value: str) -> None:
        self._r.hset(settings.REDIS_STATS_KEY, field, value)

    def get_stats(self) -> dict:
        return self._r.hgetall(settings.REDIS_STATS_KEY)

    # ============================================================
    # 内部工具
    # ============================================================

    @staticmethod
    def _hash(url: str) -> str:
        """URL → MD5 摘要，去重 Key 统一长度，避免 Set 中存超长 URL。"""
        return hashlib.md5(url.encode()).hexdigest()

    def ping(self) -> bool:
        return self._r.ping()
