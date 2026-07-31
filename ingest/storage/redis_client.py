"""Redis client — URL dedup, crawl stats, connection management. Degradation supported."""
import hashlib
import redis
from redis import ConnectionPool
from settings import settings


class RedisClient:
    """Single Redis DB for three concerns: dedup, queue, stats.

    降级模式: 如果初始化时Redis不可用, 所有方法变成 no-op, 不抛异常。
    这样爬虫主流程不会被Redis故障影响, 只是在统计和去重上有数据丢失风险。
    """

    def __init__(self):
        self._available = False
        self._r = None
        try:
            pool = ConnectionPool(
                host=settings.REDIS_HOST,
                port=settings.REDIS_PORT,
                password=settings.REDIS_PASSWORD,
                db=settings.REDIS_DB,
                decode_responses=True,
                max_connections=10,
                protocol=2,  # RESP2，兼容旧版 Redis (< 6.0)
                socket_connect_timeout=2,  # 2秒连不上就放弃
            )
            self._r = redis.Redis(connection_pool=pool)
            self._r.ping()  # 主动验证
            self._available = True
        except Exception as e:
            print(f"WARN: Redis初始化失败, 进入降级模式: {e}")
            self._available = False

    @property
    def available(self) -> bool:
        return self._available

    # ============================================================
    # URL 去重 — Set
    # ============================================================

    def is_crawled(self, url: str) -> bool:
        """O(1) 检查 URL 是否已爬取。降级模式下永远返回False(认为没爬过)。"""
        if not self._available:
            return False
        try:
            return bool(self._r.sismember(settings.REDIS_DEDUP_KEY, self._hash(url)))
        except Exception:
            return False

    def mark_crawled(self, url: str) -> None:
        """标记 URL 为已爬取。降级模式下静默跳过。"""
        if not self._available:
            return
        try:
            self._r.sadd(settings.REDIS_DEDUP_KEY, self._hash(url))
        except Exception:
            pass

    def dedup_size(self) -> int:
        """已爬取 URL 去重集合的大小。降级模式返回0。"""
        if not self._available:
            return 0
        try:
            return self._r.scard(settings.REDIS_DEDUP_KEY)
        except Exception:
            return 0

    # ============================================================
    # 统计计数器 — Hash
    # ============================================================

    def incr_stat(self, field: str, amount: int = 1) -> int:
        """原子递增统计字段。降级模式静默跳过返回0。"""
        if not self._available:
            return 0
        try:
            return self._r.hincrby(settings.REDIS_STATS_KEY, field, amount)
        except Exception:
            return 0

    def set_stat(self, field: str, value: str) -> None:
        if not self._available:
            return
        try:
            self._r.hset(settings.REDIS_STATS_KEY, field, value)
        except Exception:
            pass

    def get_stats(self) -> dict:
        if not self._available:
            return {}
        try:
            return self._r.hgetall(settings.REDIS_STATS_KEY)
        except Exception:
            return {}

    # ============================================================
    # 内部工具
    # ============================================================

    @staticmethod
    def _hash(url: str) -> str:
        """URL → MD5 摘要，去重 Key 统一长度，避免 Set 中存超长 URL。"""
        return hashlib.md5(url.encode()).hexdigest()

    def ping(self) -> bool:
        if not self._available:
            return False
        try:
            return self._r.ping()
        except Exception:
            return False
