import unittest
from types import SimpleNamespace
from filters.fps_rules import classify
from filters.editorial import editorial_decision

class EditorialDecisionTest(unittest.TestCase):
    def model(self, **overrides):
        fields = dict(decision='ACCEPT', game_codes=['cs2'], content_type='patch_notes', is_gaming=True, confidence=1.0)
        fields.update(overrides)
        return SimpleNamespace(**fields)
    def test_model_reject_or_review_cannot_be_ignored(self):
        rule = classify('CS2 更新公告')
        for decision in ['REJECT','REVIEW']:
            self.assertEqual(decision, editorial_decision(rule, self.model(decision=decision)).decision)
    def test_accept_needs_agreement_not_high_confidence(self):
        rule = classify('CS2 更新公告')
        for overrides in [dict(game_codes=['valorant']), dict(content_type='guide'), dict(game_codes=[]), dict(game_codes=['minecraft'])]:
            self.assertEqual('REVIEW', editorial_decision(rule, self.model(**overrides)).decision)
        self.assertEqual('ACCEPT', editorial_decision(rule, self.model(confidence=0.1)).decision)
    def test_hard_rejection_and_ambiguity_cannot_be_promoted(self):
        self.assertEqual('REJECT', editorial_decision(classify('CS2 账号交易'), self.model()).decision)
        self.assertEqual('REVIEW', editorial_decision(classify('CS2 和 Apex Legends 活动'), self.model()).decision)
    def test_unknown_rule_type_can_be_proposed_for_manual_review_only(self):
        result = editorial_decision(classify('CS2 新消息'), self.model(content_type='announcement'))
        self.assertEqual('REVIEW', result.decision)
        self.assertEqual('announcement', result.content_type)
