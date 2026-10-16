import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from runtime.deliver import OutboxDeliverer
from runtime.outbox import Outbox

class OutboxTest(unittest.TestCase):
 def test_persists_until_ack_and_counts_pending(self):
  with tempfile.TemporaryDirectory() as directory:
   outbox = Outbox(str(Path(directory) / "outbox.sqlite3"))
   event = outbox.enqueue({"schemaVersion": 1, "title": "CS2 更新"})
   self.assertEqual(1, outbox.ready_count())
   outbox.fail(event, "network")
   self.assertEqual(1, len(outbox.ready()))
   self.assertEqual(1, outbox.ready_count())
   outbox.ack(event)
   self.assertEqual([], outbox.ready())
   self.assertEqual(0, outbox.ready_count())

class DelivererTest(unittest.TestCase):
 def test_success_acks_marks_source_url_and_failure_is_scheduled(self):
  with tempfile.TemporaryDirectory() as directory:
   outbox = Outbox(str(Path(directory) / "outbox.sqlite3"))
   event_id = outbox.enqueue({"originalUrl": "https://example.com/a"})
   acknowledged = []
   class Ok:
    status_code = 201
    def json(self): return {"eventId": event_id, "payloadHash": "a" * 64, "articleId": 7, "decision": "REVIEW"}
   with patch("runtime.deliver.httpx.post", return_value=Ok()):
    self.assertEqual(1, OutboxDeliverer(outbox, "http://x", "t", acknowledged.append).deliver_ready())
   self.assertEqual([], outbox.ready())
   self.assertEqual(["https://example.com/a"], [event["originalUrl"] for event in acknowledged])

   outbox.enqueue({"originalUrl": "https://example.com/b"})
   class Bad: status_code = 503; text = "retry"
   with patch("runtime.deliver.httpx.post", return_value=Bad()):
    self.assertEqual(0, OutboxDeliverer(outbox, "http://x", "t", acknowledged.append).deliver_ready())
   self.assertEqual(0, len(outbox.ready()))
   self.assertEqual(1, outbox.ready_count())

if __name__ == "__main__":
 unittest.main()

