import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch
from runtime.outbox import Outbox
from runtime.deliver import OutboxDeliverer

class DeliveryFailureClassTest(unittest.TestCase):
    def test_bad_event_is_retained_but_not_retried_or_removed_from_budget(self):
        for status in (400,409,413,415,422):
            with self.subTest(status=status), tempfile.TemporaryDirectory() as directory:
                path = Path(directory)/'outbox.sqlite3'; outbox=Outbox(path)
                outbox.enqueue({'title':'fixture'})
                with patch('runtime.deliver.httpx.post',return_value=Mock(status_code=status)) as post:
                    worker=OutboxDeliverer(outbox,'http://fixture','fixture')
                    self.assertEqual(0,worker.deliver_ready()); worker.deliver_ready()
                    post.assert_called_once()
                self.assertEqual(0,Outbox(path).ready_count())
                self.assertEqual(1,Outbox(path).pending_count())

    def test_authentication_and_backpressure_stop_the_entire_delivery_batch(self):
        for status in (401,403,429,503):
            with self.subTest(status=status), tempfile.TemporaryDirectory() as directory:
                outbox=Outbox(Path(directory)/'outbox.sqlite3')
                outbox.enqueue({'title':'one'}); outbox.enqueue({'title':'two'})
                with patch('runtime.deliver.httpx.post',return_value=Mock(status_code=status)) as post:
                    worker=OutboxDeliverer(outbox,'http://fixture','fixture')
                    self.assertEqual(0,worker.deliver_ready())
                    self.assertEqual(f'HTTP {status}',worker.halted_reason)
                    post.assert_called_once()
                self.assertEqual(2,outbox.pending_count())
                self.assertEqual(2,outbox.ready_count())
