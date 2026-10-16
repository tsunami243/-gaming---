import json
import tempfile
import unittest
from pathlib import Path

from quality.prepare_candidates import prepare, write_jsonl
from quality.evaluate import evaluate


class QualityCandidatePreparationTest(unittest.TestCase):
    def test_normalizes_deduplicates_and_never_emits_a_frozen_label(self):
        records = [
            {"sourceCode": "valorant-official", "language": "en", "title": " VALORANT   Patch ", "text": "Official\nnotes", "originalUrl": "https://example.invalid/a"},
            {"sourceCode": "valorant-official", "language": "en", "title": "VALORANT Patch", "text": "Official notes", "originalUrl": "https://example.invalid/a"},
        ]
        candidates = prepare(records)
        self.assertEqual(1, len(candidates))
        candidate = candidates[0]
        self.assertEqual("candidate", candidate["split"])
        self.assertEqual("PENDING_HUMAN_REVIEW", candidate["labelStatus"])
        self.assertNotIn("expectedDecision", candidate)
        self.assertEqual("VALORANT Patch", candidate["title"])

    def test_refuses_overwrite_and_evaluator_rejects_unlabeled_candidates(self):
        candidate = prepare([{"sourceCode": "cs2-official", "language": "en", "title": "CS2 Update", "text": "Official patch", "originalUrl": "https://example.invalid/cs2"}])[0]
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "candidates.jsonl"
            write_jsonl(path, [candidate])
            with self.assertRaises(FileExistsError):
                write_jsonl(path, [candidate])
        with self.assertRaises(ValueError):
            evaluate([candidate])


if __name__ == "__main__":
    unittest.main()
