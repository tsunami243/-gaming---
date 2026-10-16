"""Durable, bounded, retryable ingest outbox."""
import hashlib
import json
import sqlite3
import time
import uuid
from contextlib import closing
from datetime import datetime, timezone
from pathlib import Path

MAX_EVENT_BYTES = 256_000
MAX_DELIVERY_ATTEMPTS = 5
MAX_RUNTIME_STORAGE_BYTES = 50_000_000


class PayloadTooLarge(ValueError):
    """事件序列化后超过 MAX_EVENT_BYTES 上限时抛出。"""
    pass


class RuntimeStorageLimitExceeded(RuntimeError):
    """运行时 SQLite 存储预算耗尽时抛出。"""
    pass


class Outbox:
    """持久化、有界、可重试的摄取投递 outbox（SQLite 存储）。"""

    def __init__(self, path, runtime_paths=None, max_runtime_bytes=MAX_RUNTIME_STORAGE_BYTES):
        """初始化 outbox 并确保所需表结构存在。

        Args:
            path: outbox 的 SQLite 数据库文件路径。
            runtime_paths: 计入存储预算的 SQLite 基础文件路径集合，默认仅 path。
            max_runtime_bytes: 运行时存储字节上限，须为正整数。

        Raises:
            ValueError: max_runtime_bytes 不是正整数时抛出。
        """
        self.path = Path(path)
        self.runtime_paths = tuple(Path(item) for item in (runtime_paths or (self.path,)))
        if type(max_runtime_bytes) is not int or max_runtime_bytes < 1:
            raise ValueError("max runtime bytes must be a positive integer")
        self.max_runtime_bytes = max_runtime_bytes
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with closing(sqlite3.connect(self.path)) as db:
            db.execute("create table if not exists outbox(event_id text primary key,payload_json text not null,payload_hash text not null,status text not null,attempts integer not null default 0,last_error text,next_attempt_at real,created_at text not null,updated_at text not null)")
            columns = {row[1] for row in db.execute("pragma table_info(outbox)")}
            if "next_attempt_at" not in columns:
                db.execute("alter table outbox add column next_attempt_at real")
            db.execute("create table if not exists delivery_control(id integer primary key check(id=1), reason text not null, resume_at real)")
            db.execute("create table if not exists outbox_operator_audit(id integer primary key autoincrement,event_id text not null,action text not null,reason text not null,created_at text not null)")
            db.commit()

    def storage_bytes(self):
        """Count each configured SQLite base file and journal once; never delete to make room."""
        files = set()
        for base in self.runtime_paths:
            for candidate in (base, Path(str(base) + "-wal"), Path(str(base) + "-shm"), Path(str(base) + "-journal")):
                files.add(candidate.resolve(strict=False))
        return sum(candidate.stat().st_size for candidate in files if candidate.is_file())

    def ensure_storage_capacity(self, incoming_bytes=0):
        """在写入前校验运行时存储预算，不足则 fail-closed 拒绝。

        Args:
            incoming_bytes: 本次计划写入的字节数，须为非负整数。

        Returns:
            当前已使用的字节数。

        Raises:
            ValueError: incoming_bytes 不是非负整数时抛出。
            RuntimeStorageLimitExceeded: 已用空间加写入量超出预算时抛出。
        """
        if type(incoming_bytes) is not int or incoming_bytes < 0:
            raise ValueError("incoming bytes must be a non-negative integer")
        used = self.storage_bytes()
        if used + incoming_bytes > self.max_runtime_bytes:
            raise RuntimeStorageLimitExceeded(f"runtime SQLite budget reached: used={used} incoming={incoming_bytes} limit={self.max_runtime_bytes}")
        return used

    def enqueue(self, payload):
        """将事件写入 outbox，并按 eventId 幂等去重。

        Args:
            payload: 事件字典；缺失 eventId 时自动生成。

        Returns:
            事件的 eventId。

        Raises:
            PayloadTooLarge: 编码后超过 MAX_EVENT_BYTES 时抛出。
            RuntimeStorageLimitExceeded: 存储预算不足时抛出。
            ValueError: 同 eventId 已存在且负载不一致时抛出。
        """
        event_id = payload.get("eventId") or str(uuid.uuid4())
        payload = {**payload, "eventId": event_id}
        encoded = json.dumps(payload, ensure_ascii=False, sort_keys=True, allow_nan=False)
        encoded_bytes = encoded.encode("utf-8")
        if len(encoded_bytes) > MAX_EVENT_BYTES:
            raise PayloadTooLarge("event exceeds 256000 UTF-8 bytes; payload was not truncated or queued")
        # Reserve conservative SQLite/WAL metadata; full budget failure never deletes unresolved data.
        self.ensure_storage_capacity(len(encoded_bytes) + 8192)
        now = datetime.now(timezone.utc).isoformat()
        with closing(sqlite3.connect(self.path)) as db:
            db.execute("begin immediate")
            existing = db.execute("select payload_json from outbox where event_id=?", (event_id,)).fetchone()
            if existing is not None:
                if existing[0] != encoded:
                    raise ValueError("eventId already exists with a different payload")
            else:
                db.execute("insert into outbox(event_id,payload_json,payload_hash,status,attempts,last_error,next_attempt_at,created_at,updated_at) values(?,?,?,?,0,null,null,?,?)", (event_id, encoded, hashlib.sha256(encoded_bytes).hexdigest(), "READY", now, now))
            db.commit()
        return event_id

    def ready(self, limit=50):
        """读取当前可投递的事件（READY 且已到达重试时间）。

        Args:
            limit: 最大返回条数，须在 1 到 200 之间。

        Returns:
            (event_id, payload) 元组列表，按创建时间、event_id 排序。

        Raises:
            ValueError: limit 超出 1..200 范围时抛出。
        """
        if type(limit) is not int or limit < 1 or limit > 200:
            raise ValueError("outbox batch limit must be between 1 and 200")
        with closing(sqlite3.connect(self.path)) as db:
            return [(row[0], json.loads(row[1])) for row in db.execute("select event_id,payload_json from outbox where status='READY' and (next_attempt_at is null or next_attempt_at<=?) order by created_at,event_id limit ?", (time.time(), limit))]

    def ready_count(self):
        """返回状态为 READY 的事件数量。"""
        with closing(sqlite3.connect(self.path)) as db:
            return db.execute("select count(*) from outbox where status='READY'").fetchone()[0]

    def ack(self, event_id):
        """将事件标记为 ACKED 并清除其重试时间。

        Args:
            event_id: 要确认的事件 ID。
        """
        with closing(sqlite3.connect(self.path)) as db:
            db.execute("update outbox set status='ACKED',next_attempt_at=null,updated_at=? where event_id=?", (datetime.now(timezone.utc).isoformat(), event_id))
            db.commit()

    def fail(self, event_id, error, delay_seconds=0):
        """记录一次投递失败，并按累计次数决定重试或隔离。

        Args:
            event_id: 失败的事件 ID。
            error: 错误信息，最多保留 500 字符。
            delay_seconds: 重试延迟秒数，须为非负数。

        Returns:
            含 attempts、status、next_attempt_at 的字典；事件不存在或非 READY 时返回 None。

        Raises:
            ValueError: delay_seconds 非法时抛出。
        """
        if type(delay_seconds) not in (int, float) or delay_seconds < 0:
            raise ValueError("invalid retry delay")
        now = time.time()
        with closing(sqlite3.connect(self.path)) as db:
            db.execute("begin immediate")
            row = db.execute("select attempts from outbox where event_id=? and status='READY'", (event_id,)).fetchone()
            if row is None:
                db.commit()
                return None
            attempts = row[0] + 1
            status = "QUARANTINED" if attempts >= MAX_DELIVERY_ATTEMPTS else "READY"
            next_attempt_at = None if status == "QUARANTINED" else now + delay_seconds
            db.execute("update outbox set status=?,attempts=?,last_error=?,next_attempt_at=?,updated_at=? where event_id=?", (status, attempts, error[:500], next_attempt_at, datetime.now(timezone.utc).isoformat(), event_id))
            db.commit()
            return {"attempts": attempts, "status": status, "next_attempt_at": next_attempt_at}

    def schedule_retry(self, event_id, delay_seconds):
        """为 READY 事件设置下次可投递时间。

        Args:
            event_id: 目标事件 ID。
            delay_seconds: 相对当前时间的延迟秒数，须为非负数。

        Raises:
            ValueError: delay_seconds 非法时抛出。
        """
        if type(delay_seconds) not in (int, float) or delay_seconds < 0:
            raise ValueError("invalid retry delay")
        with closing(sqlite3.connect(self.path)) as db:
            db.execute("update outbox set next_attempt_at=?,updated_at=? where event_id=? and status='READY'", (time.time() + delay_seconds, datetime.now(timezone.utc).isoformat(), event_id))
            db.commit()

    def pending_count(self):
        """返回所有未 ACKED（含隔离）的事件数量。"""
        with closing(sqlite3.connect(self.path)) as db:
            return db.execute("select count(*) from outbox where status != 'ACKED'").fetchone()[0]

    def quarantine(self, event_id, error):
        """立即将 READY 事件隔离，不再自动重试。

        Args:
            event_id: 目标事件 ID。
            error: 错误信息，最多保留 500 字符。
        """
        with closing(sqlite3.connect(self.path)) as db:
            db.execute("update outbox set status='QUARANTINED',attempts=attempts+1,last_error=?,next_attempt_at=null,updated_at=? where event_id=? and status='READY'", (error[:500], datetime.now(timezone.utc).isoformat(), event_id))
            db.commit()

    def requeue_quarantined(self, event_id, reason):
        """人工复核后将隔离事件重新置为 READY，并记录操作审计。

        Args:
            event_id: 要重新入队的事件 ID。
            reason: 操作原因，去空格后须为 1 到 200 字符。

        Raises:
            ValueError: event_id 或 reason 非法，或事件不处于 QUARANTINED 时抛出。
            LookupError: 事件不存在时抛出。
        """
        if not isinstance(event_id, str) or not event_id:
            raise ValueError("event id is required")
        if not isinstance(reason, str) or not reason.strip() or len(reason.strip()) > 200:
            raise ValueError("operator reason must be between 1 and 200 characters")
        now = datetime.now(timezone.utc).isoformat()
        with closing(sqlite3.connect(self.path)) as db:
            db.execute("begin immediate")
            row = db.execute("select status from outbox where event_id=?", (event_id,)).fetchone()
            if row is None:
                db.commit()
                raise LookupError("outbox event not found")
            if row[0] != "QUARANTINED":
                db.commit()
                raise ValueError("only quarantined events can be explicitly requeued")
            db.execute("update outbox set status='READY',attempts=0,last_error=?,next_attempt_at=null,updated_at=? where event_id=?", ("operator requeue: " + reason.strip(), now, event_id))
            db.execute("insert into outbox_operator_audit(event_id,action,reason,created_at) values(?,?,?,?)", (event_id, "REQUEUE_QUARANTINED", reason.strip(), now))
            db.commit()

    def pause_delivery(self, reason, delay_seconds=None):
        """暂停投递，并可选择在指定延迟后自动恢复。

        Args:
            reason: 暂停原因，最多保留 100 字符。
            delay_seconds: 自动恢复的延迟秒数；None 表示需人工恢复。

        Raises:
            ValueError: delay_seconds 非法时抛出。
        """
        if delay_seconds is not None and (type(delay_seconds) not in (int, float) or delay_seconds < 0):
            raise ValueError("invalid delay")
        resume_at = None if delay_seconds is None else time.time() + delay_seconds
        with closing(sqlite3.connect(self.path)) as db:
            db.execute("insert into delivery_control(id,reason,resume_at) values(1,?,?) on conflict(id) do update set reason=excluded.reason,resume_at=excluded.resume_at", (reason[:100], resume_at))
            db.commit()

    def delivery_pause_reason(self):
        """返回当前生效的投递暂停原因；无暂停时返回 None。"""
        with closing(sqlite3.connect(self.path)) as db:
            row = db.execute("select reason,resume_at from delivery_control where id=1").fetchone()
            if row is not None and (row[1] is None or row[1] > time.time()):
                return row[0]
        return None
