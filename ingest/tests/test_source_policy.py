import unittest
from unittest.mock import patch
from main import source_is_enabled
from settings import settings

class SourcePolicyTest(unittest.TestCase):
    def test_source_policy_allows_dry_run_without_network(self):
        self.assertTrue(source_is_enabled('valorant-official', True))

    def test_source_policy_honors_server_pause(self):
        old_url, old_token = settings.INGEST_INTERNAL_BASE_URL, settings.INGEST_SERVICE_TOKEN
        settings.INGEST_INTERNAL_BASE_URL, settings.INGEST_SERVICE_TOKEN = 'http://api', 'token'
        try:
            class Response:
                def raise_for_status(self): pass
                def json(self): return {'enabled': False, 'verificationStatus': 'PAUSED'}
            with patch('main.httpx.get', return_value=Response()):
                self.assertFalse(source_is_enabled('valorant-official', False))
        finally:
            settings.INGEST_INTERNAL_BASE_URL, settings.INGEST_SERVICE_TOKEN = old_url, old_token

if __name__ == '__main__':
    unittest.main()

class SourcePolicyFailClosedTest(unittest.TestCase):
    def test_missing_config_is_disabled_without_network(self):
        for url, token in [('', ''), ('http://api', ''), ('', 'fixture')]:
            with patch.object(settings, 'INGEST_INTERNAL_BASE_URL', url), patch.object(settings, 'INGEST_SERVICE_TOKEN', token), patch('main.httpx.get') as get:
                self.assertFalse(source_is_enabled('valorant-official', False))
                get.assert_not_called()

    def test_only_literal_true_with_runnable_status_is_allowed(self):
        from unittest.mock import Mock
        with patch.object(settings, 'INGEST_INTERNAL_BASE_URL', 'http://api'), patch.object(settings, 'INGEST_SERVICE_TOKEN', 'fixture'):
            for policy in [None, [], {}, {'enabled': 'false', 'verificationStatus': 'SHADOW'},
                           {'enabled': 1, 'verificationStatus': 'SHADOW'}, {'enabled': True, 'verificationStatus': 'PAUSED'},
                           {'enabled': True}, {'enabled': True, 'verificationStatus': 'CANDIDATE'}]:
                response = Mock(); response.json.return_value = policy
                with patch('main.httpx.get', return_value=response):
                    self.assertFalse(source_is_enabled('valorant-official', False))
            for status in ['SHADOW', 'AUTO_ENABLED']:
                response = Mock(); response.json.return_value = {'enabled': True, 'verificationStatus': status}
                with patch('main.httpx.get', return_value=response):
                    self.assertTrue(source_is_enabled('valorant-official', False))

    def test_cli_missing_config_stops_before_redis_or_paid_clients(self):
        from click.testing import CliRunner
        from main import main
        with patch.object(settings, 'INGEST_INTERNAL_BASE_URL', ''), patch('main.RedisClient') as redis, patch('main.DeepSeekAgent') as ai, patch('main.Outbox') as outbox:
            result = CliRunner().invoke(main, ['--source', 'valorant-official'])
            self.assertEqual(1, result.exit_code)
            self.assertIn('no collection started', result.output)
            redis.assert_not_called(); ai.assert_not_called(); outbox.assert_not_called()
