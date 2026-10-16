import json
import tempfile
import unittest
from pathlib import Path
from runtime.outbox import Outbox, PayloadTooLarge, MAX_EVENT_BYTES

class OutboxPayloadBoundsTest(unittest.TestCase):
    def test_utf8_bytes_not_character_count_determine_limit(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox=Outbox(Path(directory)/'outbox.sqlite3')
            original={'eventId':'stable','content':'中'*90_000}
            self.assertLess(len(original['content']),MAX_EVENT_BYTES)
            with self.assertRaises(PayloadTooLarge): outbox.enqueue(original)
            self.assertEqual(0,outbox.pending_count())
            self.assertEqual('中'*90_000,original['content'])
    def test_exact_bound_can_queue_but_one_byte_above_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox=Outbox(Path(directory)/'outbox.sqlite3')
            event={'eventId':'a','content':''}
            overhead=len(json.dumps(event,ensure_ascii=False,sort_keys=True).encode())
            event['content']='a'*(MAX_EVENT_BYTES-overhead)
            self.assertEqual('a',outbox.enqueue(event))
            too_big={**event,'eventId':'b','content':event['content']+'x'}
            with self.assertRaises(PayloadTooLarge): outbox.enqueue(too_big)
            self.assertEqual(1,outbox.pending_count())
            self.assertEqual(event,outbox.ready()[0][1])
    def test_non_finite_json_is_rejected_without_changing_pending_data(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox=Outbox(Path(directory)/'outbox.sqlite3')
            outbox.enqueue({'eventId':'existing','title':'keep'})
            with self.assertRaises(ValueError): outbox.enqueue({'value':float('nan')})
            self.assertEqual(1,outbox.pending_count())
