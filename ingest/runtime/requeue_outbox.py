"""Explicitly requeue one reviewed QUARANTINED outbox event; never clears delivery pauses."""
import argparse
import json
from pathlib import Path

from outbox import Outbox


def main():
    """命令行入口：在 --confirm 确认后重新入队一个已复核的隔离事件。"""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("database")
    parser.add_argument("--event-id", required=True)
    parser.add_argument("--reason", required=True)
    parser.add_argument("--confirm", action="store_true", help="required acknowledgement that the event was manually reviewed")
    args = parser.parse_args()
    if not args.confirm:
        parser.exit(2, "Refusing to modify outbox without --confirm. No state was changed.\n")
    try:
        path = Path(args.database).resolve(strict=True)
        outbox = Outbox(path)
        outbox.requeue_quarantined(args.event_id, args.reason)
        print(json.dumps({"eventId": args.event_id, "action": "REQUEUE_QUARANTINED", "deliveryPauseCleared": False}, ensure_ascii=False))
    except (OSError, LookupError, ValueError) as exc:
        parser.exit(1, f"Cannot requeue event: {type(exc).__name__}. No other events were changed.\n")


if __name__ == "__main__":
    main()
