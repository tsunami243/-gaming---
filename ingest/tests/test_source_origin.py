import unittest
import httpx
from spiders.valorant_official import ValorantOfficialSpider

class SourceOriginTest(unittest.TestCase):
    def make(self, handler):
        spider = ValorantOfficialSpider()
        client = httpx.Client(transport=httpx.MockTransport(handler), follow_redirects=True)
        self.addCleanup(client.close); spider._client = client; spider._lookup_addresses = lambda hostname, port: {"93.184.216.34"}
        return spider
    def test_unsafe_initial_urls_never_reach_transport(self):
        seen = []
        spider = self.make(lambda request: seen.append(str(request.url)))
        for url in ['http://playvalorant.com/news', 'https://playvalorant.com.evil.invalid/news',
                    'https://127.0.0.1/', 'https://169.254.169.254/', 'https://playvalorant.com:8080/news',
                    'https://user:pass@playvalorant.com/news', 'file:///etc/passwd']:
            with self.subTest(url=url), self.assertRaises(ValueError): spider.get_text(url)
        self.assertEqual([], seen)
    def test_private_dns_result_is_rejected_before_transport(self):
        seen = []
        spider = self.make(lambda request: seen.append(str(request.url)))
        spider._lookup_addresses = lambda hostname, port: {"127.0.0.1"}
        with self.assertRaises(ValueError): spider.get_text("https://playvalorant.com/start")
        self.assertEqual([], seen)

    def test_cross_origin_redirect_is_rejected_before_next_request(self):
        for location in ['https://evil.invalid/news', 'http://playvalorant.com/news', 'http://127.0.0.1/admin']:
            seen = []
            def handler(request):
                seen.append(str(request.url)); return httpx.Response(302, headers={'location': location})
            with self.subTest(location=location), self.assertRaises(ValueError): self.make(handler).get_text('https://playvalorant.com/start')
            self.assertEqual(1, len(seen))
    def test_relative_same_origin_redirect_is_allowed(self):
        seen = []
        def handler(request):
            seen.append(str(request.url))
            return httpx.Response(302, headers={'location': '/end'}) if request.url.path == '/start' else httpx.Response(200, text='ok')
        self.assertEqual('ok', self.make(handler).get_text('https://playvalorant.com/start'))
        self.assertEqual(['https://playvalorant.com/start','https://playvalorant.com/end'], seen)
    def test_redirect_loop_stops_at_three_hops(self):
        seen = []
        def handler(request):
            seen.append(str(request.url)); return httpx.Response(302, headers={'location': '/loop'})
        with self.assertRaises(ValueError): self.make(handler).get_text('https://playvalorant.com/start')
        self.assertEqual(4, len(seen))


