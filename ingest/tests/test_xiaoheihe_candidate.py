import unittest
from unittest.mock import patch

from click.testing import CliRunner

from settings import settings
from main import main


class XiaoheiheCandidateSourceTest(unittest.TestCase):
    def test_candidate_source_is_known_but_never_starts_collection(self):
        runner = CliRunner()
        with patch('main.RedisClient') as redis, patch('main.DeepSeekAgent') as agent, patch('main.Outbox') as outbox, patch('main.httpx.get') as http_get:
            result = runner.invoke(main, ['--source', 'xiaoheihe-community', '--dry-run'])

        self.assertEqual(2, result.exit_code)
        self.assertIn('CANDIDATE', result.output)
        self.assertIn('no collection started', result.output)
        redis.assert_not_called()
        agent.assert_not_called()
        outbox.assert_not_called()
        http_get.assert_not_called()

    def test_community_summary_limits_are_safe_by_default(self):
        self.assertFalse(settings.XIAOHEIHE_SUMMARY_ENABLED)
        self.assertEqual(20, settings.XIAOHEIHE_SUMMARY_DAILY_LIMIT)
        self.assertEqual(1200, settings.XIAOHEIHE_SUMMARY_INPUT_MAX_CHARS)
        self.assertEqual(220, settings.XIAOHEIHE_SUMMARY_OUTPUT_MAX_CHARS)
        self.assertEqual(1, settings.XIAOHEIHE_SUMMARY_MAX_ATTEMPTS)


if __name__ == '__main__':
    unittest.main()
