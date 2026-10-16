"""Dry runs must never construct clients for production state or paid services."""
import unittest
from contextlib import ExitStack
from unittest.mock import Mock, patch
from click.testing import CliRunner
from main import main
from models.article import RawArticle

class DryRunIsolationTest(unittest.TestCase):
    def run_dry(self, articles=None, failure=None, extra_args=()):
        spider = Mock(source_name='Offline fixture', LIST_URL='https://example.invalid/list')
        spider.crawl.return_value = articles or []
        spider.crawl.side_effect = failure
        with ExitStack() as stack:
            forbidden = [stack.enter_context(patch('main.' + name, side_effect=AssertionError(name + ' must not run')))
                         for name in ('RedisClient', 'RunState', 'Outbox', 'OutboxDeliverer', 'DeepSeekAgent', 'OssClient', 'CapacityGuard')]
            stack.enter_context(patch('main.ValorantOfficialSpider', return_value=spider))
            stack.enter_context(patch('main.ApexOfficialSpider', return_value=spider))
            result = CliRunner().invoke(main, ['--source', 'valorant-official', '--dry-run', *extra_args])
            for client in forbidden:
                client.assert_not_called()
        return result

    def test_success_processes_fixture_without_state_dependencies(self):
        article = RawArticle(title='VALORANT Patch Notes', raw_html='<p>Update</p>', source='fixture', original_url='https://example.invalid/patch')
        result = self.run_dry([article])
        self.assertEqual(0, result.exit_code, result.output)
        self.assertIn('crawled: 1', result.output)
        self.assertNotIn('Stats:', result.output)

    def test_source_failure_does_not_write_error_counter(self):
        result = self.run_dry(failure=OSError('offline fixture failure'))
        self.assertIn('offline fixture failure', result.output)
        self.assertNotIn('NoneType', result.output)

    def test_empty_list_does_not_write_last_run_time(self):
        result = self.run_dry()
        self.assertEqual(0, result.exit_code, result.output)
        self.assertNotIn('Stats:', result.output)

    def test_stats_combination_is_rejected_before_redis_connection(self):
        result = self.run_dry(extra_args=['--stats'])
        self.assertEqual(2, result.exit_code)
        self.assertIn('cannot be combined', result.output)
