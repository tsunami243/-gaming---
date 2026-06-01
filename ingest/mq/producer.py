"""Push ProcessedArticle to Redis List → Java consumer picks up."""
from storage.redis_client import RedisClient
from models.article import ProcessedArticle
from settings import settings


class QueueProducer:
    """Writes articles into the Redis queue (ingest:article_queue)."""

    def __init__(self, redis_client: RedisClient):
        self._redis = redis_client

    def enqueue(self, article: ProcessedArticle) -> int:
        """Push one article to queue. Returns new queue length."""
        payload = article.to_mq_json()
        length = self._redis._r.rpush(settings.REDIS_QUEUE_KEY, payload)
        self._redis.incr_stat("total_enqueued")
        print(f"  → 已入队 [{article.source}] {article.title[:40]}...  (队列长度 {length})")
        return length

    def enqueue_batch(self, articles: list[ProcessedArticle]) -> int:
        """Push multiple articles. Returns total enqueued count."""
        count = 0
        for article in articles:
            try:
                self.enqueue(article)
                count += 1
            except Exception as exc:
                print(f"  ✗ 入队失败: {exc}")
                self._redis.incr_stat("total_enqueue_errors")
        return count

    def queue_length(self) -> int:
        return self._redis._r.llen(settings.REDIS_QUEUE_KEY)
