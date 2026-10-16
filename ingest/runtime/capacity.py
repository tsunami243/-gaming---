"""Fail-closed admission based on a complete, fresh project storage snapshot.
A filesystem's total-used value is NOT the project's storage consumption.
"""
import json
import time
from dataclasses import dataclass
from pathlib import Path

COMPONENTS = {'images', 'volumes', 'logs', 'temporary', 'backups'}

@dataclass(frozen=True)
class CapacityDecision:
    """一次容量检查的不可变结论。"""

    state: str
    used_bytes: int | None
    total_bytes: int | None
    reason: str | None

    @property
    def accepts_new_ingest(self):
        """该结论是否允许接受新的摄取。"""
        return self.state in ('OK', 'WARN')

class CapacityGuard:
    """基于完整、新鲜的项目存储快照做 fail-closed 准入判断。"""

    def __init__(self, snapshot_path, warn_bytes=7_000_000_000, stop_bytes=8_000_000_000, hard_bytes=10_000_000_000):
        """初始化容量闸门并校验阈值单调递增。

        Args:
            snapshot_path: 快照文件路径；为假值时不接受新摄取。
            warn_bytes: 仅告警的用量阈值。
            stop_bytes: 需暂停摄取的用量阈值。
            hard_bytes: 硬停止阈值，不得超过 10_000_000_000。

        Raises:
            ValueError: 阈值不满足 0 < warn_bytes < stop_bytes < hard_bytes <= 1e10 时抛出。
        """
        self.path = Path(snapshot_path) if snapshot_path else None
        if not 0 < warn_bytes < stop_bytes < hard_bytes <= 10_000_000_000:
            raise ValueError('invalid capacity thresholds')
        self.warn_bytes, self.stop_bytes, self.hard_bytes = warn_bytes, stop_bytes, hard_bytes

    def check(self):
        """读取并校验快照，返回当前容量结论。

        Returns:
            对应的 CapacityDecision；快照缺失、过期或不可信时返回 UNKNOWN。
        """
        try:
            if self.path is None or not self.path.is_file() or self.path.stat().st_size > 16_384:
                raise ValueError('snapshot unavailable')
            snapshot = json.loads(self.path.read_text(encoding='utf-8'))
            if snapshot.get('scope') != 'gameboi-project':
                raise ValueError('wrong scope')
            observed = snapshot.get('observedAt')
            if type(observed) not in (int, float):
                raise ValueError('invalid time')
            age = time.time() - observed
            if not -5 <= age <= 120:
                raise ValueError('stale or future snapshot')
            components = snapshot.get('components')
            if not isinstance(components, dict) or set(components) != COMPONENTS:
                raise ValueError('incomplete accounting')
            values = [*components.values(), snapshot.get('hostAvailableBytes')]
            if any(type(value) is not int or value < 0 for value in values):
                raise ValueError('invalid byte values')
            used = sum(components.values())
            free = snapshot['hostAvailableBytes']
        except Exception:
            return CapacityDecision('UNKNOWN', None, None, 'fresh complete project snapshot required')
        if used >= self.hard_bytes:
            return CapacityDecision('HARD_STOP', used, self.hard_bytes, 'project hard threshold reached')
        if used >= self.stop_bytes or free < 5_000_000_000:
            return CapacityDecision('STOP', used, self.hard_bytes, 'project usage or host free space requires pause')
        if used >= self.warn_bytes:
            return CapacityDecision('WARN', used, self.hard_bytes, 'project warning threshold reached')
        return CapacityDecision('OK', used, self.hard_bytes, None)
