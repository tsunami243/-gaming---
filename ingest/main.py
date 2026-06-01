"""Entry point — wired for Linux Cron.

Usage:
    python main.py                     # full pipeline (dry-run safe: skips AI without API key)
    python main.py --source gamersky   # only crawl one source
    python main.py --dry-run           # crawl only, skip AI processing
    python main.py --stats             # just print Redis stats
"""
import sys
import hashlib
import click
from settings import settings
from storage.redis_client import RedisClient
from storage.oss_client import OssClient
from mq.producer import QueueProducer
from agent.deepseek_client import DeepSeekAgent
from spiders.gamersky import GamerSkySpider
from models.article import ProcessedArticle


@click.command()
@click.option("--source", default=None, help="Only crawl this source (e.g. 'gamersky')")
@click.option("--dry-run", is_flag=True, help="Crawl and print, skip AI + enqueue")
@click.option("--stats", "show_stats", is_flag=True, help="Print Redis stats and exit")
def main(source: str, dry_run: bool, show_stats: bool):
    """Game news ingestion pipeline — crawl → AI process → enqueue."""
    redis = RedisClient()
    if not redis.ping():
        print("ERROR: Redis not reachable — aborting.")
        sys.exit(1)

    if show_stats:
        _print_stats(redis)
        return

    # Select spiders
    all_spiders = {"gamersky": GamerSkySpider()}
    if source:
        spiders = {source: all_spiders[source]} if source in all_spiders else {}
        if not spiders:
            print(f"Unknown source '{source}'. Available: {list(all_spiders)}")
            sys.exit(1)
    else:
        spiders = all_spiders

    # Init pipeline components
    agent = DeepSeekAgent()
    oss = OssClient()
    producer = QueueProducer(redis)

    total_crawled = 0
    total_enqueued = 0

    print(f"{'='*60}")
    print(f"Ingest run  |  sources: {list(spiders.keys())}  |  dry-run: {dry_run}")
    print(f"{'='*60}")

    for name, spider in spiders.items():
        print(f"\n--- [{spider.source_name}] 开始爬取 ---")

        try:
            raw_articles = spider.crawl(spider.LIST_URL)
        except Exception as e:
            print(f"ERROR crawling {name}: {e}")
            redis.incr_stat("total_errors")
            continue

        for raw in raw_articles:
            # Dedup
            if redis.is_crawled(raw.original_url):
                print(f"  ⏭ 跳过 (已爬过): {raw.title[:40]}")
                redis.incr_stat("total_skipped")
                continue

            redis.mark_crawled(raw.original_url)
            total_crawled += 1
            redis.incr_stat("total_crawled")
            print(f"\n  [{total_crawled}] {raw.title}")

            if dry_run:
                print(f"      (dry-run — skip AI/enqueue)")
                continue

            # AI processing
            processed: ProcessedArticle = agent.process(raw)
            print(f"      AI → cat={processed.category_id}  conf={processed.confidence:.0%}  gaming={processed.is_gaming}")

            # Filter：非游戏内容不入队
            if not processed.is_gaming:
                print(f"      ✗ 已过滤 (非游戏内容)")
                redis.incr_stat("total_filtered")
                continue

            # Image upload
            article_dir = hashlib.md5(raw.original_url.encode()).hexdigest()[:8]
            new_content, new_cover = oss.upload_article_images(
                processed.content, processed.cover_img, article_dir
            )
            processed.content = new_content
            processed.cover_img = new_cover

            # Enqueue
            producer.enqueue(processed)
            total_enqueued += 1

    # Summary
    print(f"\n{'='*60}")
    print(f"Run complete  |  crawled: {total_crawled}  |  enqueued: {total_enqueued}")
    redis.set_stat("last_run_at", "")
    _print_stats(redis)
    print(f"{'='*60}")


def _print_stats(redis: RedisClient):
    stats = redis.get_stats()
    if not stats:
        print("No stats yet.")
    else:
        print(f"Stats: dedup_size={redis.dedup_size()}  |  " + "  ".join(
            f"{k}={v}" for k, v in sorted(stats.items())))


if __name__ == "__main__":
    main()
