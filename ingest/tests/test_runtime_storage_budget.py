import tempfile
import unittest
from pathlib import Path

from runtime.outbox import Outbox, RuntimeStorageLimitExceeded


class RuntimeStorageBudgetTest(unittest.TestCase):
    def test_combined_state_and_outbox_bytes_block_new_event_without_writing_it(self):
        with tempfile.TemporaryDirectory() as directory:
            outbox_path = Path(directory) / "outbox.sqlite3"
            state_path = Path(directory) / "state.sqlite3"
            state_path.write_bytes(b"x" * 40)
            outbox = Outbox(outbox_path, runtime_paths=(outbox_path, state_path), max_runtime_bytes=100)
            with self.assertRaises(RuntimeStorageLimitExceeded):
                outbox.enqueue({"eventId": "blocked", "title": "x" * 60})
            self.assertEqual([], outbox.ready())

    def test_journal_companions_are_counted_once_and_nonexistent_paths_cost_zero(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "outbox.sqlite3"
            wal = Path(str(path) + "-wal")
            outbox = Outbox(path, runtime_paths=(path, path), max_runtime_bytes=100_000)
            before = outbox.storage_bytes()
            wal.write_bytes(b"b" * 15)
            self.assertEqual(before + 15, outbox.storage_bytes())
            self.assertEqual(before + 15, outbox.ensure_storage_capacity())


if __name__ == "__main__":
    unittest.main()

