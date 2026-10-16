"""Entry point — wired for Linux Cron.

Usage:
    python main.py                     # full pipeline (dry-run safe: skips AI without API key)
    python main.py --source valorant-official   # only crawl one source
    python main.py --dry-run           # crawl only, skip AI processing
    python main.py --stats             # just print Redis stats
"""
import asyncio
import sys
import click
from settings import settings
from storage.redis_client import RedisClient
from storage.oss_client import OssClient
from agent.deepseek_client import DeepSeekAgent
from agent.local_processor import LocalArticleProcessor
from filters.fps_rules import classify
from filters.editorial import editorial_decision
from runtime.state import RunState
from runtime.outbox import Outbox, RuntimeStorageLimitExceeded
from runtime.deliver import OutboxDeliverer
from runtime.capacity import CapacityGuard
from spiders.valorant_official import ValorantOfficialSpider
from spiders.valorant_esports import ValorantEsportsOfficialSpider
from spiders.apex_official import ApexOfficialSpider
from spiders.algs_official import AlgsOfficialSpider
from spiders.delta_force_garena import DeltaForceGarenaOfficialSpider
from spiders.steam_cs2 import SteamCs2OfficialSpider
from source_registry import get_candidate_source
import json
import httpx


def build_ingest_event(source_code, spider, processed, prefilter):
    """Create the versioned internal contract without losing source editorial metadata."""
    prefilter = editorial_decision(prefilter, processed)
    try:
        tags = json.loads(processed.tags) if isinstance(processed.tags, str) else processed.tags
    except json.JSONDecodeError:
        tags = []
    return {
        "schemaVersion": 1,
        "sourceCode": source_code,
        "sourceName": spider.source_name,
        "sourceLanguage": getattr(spider, "source_language", "zh-CN"),
        "originalUrl": processed.original_url,
        "title": processed.title,
        "summary": processed.summary,
        "tags": tags,
        "content": processed.content,
        "decision": prefilter.decision,
        "gameCodes": list(prefilter.game_codes),
        "contentType": prefilter.content_type,
        "ruleVersion": "fps-rules-v1",
        "fetchedAt": processed.crawled_at,
        "originalPublishedAt": processed.original_published_at or None,
        "originalPublishedDate": processed.original_published_date or None,
    }


def process_one_source(name, spider, agent, oss, outbox, redis, state, dry_run):
    """处理单个爬虫源:爬取 → AI处理 → 入队。同步函数,会被to_thread包装。"""
    crawled = 0
    enqueued = 0

    print(f"\n--- [{spider.source_name}] 开始爬取 ---")
    try:
        raw_articles = spider.crawl(spider.LIST_URL)
    except Exception as e:
        print(f"ERROR crawling {name}: {e}")
        if not dry_run:
            redis.incr_stat("total_errors")
        return 0, 0

    for raw in raw_articles:
        prefilter = classify(raw.title, raw.raw_html)
        if prefilter.decision == "REJECT":
            print(f"  ✗ 已过滤 ({prefilter.reason_code}): {raw.title[:40]}")
            if not dry_run:
                redis.incr_stat("total_filtered")
            continue
        if not dry_run and redis.is_crawled(raw.original_url):
            print(f"  ⏭ 跳过 (已成功交付): {raw.title[:40]}")
            if not dry_run:
                redis.incr_stat("total_skipped")
            continue

        if not dry_run:
            capacity = CapacityGuard(settings.CAPACITY_SNAPSHOT_PATH, settings.DISK_WARN_BYTES,
                                     settings.DISK_STOP_BYTES, settings.DISK_HARD_BYTES).check()
            if not capacity.accepts_new_ingest or outbox.pending_count() >= settings.MAX_OUTBOX_PENDING:
                print("PAUSE: candidate admission denied before AI call")
                break

        if not dry_run and not state.reserve_candidate(settings.MAX_CANDIDATES_PER_DAY):
            print("  ⏸ 已达到每日候选处理上限，停止当前来源")
            break

        crawled += 1
        if not dry_run:
            redis.incr_stat("total_crawled")
        print(f"\n  [{crawled}] {raw.title}  prefilter={prefilter.decision} game={prefilter.game_codes} type={prefilter.content_type}")

        if dry_run:
            print("      (dry-run — no AI, queue, OSS, dedup or stats writes)")
            continue

        processed = agent.process(raw, prefilter) if getattr(agent, "accepts_prefilter", False) else agent.process(raw)
        print(f"      processor → cat={processed.category_id}  conf={processed.confidence:.0%}  gaming={processed.is_gaming}")

        final_decision = editorial_decision(prefilter, processed)
        if final_decision.decision == 'REJECT':
            print(f"      ✗ 已过滤 (非游戏内容)")
            redis.incr_stat("total_filtered")
            continue

        # EXTERNAL_SUMMARY does not authorize image copying to object storage.
        # Keep image fetching/upload out of the first-release processing path.

        if not processed.title or not processed.title.strip():
            print(f"      ✗ 已过滤 (空标题)")
            redis.incr_stat("total_filtered")
            continue
        if not processed.content or len(processed.content.strip()) < 50:
            print(f"      ✗ 已过滤 (内容过短)")
            redis.incr_stat("total_filtered")
            continue

        try:
            event_id = outbox.enqueue(build_ingest_event(name, spider, processed, prefilter))
        except RuntimeStorageLimitExceeded:
            print("PAUSE: runtime SQLite storage budget reached; unresolved events were retained")
            break
        print(f"  → 已写入 outbox [{event_id}] {processed.title[:40]}...")
        enqueued += 1

    return crawled, enqueued
#

def source_is_enabled(source_code, dry_run):
    """Honor server-side pause state without giving the crawler database credentials."""
    if dry_run:
        return True
    if not settings.INGEST_INTERNAL_BASE_URL or not settings.INGEST_SERVICE_TOKEN:
        return False
    try:
        response = httpx.get(
            settings.INGEST_INTERNAL_BASE_URL.rstrip('/') + f"/internal/v1/ingest/sources/{source_code}/policy",
            headers={"X-Ingest-Service-Token": settings.INGEST_SERVICE_TOKEN}, timeout=10,
        )
        response.raise_for_status()
        policy = response.json()
        return (isinstance(policy, dict) and policy.get("enabled") is True
                and policy.get("verificationStatus") in ("SHADOW", "AUTO_ENABLED"))
    except Exception as exc:
        print(f"PAUSE: cannot verify source policy for {source_code}: {type(exc).__name__}")
        return False

async def run_sources_concurrently(spiders, agent, oss, outbox, redis, state, dry_run):
    """Compatibility name; first release intentionally processes sources serially."""
    total_crawled = 0
    total_enqueued = 0
    for name, spider in spiders.items():
        if not source_is_enabled(name, dry_run):
            print(f"SKIP: source {name} is paused or policy is unavailable")
            continue
        try:
            crawled, enqueued = process_one_source(name, spider, agent, oss, outbox, redis, state, dry_run)
            total_crawled += crawled
            total_enqueued += enqueued
        except Exception as exc:
            print(f"ERROR in source {name}: {exc}")
            if not dry_run:
                redis.incr_stat("total_errors")
    return total_crawled, total_enqueued


@click.command()
@click.option("--source", default=None, help="Only crawl an approved source (e.g. 'valorant-official')")
@click.option("--dry-run", is_flag=True, help="Crawl and print, skip AI + enqueue")
@click.option("--no-ai", "no_ai", is_flag=True, help="Use the deterministic local cleaner instead of the paid model")
@click.option("--stats", "show_stats", is_flag=True, help="Print Redis stats and exit")
def main(source: str, dry_run: bool, show_stats: bool, no_ai: bool):
    """Game news ingestion pipeline — crawl → process → enqueue."""
    if dry_run and show_stats:
        raise click.UsageError("--dry-run cannot be combined with --stats")
    if no_ai and show_stats:
        raise click.UsageError("--no-ai cannot be combined with --stats")
    candidate_source = get_candidate_source(source)
    if candidate_source is not None:
        click.echo(
            f"CANDIDATE: source '{candidate_source.code}' ({candidate_source.display_name}) is blocked: "
            f"{candidate_source.reason}; no collection started"
        )
        raise click.exceptions.Exit(2)
    if not dry_run and not show_stats and (not settings.INGEST_INTERNAL_BASE_URL or not settings.INGEST_SERVICE_TOKEN):
        raise click.ClickException("Internal ingest endpoint and service token are required; no collection started")
    redis = None if dry_run else RedisClient()
    if redis is not None and not redis.available:
        print("WARN: Redis unavailable — running in degraded mode (dedup/stats disabled)")

    if show_stats:
        _print_stats(redis)
        return

    all_spiders = {"valorant-official": ValorantOfficialSpider(), "valorant-esports-official": ValorantEsportsOfficialSpider(), "apex-official": ApexOfficialSpider(), "algs-official": AlgsOfficialSpider(), "delta-force-garena-official": DeltaForceGarenaOfficialSpider(), "cs2-official": SteamCs2OfficialSpider()}
    if source:
        spiders = {source: all_spiders[source]} if source in all_spiders else {}
        if not spiders:
            print(f"Unknown source '{source}'. Available: {list(all_spiders)}")
            sys.exit(1)
    else:
        spiders = all_spiders

    state = None if dry_run else RunState(settings.RUNTIME_STATE_PATH)
    if state is not None and not state.claim_worker():
        print("Another ingestion worker is active; refusing overlapping run.")
        sys.exit(2)


    print(f"{'=' * 60}")
    print(f"Ingest run  |  sources: {list(spiders.keys())}  |  dry-run: {dry_run}  |  processor: {'local' if no_ai else 'deepseek'}")
    print(f"{'=' * 60}")

    delivered = 0
    try:
        agent = None if dry_run else (LocalArticleProcessor() if no_ai else DeepSeekAgent())
        oss = None  # Reserved for a future licensed full-text pipeline.
        outbox = None if dry_run else Outbox(settings.OUTBOX_PATH, runtime_paths=(settings.OUTBOX_PATH, settings.RUNTIME_STATE_PATH), max_runtime_bytes=settings.MAX_RUNTIME_STORAGE_BYTES)
        if not dry_run:
            capacity = CapacityGuard(settings.CAPACITY_SNAPSHOT_PATH, settings.DISK_WARN_BYTES, settings.DISK_STOP_BYTES, settings.DISK_HARD_BYTES).check()
            print(f"Capacity gate: {capacity.state} used={capacity.used_bytes} reason={capacity.reason or '-'}")
            if not capacity.accepts_new_ingest:
                print("PAUSE: capacity gate does not allow new ingestion; durable outbox remains untouched")
                return
        if not dry_run and settings.INGEST_INTERNAL_BASE_URL and settings.INGEST_SERVICE_TOKEN:
            deliverer = OutboxDeliverer(outbox, settings.INGEST_INTERNAL_BASE_URL, settings.INGEST_SERVICE_TOKEN,
                                        on_ack=lambda payload: redis.mark_crawled(payload["originalUrl"]))
            delivered += deliverer.deliver_ready()
            if deliverer.halted_reason:
                print(f"PAUSE: delivery halted ({deliverer.halted_reason}); unresolved events retained")
                return
        elif not dry_run:
            print("WARN: internal ingest endpoint is not configured; events remain durable in the SQLite outbox")

        if not dry_run and outbox.pending_count() >= settings.MAX_OUTBOX_PENDING:
            print(f"PAUSE: pending outbox is {outbox.pending_count()}, at configured limit {settings.MAX_OUTBOX_PENDING}")
            return

        total_crawled, total_enqueued = asyncio.run(
            run_sources_concurrently(spiders, agent, oss, outbox, redis, state, dry_run)
        )
        if not dry_run and settings.INGEST_INTERNAL_BASE_URL and settings.INGEST_SERVICE_TOKEN:
            deliverer = OutboxDeliverer(outbox, settings.INGEST_INTERNAL_BASE_URL, settings.INGEST_SERVICE_TOKEN,
                                        on_ack=lambda payload: redis.mark_crawled(payload["originalUrl"]))
            delivered += deliverer.deliver_ready()
            if deliverer.halted_reason:
                print(f"PAUSE: delivery halted ({deliverer.halted_reason}); unresolved events retained")
                return
    finally:
        if state is not None:
            state.release_worker()

    print(f"\n{'=' * 60}")
    print(f"Run complete  |  crawled: {total_crawled}  |  enqueued: {total_enqueued}  |  delivered: {delivered}")
    if not dry_run:
        redis.set_stat("last_run_at", "")
        _print_stats(redis)
    print(f"{'=' * 60}")


def _print_stats(redis: RedisClient):
    """打印 Redis 运行统计；Redis 不可用时提示处于降级模式。

    Args:
        redis: Redis 客户端实例。
    """
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
