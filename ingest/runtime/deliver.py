"""HTTP delivery: a success status alone is not a committed ingest receipt."""
import re
import time
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime

import httpx


def validate_receipt(receipt, event_id):
    """校验服务端回执与本地事件严格一致。

    Args:
        receipt: 服务端返回的回执字典。
        event_id: 本地事件的 eventId。

    Raises:
        ValueError: 回执的 eventId、payloadHash、decision 或 articleId 任一不合法时抛出。
    """
    if not isinstance(receipt, dict) or receipt.get('eventId') != event_id:
        raise ValueError('receipt event mismatch')
    digest = receipt.get('payloadHash')
    if not isinstance(digest, str) or not re.fullmatch(r'[a-f0-9]{64}', digest):
        raise ValueError('receipt hash missing or malformed')
    decision = receipt.get('decision')
    if decision not in ('ACCEPT', 'REVIEW', 'REJECT'):
        raise ValueError('receipt decision invalid')
    article_id = receipt.get('articleId')
    if decision != 'REJECT' and (type(article_id) is not int or article_id <= 0):
        raise ValueError('accepted receipt has no persisted article')
    if decision == 'REJECT' and article_id is not None:
        raise ValueError('rejected receipt unexpectedly references an article')


def retry_after_seconds(value, now_epoch=None):
    """Parse RFC seconds or HTTP-date Retry-After safely; invalid/past values fall back to None."""
    if not isinstance(value, str):
        return None
    value = value.strip()
    if value.isdigit() and len(value) <= 10:
        return int(value)
    try:
        target = parsedate_to_datetime(value)
        if target.tzinfo is None:
            target = target.replace(tzinfo=timezone.utc)
        delay = target.timestamp() - (time.time() if now_epoch is None else now_epoch)
        return max(0, int(delay))
    except (TypeError, ValueError, IndexError, OverflowError):
        return None


def exponential_backoff(attempts):
    """Bound per-event retry without sleeping a worker; attempt 1 starts at one minute."""
    return min(3600, 60 * (2 ** max(0, attempts - 1)))


class OutboxDeliverer:
    """将 outbox 中的 READY 事件投递到摄取服务，仅在回执确认后 ack。"""

    def __init__(self, outbox, base_url, token, on_ack=None):
        """初始化投递器。

        Args:
            outbox: 提供 ready/ack/fail 等接口的 Outbox 实例。
            base_url: 摄取服务基础 URL，尾部斜杠会被去除。
            token: 请求头 X-Ingest-Service-Token 使用的服务令牌。
            on_ack: 可选回调，在回执确认后以 payload 调用。
        """
        self.outbox, self.base_url, self.token, self.on_ack = outbox, base_url.rstrip('/'), token, on_ack
        self.halted_reason = None

    def _fail_with_backoff(self, event_id, error):
        """记录失败，并在事件仍为 READY 时安排指数退避重试。

        Args:
            event_id: 失败的事件 ID。
            error: 错误信息。

        Returns:
            安排的重试延迟秒数；无需重试时返回 None。
        """
        result = self.outbox.fail(event_id, error)
        if result is not None and result['status'] == 'READY':
            delay = exponential_backoff(result['attempts'])
            self.outbox.schedule_retry(event_id, delay)
            return delay
        return None

    def deliver_ready(self):
        """投递一批 READY 事件，遇到暂停或网络错误时提前停止。

        Returns:
            本次成功确认投递的事件数量。
        """
        delivered = 0
        self.halted_reason = self.outbox.delivery_pause_reason()
        if self.halted_reason:
            return 0
        for event_id, payload in self.outbox.ready():
            try:
                response = httpx.post(self.base_url + '/internal/v1/ingest/events', json=payload,
                                      headers={'X-Ingest-Service-Token': self.token}, timeout=30)
                if response.status_code not in (200, 201):
                    status = response.status_code
                    if status in (400, 409, 413, 415, 422):
                        self.outbox.quarantine(event_id, f'HTTP {status}')
                    elif status in (401, 403):
                        self.outbox.fail(event_id, f'HTTP {status}')
                        self.halted_reason = f'HTTP {status}'
                        self.outbox.pause_delivery(self.halted_reason)
                        break
                    elif status in (429, 503):
                        delay = retry_after_seconds(response.headers.get('Retry-After'))
                        delay = max(900, delay if delay is not None else 0)
                        self.outbox.fail(event_id, f'HTTP {status}', delay)
                        self.halted_reason = f'HTTP {status}'
                        self.outbox.pause_delivery(self.halted_reason, delay)
                        break
                    else:
                        self._fail_with_backoff(event_id, f'HTTP {status}')
                    continue
                validate_receipt(response.json(), event_id)
                self.outbox.ack(event_id)
            except httpx.RequestError as exc:
                delay = self._fail_with_backoff(event_id, f'delivery not confirmed: {type(exc).__name__}')
                if delay is not None:
                    self.halted_reason = 'NETWORK_ERROR'
                    self.outbox.pause_delivery(self.halted_reason, delay)
                    break
                continue
            except Exception as exc:
                self._fail_with_backoff(event_id, f'delivery not confirmed: {type(exc).__name__}')
                continue
            delivered += 1
            if self.on_ack:
                try:
                    self.on_ack(payload)
                except Exception:
                    print('WARN: receipt committed; optional dedup cache update failed')
        return delivered

