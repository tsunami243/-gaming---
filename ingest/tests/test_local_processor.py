"""The offline processor must replace the paid model without weakening any gate."""
import json
import unittest
from pathlib import Path

from bs4 import BeautifulSoup

from agent.local_processor import LocalArticleProcessor, build_summary, cleaned_html, cleaned_text
from filters.editorial import editorial_decision
from filters.fps_rules import classify
from models.article import RawArticle


class LocalProcessorTest(unittest.TestCase):
    def test_cleaner_drops_chrome_and_keeps_editorial_text(self):
        markup = """
        <div class="cookie-consent">Accept all cookies</div>
        <main>
          <h4 id="toc">Table of Contents</h4>
          <p>Effective September 29th, Apex Legends switches anticheat systems.</p>
          <h2>EA Javelin Anticheat</h2>
          <p>Players on PC will be migrated automatically.</p>
          <script>var tracking = 1;</script>
        </main>
        <footer class="site-footer">Copyright EA</footer>
        """
        text = cleaned_text(markup)
        self.assertIn('Effective September 29th', text)
        self.assertIn('EA Javelin Anticheat', text)
        self.assertNotIn('cookies', text.lower())
        self.assertNotIn('tracking', text)
        self.assertNotIn('Copyright', text)

    def test_cleaned_html_is_class_free_and_escaped(self):
        markup = '<div class="x y z"><p>A &amp; B <b>bold</b></p></div>'
        result = cleaned_html(markup)
        self.assertEqual('<p>A &amp; B bold</p>', result)
        self.assertNotIn('class=', result)

    def test_summary_skips_leading_heading_fragments(self):
        text = "INTRO\nWelcome back to Split 2 of Apex Legends: Marked. This patch brings balancing changes."
        summary = build_summary(text)
        self.assertTrue(summary.startswith('Welcome back'))
        self.assertNotIn('INTRO', summary)

    def test_summary_is_empty_for_empty_body(self):
        self.assertEqual('', build_summary(''))

    def test_short_article_can_reach_accept_without_the_model(self):
        raw = RawArticle(
            title='Apex Legends: Marked Patch Notes',
            raw_html='<main><p>' + ('Balance changes arrive for every Legend. ' * 8) + '</p></main>',
            source='Apex Legends 官方公告（EA）',
            original_url='https://www.ea.com/games/apex-legends/apex-legends/news/marked',
        )
        processor = LocalArticleProcessor()
        prefilter = classify(raw.title, raw.raw_html)
        self.assertEqual('ACCEPT', prefilter.decision)
        processed = processor.process(raw, prefilter)
        final = editorial_decision(prefilter, processed)
        self.assertEqual('ACCEPT', final.decision)
        self.assertEqual('patch_notes', final.content_type)
        self.assertEqual(['apex-legends'], processed.game_codes)
        self.assertEqual(json.loads(processed.tags), ['Apex Legends', '版本更新'])

    def test_ambiguous_rule_outcome_stays_review(self):
        raw = RawArticle(
            title='Apex Legends 与 VALORANT 联动活动',
            raw_html='<main><p>' + ('Two games meet in one Arena this season. ' * 8) + '</p></main>',
            source='fixture',
            original_url='https://example.invalid/crossover',
        )
        processor = LocalArticleProcessor()
        prefilter = classify(raw.title, raw.raw_html)
        self.assertEqual('REVIEW', prefilter.decision)
        processed = processor.process(raw, prefilter)
        self.assertEqual('REVIEW', editorial_decision(prefilter, processed).decision)
        self.assertIsNone(processed.content_type)

    def test_stub_body_cannot_be_auto_accepted(self):
        raw = RawArticle(
            title='Apex Legends Patch Notes',
            raw_html='<main><p>Short.</p></main>',
            source='fixture',
            original_url='https://example.invalid/stub',
        )
        processor = LocalArticleProcessor()
        prefilter = classify(raw.title, raw.raw_html)
        processed = processor.process(raw, prefilter)
        self.assertEqual('REVIEW', processed.decision)
        self.assertEqual('LOCAL_REVIEW', processed.reason_code)
        self.assertEqual('', processed.content)
        self.assertEqual('REVIEW', editorial_decision(prefilter, processed).decision)

    def test_disallowed_content_is_rejected_locally(self):
        raw = RawArticle(
            title='Apex Legends 账号交易',
            raw_html='<main><p>' + ('Buy and sell accounts safely here. ' * 8) + '</p></main>',
            source='fixture',
            original_url='https://example.invalid/boosting',
        )
        processor = LocalArticleProcessor()
        prefilter = classify(raw.title, raw.raw_html)
        processed = processor.process(raw, prefilter)
        self.assertEqual('REJECT', editorial_decision(prefilter, processed).decision)

    def test_captured_ea_fixture_cleans_and_accepts_offline(self):
        root = Path(__file__).parent / 'fixtures'
        detail = root / 'apex_marked_midseason_patch_notes_2026-09-20.html'
        if not detail.is_file():
            self.skipTest('captured Apex fixture is unavailable')
        raw = RawArticle(
            title='Apex Legends™: Marked Midseason Patch Notes',
            raw_html=detail.read_text(encoding='utf-8'),
            source='Apex Legends 官方公告（EA）',
            original_url='https://www.ea.com/games/apex-legends/apex-legends/news/marked-midseason-patch-notes',
        )
        processor = LocalArticleProcessor()
        prefilter = classify(raw.title, raw.raw_html)
        processed = processor.process(raw, prefilter)
        self.assertEqual('ACCEPT', editorial_decision(prefilter, processed).decision)
        self.assertGreater(len(processed.content), 1000)
        self.assertNotIn('class="', processed.content)


if __name__ == '__main__':
    unittest.main()
