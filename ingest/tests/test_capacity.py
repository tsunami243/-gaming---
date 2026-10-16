import json
import tempfile
import time
import unittest
from pathlib import Path
from runtime.capacity import CapacityGuard

class CapacityGuardTest(unittest.TestCase):
    def snapshot(self, used=0, free=20_000_000_000):
        return dict(scope='gameboi-project', observedAt=time.time(), hostAvailableBytes=free,
                    components=dict(images=used, volumes=0, logs=0, temporary=0, backups=0))
    def check(self, snapshot):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'capacity.json'
            path.write_text(json.dumps(snapshot), encoding='utf-8')
            return CapacityGuard(path).check()
    def test_thresholds_use_project_sum_and_exact_decimal_bytes(self):
        for used, expected in [(0,'OK'),(6_999_999_999,'OK'),(7_000_000_000,'WARN'),(8_000_000_000,'STOP'),(10_000_000_000,'HARD_STOP')]:
            with self.subTest(used=used): self.assertEqual(expected, self.check(self.snapshot(used)).state)
        snapshot = self.snapshot(3_000_000_000); snapshot['components']['volumes'] = 5_000_000_000
        self.assertEqual('STOP', self.check(snapshot).state)
    def test_host_headroom_independently_stops_even_small_project(self):
        self.assertEqual('STOP', self.check(self.snapshot(1, 4_999_999_999)).state)
        self.assertEqual('OK', self.check(self.snapshot(1, 5_000_000_000)).state)
    def test_invalid_missing_or_stale_observation_is_unknown(self):
        cases = [None, {}, self.snapshot()]
        cases[-1]['observedAt'] = time.time() - 121
        missing = self.snapshot(); del missing['components']['logs']; cases.append(missing)
        wrong = self.snapshot(); wrong['scope'] = 'whole-host'; cases.append(wrong)
        future = self.snapshot(); future['observedAt'] += 60; cases.append(future)
        bad = self.snapshot(); bad['components']['images'] = -1; cases.append(bad)
        boolean = self.snapshot(); boolean['hostAvailableBytes'] = True; cases.append(boolean)
        for snapshot in cases:
            with self.subTest(snapshot=snapshot): self.assertEqual('UNKNOWN', self.check(snapshot).state)
        self.assertEqual('UNKNOWN', CapacityGuard('').check().state)
    def test_directory_usage_cannot_be_misrepresented_as_project_usage(self):
        with tempfile.TemporaryDirectory() as directory:
            self.assertEqual('UNKNOWN', CapacityGuard(directory).check().state)
    def test_cannot_raise_the_hard_budget(self):
        with self.assertRaises(ValueError): CapacityGuard('', hard_bytes=11_000_000_000)
