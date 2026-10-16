import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch
from runtime.outbox import Outbox
from runtime.deliver import OutboxDeliverer

class PersistentDeliveryPauseTest(unittest.TestCase):
    def test_auth_pause_survives_restart_and_passage_of_time(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'queue.sqlite3'; outbox=Outbox(path); outbox.enqueue({'title':'fixture'})
            with patch('runtime.deliver.httpx.post', return_value=Mock(status_code=403)) as post:
                OutboxDeliverer(outbox,'http://fixture','fixture').deliver_ready()
                with patch('runtime.outbox.time.time', return_value=99_999_999_999):
                    worker=OutboxDeliverer(Outbox(path),'http://fixture','changed-token')
                    self.assertEqual(0,worker.deliver_ready())
                    self.assertEqual('HTTP 403',worker.halted_reason)
                post.assert_called_once()
            self.assertEqual(1,Outbox(path).pending_count())

    def test_backpressure_waits_then_retries_same_event(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'queue.sqlite3'; outbox=Outbox(path); event_id=outbox.enqueue({'title':'fixture'})
            response=Mock(status_code=429, headers={'Retry-After':'1800'})
            with patch('runtime.outbox.time.time', return_value=1000), patch('runtime.deliver.httpx.post', return_value=response):
                OutboxDeliverer(outbox,'http://fixture','fixture').deliver_ready()
            with patch('runtime.outbox.time.time', return_value=2799), patch('runtime.deliver.httpx.post') as post:
                self.assertEqual(0,OutboxDeliverer(Outbox(path),'http://fixture','fixture').deliver_ready()); post.assert_not_called()
            receipt=Mock(status_code=200)
            receipt.json.return_value=dict(eventId=event_id,payloadHash='a'*64,articleId=7,decision='REVIEW')
            with patch('runtime.outbox.time.time',return_value=2800), patch('runtime.deliver.httpx.post',return_value=receipt) as post:
                self.assertEqual(1,OutboxDeliverer(Outbox(path),'http://fixture','fixture').deliver_ready())
                self.assertEqual(event_id,post.call_args.kwargs['json']['eventId'])
            self.assertEqual(0,outbox.pending_count())
