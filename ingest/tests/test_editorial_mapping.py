import json
import unittest
from unittest.mock import Mock, patch
from agent.deepseek_client import DeepSeekAgent
from models.article import RawArticle

class EditorialMappingTest(unittest.TestCase):
    def test_structured_decision_survives_agent_mapping(self):
        agent = DeepSeekAgent.__new__(DeepSeekAgent)
        agent._client = Mock(); agent._model = 'offline-fixture'
        payload = dict(title='CS2', content='text', summary='summary', decision='REJECT', game_codes=['cs2'], content_type='guide', reason_code='OFF_TOPIC')
        agent._client.chat.completions.create.return_value = Mock(choices=[Mock(message=Mock(content=json.dumps(payload)))])
        raw = RawArticle(title='fixture', raw_html='fixture', source='fixture', original_url='https://example.invalid')
        result = agent._call(raw, 'fixture')
        self.assertEqual('REJECT', result.decision)
        self.assertEqual(['cs2'], result.game_codes)
        self.assertEqual('guide', result.content_type)
    def test_model_failure_cannot_reach_acceptance_as_raw_fallback(self):
        agent = DeepSeekAgent.__new__(DeepSeekAgent)
        raw = RawArticle(title='CS2 update', raw_html='unsafe fallback', source='fixture', original_url='https://example.invalid')
        with patch.object(agent, '_call', side_effect=ValueError('fixture')), patch('agent.deepseek_client.time.sleep'):
            result = agent.process(raw)
        self.assertFalse(result.is_gaming)
        self.assertEqual('REJECT', result.decision)
        self.assertEqual('PROCESSING_FAILED', result.reason_code)
