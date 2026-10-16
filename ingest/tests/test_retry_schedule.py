import sqlite3
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

import httpx

from runtime.deliver import OutboxDeliverer, retry_after_seconds
from runtime.outbox import MAX_DELIVERY_ATTEMPTS, Outbox


class RetryScheduleTest(unittest.TestCase):
    def test_network_failure_is_persisted_paused_and_retried_only_when_due(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "outbox.sqlite3"
            outbox = Outbox(path)
            event_id = outbox.enqueue({"title": "fixture"})
            with patch("runtime.outbox.time.time", return_value=1000), patch("runtime.deliver.httpx.post", side_effect=httpx.ConnectError("offline")) as post:
                worker = OutboxDeliverer(outbox, "http://fixture", "token")
                self.assertEqual(0, worker.deliver_ready())
                self.assertEqual("NETWORK_ERROR", worker.halted_reason)
                post.assert_called_once()
            with patch("runtime.outbox.time.time", return_value=1059), patch("runtime.deliver.httpx.post") as post:
                self.assertEqual(0, OutboxDeliverer(Outbox(path), "http://fixture", "token").deliver_ready())
                post.assert_not_called()
            receipt = Mock(status_code=201)
            receipt.json.return_value = {"eventId": event_id, "payloadHash": "a" * 64, "articleId": 7, "decision": "REVIEW"}
            with patch("runtime.outbox.time.time", return_value=1060), patch("runtime.deliver.httpx.post", return_value=receipt) as post:
                self.assertEqual(1, OutboxDeliverer(Outbox(path), "http://fixture", "token").deliver_ready())
                post.assert_called_once()
            self.assertEqual(0, outbox.pending_count())

    def test_retry_after_http_date_is_persisted_across_restart(self):
        self.assertEqual(1000, retry_after_seconds("Thu, 01 Jan 1970 00:33:20 GMT", now_epoch=1000))
        self.assertIsNone(retry_after_seconds("not-a-date", now_epoch=1000))
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "outbox.sqlite3"
            outbox = Outbox(path)
            outbox.enqueue({"title": "fixture"})
            response = Mock(status_code=503, headers={"Retry-After": "Thu, 01 Jan 1970 00:33:20 GMT"})
            with patch("runtime.outbox.time.time", return_value=1000), patch("runtime.deliver.time.time", return_value=1000), patch("runtime.deliver.httpx.post", return_value=response):
                self.assertEqual(0, OutboxDeliverer(outbox, "http://fixture", "token").deliver_ready())
            with patch("runtime.outbox.time.time", return_value=1999):
                self.assertEqual([], Outbox(path).ready())
            with patch("runtime.outbox.time.time", return_value=2000):
                self.assertEqual(1, len(Outbox(path).ready()))

    def test_five_unconfirmed_attempts_are_quarantined_without_losing_the_event(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "outbox.sqlite3"
            outbox = Outbox(path)
            event_id = outbox.enqueue({"title": "fixture"})
            with patch("runtime.outbox.time.time", return_value=1000):
                for _ in range(MAX_DELIVERY_ATTEMPTS):
                    result = outbox.fail(event_id, "network", 0)
                self.assertEqual("QUARANTINED", result["status"])
            self.assertEqual([], outbox.ready())
            self.assertEqual(1, outbox.pending_count())
            db = sqlite3.connect(path)
            try:
                self.assertEqual(("QUARANTINED", MAX_DELIVERY_ATTEMPTS), db.execute("select status,attempts from outbox where event_id=?", (event_id,)).fetchone())
            finally:
                db.close()


if __name__ == "__main__":
    unittest.main()

