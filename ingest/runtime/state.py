"""Small SQLite state store for the single first-release ingestion worker."""
import os
import sqlite3
from datetime import date
from pathlib import Path

class RunState:
    """单实例首版摄取 worker 使用的轻量 SQLite 状态存储。"""

    def __init__(self, path: str):
        """初始化状态库并确保所需表结构存在。

        Args:
            path: 状态 SQLite 数据库文件路径。
        """
        self.path = Path(path)
        self._worker_file = None
        self.path.parent.mkdir(parents=True, exist_ok=True)
        db = self._connect()
        try:
            db.execute("create table if not exists daily_budget (run_date text primary key, used integer not null)")
            db.execute("create table if not exists worker_lock (name text primary key, acquired_at text not null)")
            db.commit()
        finally:
            db.close()

    def _connect(self):
        """打开到状态库的 SQLite 连接。"""
        return sqlite3.connect(self.path)

    def claim_worker(self) -> bool:
        """尝试获取单实例 worker 锁。

        Returns:
            成功获取返回 True；已被占用或本实例已持有返回 False。

        Raises:
            Exception: 检查 SQLite 锁时发生错误，会先释放本地锁再向上抛出。
        """
        if self._worker_file is not None:
            return False
        # Never unlink the lock file: every process must lock the same file identity.
        handle = open(str(self.path) + '.lock', 'a+b')
        try:
            if os.name == 'nt':
                import msvcrt
                handle.seek(0)
                msvcrt.locking(handle.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl
                fcntl.flock(handle.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        except OSError:
            handle.close()
            return False
        self._worker_file = handle
        # Old deployments may still own the SQLite lock. Fail closed, never erase it.
        db = None
        try:
            db = self._connect()
            if db.execute("select 1 from worker_lock where name='ingest'").fetchone():
                self.release_worker()
                return False
        except Exception:
            self.release_worker()
            raise
        finally:
            if db is not None:
                db.close()
        return True

    def release_worker(self) -> None:
        """释放当前实例持有的 worker 锁；未持有时为空操作。"""
        if self._worker_file is None:
            return
        handle, self._worker_file = self._worker_file, None
        try:
            if os.name == 'nt':
                import msvcrt
                handle.seek(0)
                msvcrt.locking(handle.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                import fcntl
                fcntl.flock(handle.fileno(), fcntl.LOCK_UN)
        finally:
            handle.close()

    def reserve_candidate(self, limit: int) -> bool:
        """在当日预算内预留一个候选名额。

        Args:
            limit: 当日候选数量上限，须为正整数。

        Returns:
            预留成功返回 True；当日已达上限返回 False。

        Raises:
            ValueError: limit 不是正整数时抛出。
        """
        if type(limit) is not int or limit < 1:
            raise ValueError("candidate limit must be a positive integer")
        today = date.today().isoformat()
        db = self._connect()
        try:
            db.execute("begin immediate")
            used = db.execute("select used from daily_budget where run_date=?", (today,)).fetchone()
            current = 0 if used is None else used[0]
            if current >= limit:
                return False
            db.execute("insert into daily_budget(run_date, used) values (?, 1) on conflict(run_date) do update set used=used+1", (today,))
            db.commit()
            return True
        finally:
            db.close()
