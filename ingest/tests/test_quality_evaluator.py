import unittest
from pathlib import Path
from quality.evaluate import evaluate, load
class QualityEvaluatorTest(unittest.TestCase):
 def test_small_smoke_set_reports_metrics_but_cannot_enable_auto_publish(self):
  report=evaluate(load(Path(__file__).parents[1]/'quality'/'fixtures'/'smoke.jsonl'))
  self.assertEqual(4,report['sampleCount']);self.assertEqual(0,report['leakedNegativeAutoAccepts']);self.assertFalse(report['eligibleForAutoPublish'])
if __name__=='__main__':unittest.main()

class QualityDatasetGateTest(unittest.TestCase):
    def test_duplicating_positive_cannot_satisfy_frozen_gate(self):
        row = dict(id='same', title='CS2 更新公告', expectedDecision='ACCEPT', expectedGameCodes=['cs2'], expectedContentType='patch_notes', split='frozen', sourceCode='fixture', language='zh-CN')
        report = evaluate([row.copy() for _ in range(160)])
        self.assertFalse(report['eligibleForAutoPublish'])
        self.assertIn('duplicate sample content', report['datasetIssues'])
        self.assertEqual(1, report['uniqueContentCount'])

    def test_wrong_game_or_type_is_not_counted_as_correct_auto_accept(self):
        row = dict(id='one', title='CS2 更新公告', expectedDecision='ACCEPT', expectedGameCodes=['valorant'], expectedContentType='guide', split='frozen', sourceCode='fixture', language='zh-CN')
        report = evaluate([row])
        self.assertEqual(1, report['autoAccepted'])
        self.assertEqual(0, report['correctAutoAccepted'])
        self.assertEqual(0, report['autoPrecision'])

    def test_review_labels_are_not_in_clear_positive_coverage_denominator(self):
        rows = [dict(id='one', title='CS2 更新公告', expectedDecision='ACCEPT', expectedGameCodes=['cs2'], expectedContentType='patch_notes'),
                dict(id='two', title='unclear', expectedDecision='REVIEW', expectedGameCodes=[])]
        report = evaluate(rows)
        self.assertEqual(1, report['positiveCount'])
        self.assertEqual(1, report['coveredPositiveCount'])
        self.assertIn('non-frozen or undeclared split', report['datasetIssues'])

    def test_invalid_label_fails_instead_of_being_silently_ignored(self):
        with self.assertRaises(ValueError): evaluate([dict(id='x',title='CS2',expectedDecision='TYPO')])
