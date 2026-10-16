import json
import tempfile
import unittest
from pathlib import Path
from runtime.outbox import Outbox
from runtime.inspect_outbox import inspect_outbox

class ReadOnlyOutboxInspectionTest(unittest.TestCase):
    def test_reports_pause_and_quarantine_without_exposing_payload(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'queue.sqlite3'; outbox=Outbox(path)
            event=outbox.enqueue({'title':'sensitive-fixture','privateKey':'never-print-fixture'})
            outbox.quarantine(event,'HTTP 422'); outbox.pause_delivery('HTTP 403')
            before=path.read_bytes()
            report=inspect_outbox(path)
            self.assertEqual(1,report['unresolved'])
            self.assertEqual(1,report['counts']['QUARANTINED'])
            self.assertTrue(report['deliveryPause']['requiresOperatorReview'])
            self.assertNotIn('never-print-fixture',json.dumps(report))
            self.assertNotIn('sensitive-fixture',json.dumps(report))
            self.assertEqual(before,path.read_bytes())
    def test_missing_database_is_not_created(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'missing.sqlite3'
            with self.assertRaises(FileNotFoundError): inspect_outbox(path)
            self.assertFalse(path.exists())
    def test_event_listing_is_bounded_and_old_pause_details_redacted(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'queue.sqlite3'; outbox=Outbox(path)
            for i in range(5): outbox.enqueue({'eventId':str(i),'title':'fixture'})
            outbox.pause_delivery('old diagnostic containing fixture-secret')
            report=inspect_outbox(path,2)
            self.assertEqual(2,report['displayed']); self.assertEqual(5,report['unresolved'])
            self.assertEqual('PAUSED',report['deliveryPause']['reason'])
            self.assertNotIn('fixture-secret',json.dumps(report))
