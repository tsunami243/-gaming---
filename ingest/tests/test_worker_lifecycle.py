import os
import sqlite3
import subprocess
import sys
import tempfile
import unittest
from concurrent.futures import ThreadPoolExecutor
from contextlib import closing
from pathlib import Path
from unittest.mock import patch
from click.testing import CliRunner
from runtime.state import RunState
from main import main
from settings import settings

class WorkerLifecycleTest(unittest.TestCase):
    def test_abrupt_child_exit_releases_os_lock(self):
        with tempfile.TemporaryDirectory() as directory:
            path = str(Path(directory) / 'state.sqlite3')
            code = "from runtime.state import RunState; import sys,os; s=RunState(sys.argv[1]); print(s.claim_worker(),flush=True); sys.stdin.readline(); os._exit(0)"
            child = subprocess.Popen([sys.executable, '-c', code, path], stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
            try:
                self.assertEqual('True', child.stdout.readline().strip())
                other = RunState(path)
                self.assertFalse(other.claim_worker())
                child.communicate('\n', timeout=10)
                self.assertEqual(0, child.returncode)
                self.assertTrue(other.claim_worker())
                other.release_worker()
            finally:
                if child.poll() is None: child.kill(); child.wait(timeout=10)
                child.stdin.close(); child.stdout.close()

    def test_budget_cannot_be_oversubscribed_by_concurrent_workers(self):
        with tempfile.TemporaryDirectory() as directory:
            state = RunState(str(Path(directory) / 'state.sqlite3'))
            with ThreadPoolExecutor(max_workers=8) as executor:
                reserved = list(executor.map(lambda _: state.reserve_candidate(3), range(20)))
            self.assertEqual(3, sum(reserved))

    def test_legacy_sqlite_lock_is_preserved_and_blocks_new_worker(self):
        with tempfile.TemporaryDirectory() as directory:
            path = str(Path(directory) / 'state.sqlite3'); state = RunState(path)
            with closing(sqlite3.connect(path)) as db:
                db.execute("insert into worker_lock values ('ingest','legacy')"); db.commit()
            self.assertFalse(state.claim_worker())
            state.release_worker()
            with closing(sqlite3.connect(path)) as db:
                self.assertEqual(1, db.execute('select count(*) from worker_lock').fetchone()[0])

    def test_initialization_failure_releases_claimed_lock(self):
        with tempfile.TemporaryDirectory() as directory:
            path = str(Path(directory) / 'state.sqlite3')
            with patch.object(settings,'RUNTIME_STATE_PATH',path), patch.object(settings,'INGEST_INTERNAL_BASE_URL','http://fixture'), patch.object(settings,'INGEST_SERVICE_TOKEN','fixture'), patch('main.RedisClient'), patch('main.DeepSeekAgent',side_effect=RuntimeError('fixture failure')):
                result = CliRunner().invoke(main, ['--source','valorant-official'])
            self.assertNotEqual(0, result.exit_code)
            state = RunState(path)
            self.assertTrue(state.claim_worker()); state.release_worker()
