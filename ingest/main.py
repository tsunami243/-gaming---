"""Entry point — wired for Linux Cron.

Usage:
    python main.py                     # full pipeline (dry-run safe: skips AI without API key)
    python main.py --source gamersky   # only crawl one source
    python main.py --dry-run           # crawl only, skip AI processing
    python main.py --stats             # just print Redis stats
"""
import asyncio
import sys
import hashlib
import click
from settings import settings
from storage.redis_client import RedisClient
from storage.oss_client import OssClient
from mq.producer import QueueProducer
from agent.deepseek_client import DeepSeekAgent
from spiders.gamersky import GamerSkySpider
from spiders.gameres import GameResSpider
from models.article import ProcessedArticle


def process_one_source(name, spider, agent, oss, producer, redis, dry_run):
    """处理单个爬虫源:爬取 → AI处理 → 入队。同步函数,会被to_thread包装。"""
    crawled = 0
    enqueued = 0

    print(f"\n--- [{spider.source_name}] 开始爬取 ---")
    try:
        raw_articles = spider.crawl(spider.LIST_URL)
    except Exception as e:
        print(f"ERROR crawling {name}: {e}")
        redis.incr_stat("total_errors")
        return 0, 0

    for raw in raw_articles:
        if redis.is_crawled(raw.original_url):
            print(f"  ⏭ 跳过 (已爬过): {raw.title[:40]}")
            redis.incr_stat("total_skipped")
            continue

        redis.mark_crawled(raw.original_url)
        crawled += 1
        redis.incr_stat("total_crawled")
        print(f"\n  [{crawled}] {raw.title}")

        if dry_run:
            print(f"      (dry-run — skip AI/enqueue)")
            continue

        processed = agent.process(raw)
        print(f"      AI → cat={processed.category_id}  conf={processed.confidence:.0%}  gaming={processed.is_gaming}")

        if processed.confidence < 0.2:
            processed.is_gaming = False

        if not processed.is_gaming:
            print(f"      ✗ 已过滤 (非游戏内容)")
            redis.incr_stat("total_filtered")
            continue

        article_dir = hashlib.md5(raw.original_url.encode()).hexdigest()[:8]
        new_content, new_cover = oss.upload_article_images(
            processed.content, processed.cover_img, article_dir
        )
        processed.content = new_content
        processed.cover_img = new_cover

        if not processed.title or not processed.title.strip():
            print(f"      ✗ 已过滤 (空标题)")
            redis.incr_stat("total_filtered")
            continue
        if not processed.content or len(processed.content.strip()) < 50:
            print(f"      ✗ 已过滤 (内容过短)")
            redis.incr_stat("total_filtered")
            continue

        producer.enqueue(processed)
        enqueued += 1

    return crawled, enqueued
#
async def run_sources_concurrently(spiders, agent, oss, producer, redis, dry_run):
    """多源并发执行。用 asyncio.to_thread 把同步函数包装成协程,gather 并发跑。"""
    tasks = []
    for name, spider in spiders.items():
        # 关键:to_thread把同步process_one_source包成协程
        task = asyncio.to_thread(
            process_one_source, name, spider, agent, oss, producer, redis, dry_run
        )
        tasks.append(task)

    # gather并发跑所有任务,等所有完成
    results = await asyncio.gather(*tasks, return_exceptions=True)

    total_crawled = 0
    total_enqueued = 0
    for i, result in enumerate(results):
        if isinstance(result, Exception):
            print(f"ERROR in source {list(spiders.keys())[i]}: {result}")
            continue
        c, e = result
        total_crawled += c
        total_enqueued += e
    return total_crawled, total_enqueued


@click.command()
@click.option("--source", default=None, help="Only crawl this source (e.g. 'gamersky')")
@click.option("--dry-run", is_flag=True, help="Crawl and print, skip AI + enqueue")
@click.option("--stats", "show_stats", is_flag=True, help="Print Redis stats and exit")
def main(source: str, dry_run: bool, show_stats: bool):
    """Game news ingestion pipeline — crawl → AI process → enqueue."""
    redis = RedisClient()  # 内部自动降级,Redis挂了available=False,所有方法变no-op
    if not redis.available:
        print("WARN: Redis unavailable — running in degraded mode (dedup/stats disabled)")

    if show_stats:
        _print_stats(redis)
        return

    all_spiders = {"gamersky": GamerSkySpider(), "gameres": GameResSpider()}
    if source:
        spiders = {source: all_spiders[source]} if source in all_spiders else {}
        if not spiders:
            print(f"Unknown source '{source}'. Available: {list(all_spiders)}")
            sys.exit(1)
    else:
        spiders = all_spiders

    agent = DeepSeekAgent()
    oss = OssClient()
    producer = QueueProducer(redis)

    print(f"{'=' * 60}")
    print(f"Ingest run  |  sources: {list(spiders.keys())}  |  dry-run: {dry_run}")
    print(f"{'=' * 60}")

    # ↓ 关键修改:原来for循环换成asyncio.run
    total_crawled, total_enqueued = asyncio.run(
        run_sources_concurrently(spiders, agent, oss, producer, redis, dry_run)
    )

    print(f"\n{'=' * 60}")
    print(f"Run complete  |  crawled: {total_crawled}  |  enqueued: {total_enqueued}")
    redis.set_stat("last_run_at", "")
    _print_stats(redis)
    print(f"{'=' * 60}")


def _print_stats(redis: RedisClient):
    if not redis.available:
        print("Stats: Redis unavailable (degraded mode)")
        return
    stats = redis.get_stats()
    if not stats:
        print("No stats yet.")
    else:
        print(f"Stats: dedup_size={redis.dedup_size()}  |  " + "  ".join(
            f"{k}={v}" for k, v in sorted(stats.items())))


if __name__ == "__main__":
    main()
