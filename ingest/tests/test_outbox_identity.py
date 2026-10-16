import tempfile
import unittest
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from runtime.outbox import Outbox

class OutboxIdentityTest(unittest.TestCase):
    def test_conflict_is_explicit_and_original_payload_is_unchanged(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = Outbox(Path(directory) / 'outbox.sqlite3')
            original = {'eventId': 'fixed', 'title': 'original'}
            outbox.enqueue(original)
            with self.assertRaises(ValueError): outbox.enqueue({'eventId': 'fixed', 'title': 'changed'})
            self.assertEqual([('fixed', original)], outbox.ready())
            self.assertEqual('fixed', outbox.enqueue(dict(reversed(list(original.items())))))
            outbox.ack('fixed')
            outbox.enqueue(original)
            self.assertEqual(0, outbox.ready_count())

    def test_enqueue_does_not_modify_caller_payload(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = Outbox(Path(directory) / 'outbox.sqlite3'); payload = {'title': 'fixture'}
            event_id = outbox.enqueue(payload)
            self.assertNotIn('eventId', payload)
            self.assertEqual(event_id, outbox.ready()[0][1]['eventId'])

    def test_read_is_bounded_and_remaining_rows_are_not_lost(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = Outbox(Path(directory) / 'outbox.sqlite3')
            for index in range(55): outbox.enqueue({'eventId': f'e{index:03}', 'title': 'fixture'})
            first = outbox.ready()
            self.assertEqual(50, len(first)); self.assertEqual(55, outbox.ready_count())
            for event_id, _ in first: outbox.ack(event_id)
            self.assertEqual(5, len(outbox.ready()))
            for limit in (0, -1, 201, True):
                with self.assertRaises(ValueError): outbox.ready(limit)

    def test_simultaneous_duplicate_enqueues_have_one_row(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = Outbox(Path(directory) / 'outbox.sqlite3')
            with ThreadPoolExecutor(max_workers=4) as executor:
                ids = list(executor.map(lambda _: outbox.enqueue({'eventId': 'fixed', 'title': 'fixture'}), range(10)))
            self.assertEqual(['fixed'] * 10, ids)
            self.assertEqual(1, outbox.ready_count())
