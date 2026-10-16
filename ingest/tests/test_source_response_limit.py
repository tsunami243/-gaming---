import gzip
import unittest
import httpx
from spiders.valorant_official import ValorantOfficialSpider

class SourceResponseLimitTest(unittest.TestCase):
    def spider(self, response):
        spider = ValorantOfficialSpider()
        spider.LIST_URL = "https://example.invalid"
        client = httpx.Client(transport=httpx.MockTransport(lambda request: response))
        self.addCleanup(client.close)
        spider._client = client
        spider._lookup_addresses = lambda hostname, port: {"93.184.216.34"}
        return spider
    def test_normal_text_is_decoded_and_response_closed(self):
        response = httpx.Response(200, text='<h1>中文</h1>')
        self.assertEqual('中文', self.spider(response).get_soup('https://example.invalid').h1.text)
        self.assertTrue(response.is_closed)
    def test_exact_decoded_limit_allowed_and_one_byte_over_rejected(self):
        for length, valid in [(2_000_000,True), (2_000_001,False)]:
            response = httpx.Response(200, content=b'a'*length)
            spider = self.spider(response)
            if valid: self.assertEqual(length, len(spider.get_text('https://example.invalid')))
            else:
                with self.assertRaises(ValueError): spider.get_text('https://example.invalid')
            self.assertTrue(response.is_closed)
    def test_compression_does_not_bypass_decoded_limit(self):
        compressed = gzip.compress(b'a'*2_000_001)
        self.assertLess(len(compressed), 10000)
        response = httpx.Response(200, headers={'Content-Encoding':'gzip'}, content=compressed)
        with self.assertRaises(ValueError): self.spider(response).get_text('https://example.invalid')
        self.assertTrue(response.is_closed)
    def test_http_failure_does_not_reach_parser(self):
        response = httpx.Response(503, text='maintenance')
        with self.assertRaises(httpx.HTTPStatusError): self.spider(response).get_soup('https://example.invalid')
        self.assertTrue(response.is_closed)

