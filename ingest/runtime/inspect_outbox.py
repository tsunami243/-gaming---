"""Read-only outbox inspection. Never initializes schema, prints payloads or resumes work."""
import argparse
import json
import re
import sqlite3
import time
from contextlib import closing
from pathlib import Path


def inspect_outbox(path, limit=20):
    """只读地检查 outbox 状态并返回汇总信息。

    Args:
        path: outbox 的 SQLite 数据库文件路径。
        limit: 返回的未解决事件条数上限，须在 1 到 100 之间。

    Returns:
        含 counts、unresolved、deliveryPause、events、displayed、readOnly 的字典。

    Raises:
        ValueError: limit 超出 1..100 范围时抛出。
    """
    if type(limit) is not int or not 1 <= limit <= 100:
        raise ValueError('limit must be between 1 and 100')
    path = Path(path).resolve(strict=True)
    with closing(sqlite3.connect(path.as_uri() + '?mode=ro', uri=True)) as db:
        counts = dict(db.execute('select status,count(*) from outbox group by status'))
        entries = [dict(eventId=row[0], status=row[1], attempts=row[2], createdAt=row[3]) for row in db.execute(
            "select event_id,status,attempts,created_at from outbox where status!='ACKED' order by created_at,event_id limit ?", (limit,))]
        has_control = db.execute("select 1 from sqlite_master where type='table' and name='delivery_control'").fetchone()
        control = db.execute('select reason,resume_at from delivery_control where id=1').fetchone() if has_control else None
    pause = None
    if control and (control[1] is None or control[1] > time.time()):
        # Older versions may have persisted arbitrary diagnostic text. Never echo it.
        reason = control[0] if re.fullmatch(r'HTTP (401|403|429|503)', control[0]) else 'PAUSED'
        pause = dict(reason=reason, resumeAt=control[1], requiresOperatorReview=control[1] is None)
    return dict(counts=counts, unresolved=sum(count for status,count in counts.items() if status!='ACKED'),
                deliveryPause=pause, events=entries, displayed=len(entries), readOnly=True)


def main():
    """命令行入口：以 JSON 打印一次只读的 outbox 检查结果。"""
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('database')
    parser.add_argument('--limit', type=int, default=20)
    args=parser.parse_args()
    try:
        print(json.dumps(inspect_outbox(args.database,args.limit), ensure_ascii=False, indent=2))
    except (OSError, ValueError, sqlite3.Error):
        parser.exit(1, 'Cannot inspect outbox; verify the file, schema and read permissions. No state was created.\n')

if __name__=='__main__':
    main()
