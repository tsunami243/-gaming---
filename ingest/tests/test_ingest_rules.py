import tempfile
import unittest
from pathlib import Path
from filters.fps_rules import classify
from runtime.state import RunState

class FpsRulesTest(unittest.TestCase):
    def test_accepts_single_game_patch_note(self):
        result = classify('CS2 更新公告')
        self.assertEqual('ACCEPT', result.decision)
        self.assertEqual(('cs2',), result.game_codes)
        self.assertEqual('patch_notes', result.content_type)

    def test_rejects_non_target_and_disallowed_content(self):
        self.assertEqual('GAME_NOT_ALLOWED', classify('某手游主播账号交易').reason_code)
        self.assertEqual('DISALLOWED_TYPE', classify('无畏契约 账号交易').reason_code)

    def test_official_announcement_mentioning_streamers_is_not_a_hard_rejection(self):
        # Regression: a real Delta Force official event post mentions 主播 in its body.
        # That alone must not be treated as streamer gossip and hard-rejected.
        result = classify(
            '二洲年庆典即将启幕！山城集结，共赴洲年之约！',
            '三角洲行动二洲年庆典邀请多位主播与玩家同场互动，现场还有限定周边与线下活动。',
        )
        self.assertNotEqual('REJECT', result.decision)
        self.assertNotEqual('DISALLOWED_TYPE', result.reason_code)
        self.assertEqual(('delta-force',), result.game_codes)


    def test_explicit_event_headline_wins_over_patch_words_inside_body(self):
        result = classify('Apex Legends Event', 'Patch changes are also mentioned in this event overview')
        self.assertEqual('event', result.content_type)
        self.assertEqual('ACCEPT', result.decision)

    def test_marks_ambiguous_multi_game_content_for_review(self):
        result = classify('CS2 与 Apex Legends 联动活动')
        self.assertEqual('REVIEW', result.decision)
        self.assertEqual('LOW_EVIDENCE', result.reason_code)

    def test_domestic_esports_wording_resolves_to_a_single_type(self):
        # Real Delta Force headline: it says 职业联赛/总冠军 rather than 赛事/战队.
        # Before the keyword expansion this fell through to REVIEW with no content_type.
        result = classify('春夏双冠！恭喜TES夺得2026三角洲行动烽火职业联赛夏季赛总冠军！')
        self.assertEqual('ACCEPT', result.decision)
        self.assertEqual('esports', result.content_type)
        self.assertEqual(('delta-force',), result.game_codes)

    def test_official_celebration_wording_counts_as_an_event(self):
        result = classify('三角洲行动二洲年庆典即将启幕')
        self.assertEqual('ACCEPT', result.decision)
        self.assertEqual('event', result.content_type)

class RuntimeStateTest(unittest.TestCase):
    def test_lock_and_daily_budget_are_persistent_and_bounded(self):
        with tempfile.TemporaryDirectory() as directory:
            state = RunState(str(Path(directory) / 'state.sqlite3'))
            self.assertTrue(state.claim_worker())
            self.assertFalse(state.claim_worker())
            self.assertTrue(state.reserve_candidate(2))
            self.assertTrue(state.reserve_candidate(2))
            self.assertFalse(state.reserve_candidate(2))
            state.release_worker()
            self.assertTrue(state.claim_worker())
            state.release_worker()

if __name__ == '__main__':
    unittest.main()
