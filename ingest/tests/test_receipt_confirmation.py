from contextlib import closing
import sqlite3
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch
from runtime.outbox import Outbox
from runtime.deliver import OutboxDeliverer

class ReceiptConfirmationTest(unittest.TestCase):
    def test_invalid_success_bodies_remain_ready_and_never_mark_dedup(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = Outbox(Path(directory) / 'outbox.sqlite3')
            event_id = outbox.enqueue({'title': 'fixture'})
            valid = dict(eventId=event_id, payloadHash='a'*64, articleId=1, decision='REVIEW')
            cases = [None, {}, [], {'code': 0}, {**valid, 'eventId': 'other'},
                     {**valid, 'payloadHash': 'bad'}, {**valid, 'articleId': None},
                     {**valid, 'articleId': True}, {**valid, 'decision': 'UNKNOWN'}]
            callback = Mock()
            for body in cases:
                with self.subTest(body=body):
                    response = Mock(status_code=200); response.json.return_value = body
                    with patch('runtime.deliver.httpx.post', return_value=response):
                        self.assertEqual(0, OutboxDeliverer(outbox, 'http://fixture', 'fixture', callback).deliver_ready())
                    self.assertEqual(1, outbox.ready_count())
            callback.assert_not_called()

    def test_html_200_does_not_ack(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = Outbox(Path(directory) / 'outbox.sqlite3'); outbox.enqueue({'title': 'fixture'})
            response = Mock(status_code=200); response.json.side_effect = ValueError('not json')
            with patch('runtime.deliver.httpx.post', return_value=response):
                self.assertEqual(0, OutboxDeliverer(outbox, 'http://fixture', 'fixture').deliver_ready())
            self.assertEqual(1, outbox.ready_count())

    def test_committed_replay_survives_restart_and_callback_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'outbox.sqlite3'; outbox = Outbox(path)
            event_id = outbox.enqueue({'title': 'fixture'})
            response = Mock(status_code=200)
            response.json.return_value = dict(eventId=event_id, payloadHash='b'*64, articleId=7, decision='ACCEPT')
            with patch('runtime.deliver.httpx.post', return_value=response):
                self.assertEqual(1, OutboxDeliverer(outbox, 'http://fixture', 'fixture', Mock(side_effect=RuntimeError())).deliver_ready())
            self.assertEqual(0, Outbox(path).ready_count())
            with closing(sqlite3.connect(path)) as db:
                self.assertEqual(('ACKED', 0), db.execute('select status,attempts from outbox').fetchone())

    def test_rejection_is_acknowledged_as_processed_without_article(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox = Outbox(Path(directory) / 'outbox.sqlite3'); event_id = outbox.enqueue({'title': 'fixture'})
            response = Mock(status_code=201)
            response.json.return_value = dict(eventId=event_id, payloadHash='c'*64, articleId=None, decision='REJECT')
            with patch('runtime.deliver.httpx.post', return_value=response):
                self.assertEqual(1, OutboxDeliverer(outbox, 'http://fixture', 'fixture').deliver_ready())
            self.assertEqual(0, outbox.ready_count())
