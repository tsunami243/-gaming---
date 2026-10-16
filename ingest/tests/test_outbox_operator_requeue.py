import sqlite3
import tempfile
import unittest
from pathlib import Path

from runtime.outbox import Outbox


class OutboxOperatorRequeueTest(unittest.TestCase):
    def test_only_quarantined_event_can_be_requeued_with_a_persistent_audit_record(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "queue.sqlite3"
            outbox = Outbox(path)
            event_id = outbox.enqueue({"title": "fixture"})
            outbox.quarantine(event_id, "HTTP 422")
            outbox.pause_delivery("HTTP 403")
            outbox.requeue_quarantined(event_id, "fixed source metadata after manual review")
            self.assertEqual([(event_id, {"title": "fixture", "eventId": event_id})], outbox.ready())
            self.assertEqual("HTTP 403", outbox.delivery_pause_reason())
            db = sqlite3.connect(path)
            try:
                self.assertEqual((event_id, "REQUEUE_QUARANTINED", "fixed source metadata after manual review"), db.execute("select event_id,action,reason from outbox_operator_audit").fetchone())
                self.assertEqual(("READY", 0), db.execute("select status,attempts from outbox where event_id=?", (event_id,)).fetchone())
            finally:
                db.close()

    def test_acknowledged_or_ready_events_and_blank_reasons_cannot_be_requeued(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = Outbox(Path(directory) / "queue.sqlite3")
            ready = outbox.enqueue({"eventId": "ready", "title": "fixture"})
            with self.assertRaises(ValueError):
                outbox.requeue_quarantined(ready, "reviewed")
            outbox.quarantine(ready, "bad")
            with self.assertRaises(ValueError):
                outbox.requeue_quarantined(ready, " ")
            outbox.ack(ready)
            with self.assertRaises(ValueError):
                outbox.requeue_quarantined(ready, "reviewed")
            with self.assertRaises(LookupError):
                outbox.requeue_quarantined("missing", "reviewed")


if __name__ == "__main__":
    unittest.main()

