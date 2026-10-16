import unittest
from types import SimpleNamespace
from unittest.mock import Mock, patch
from main import process_one_source
from models.article import RawArticle, ProcessedArticle
from settings import settings

class CandidateAdmissionTest(unittest.TestCase):
    def setup_pipeline(self):
        raw = RawArticle(title='VALORANT Patch Notes', raw_html='<p>patch</p>', source='fixture', original_url='https://example.invalid/1')
        spider = Mock(source_name='fixture', LIST_URL='https://example.invalid/list')
        spider.crawl.return_value = [raw, raw.model_copy(update={'original_url':'https://example.invalid/2'})]
        agent = Mock()
        agent.process.return_value = ProcessedArticle(title='无畏契约更新', content='正文'*40, category_id=8, cover_img='',
            summary='中文摘要', source='fixture', original_url=raw.original_url, crawled_at=raw.crawled_at.isoformat())
        redis = Mock(); redis.is_crawled.return_value = False
        state = Mock(); state.reserve_candidate.return_value = True
        outbox = Mock(); outbox.pending_count.return_value = 0
        return spider, agent, Mock(), outbox, redis, state

    def test_unknown_capacity_blocks_before_budget_and_ai(self):
        args = self.setup_pipeline()
        with patch('main.CapacityGuard') as guard:
            guard.return_value.check.return_value = SimpleNamespace(accepts_new_ingest=False)
            self.assertEqual((0,0), process_one_source('fixture', *args, False))
        args[1].process.assert_not_called(); args[5].reserve_candidate.assert_not_called()
        args[3].enqueue.assert_not_called()

    def test_outbox_limit_stops_next_candidate_and_never_uploads_images(self):
        args = self.setup_pipeline()
        args[3].pending_count.side_effect = [settings.MAX_OUTBOX_PENDING-1, settings.MAX_OUTBOX_PENDING]
        with patch('main.CapacityGuard') as guard:
            guard.return_value.check.return_value = SimpleNamespace(accepts_new_ingest=True)
            self.assertEqual((1,1), process_one_source('fixture', *args, False))
        args[1].process.assert_called_once(); args[3].enqueue.assert_called_once()
        args[2].upload_article_images.assert_not_called()

    def test_capacity_is_rechecked_between_candidates(self):
        args = self.setup_pipeline()
        with patch('main.CapacityGuard') as guard:
            guard.return_value.check.side_effect = [SimpleNamespace(accepts_new_ingest=True), SimpleNamespace(accepts_new_ingest=False)]
            self.assertEqual((1,1), process_one_source('fixture', *args, False))
        args[1].process.assert_called_once()
