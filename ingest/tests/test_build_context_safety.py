import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('build_context_check', ROOT/'scripts/check-build-context.py')
check = importlib.util.module_from_spec(spec); spec.loader.exec_module(check)
class BuildContextSafetyTest(unittest.TestCase):
    def test_repository_configuration_passes_static_scope_checks(self):
        check.validate((ROOT/'.dockerignore').read_text(encoding='utf-8'), (ROOT/'deploy/Dockerfile.ingest').read_text(encoding='utf-8'))
    def test_missing_secret_exclusion_or_reinclusion_fails(self):
        valid = '\n'.join(check.REQUIRED_EXCLUSIONS)
        with self.assertRaises(ValueError): check.validate(valid.replace('**/.env.*',''), 'COPY ingest/main.py ./')
        with self.assertRaises(ValueError): check.validate(valid+'\n!ingest/.env', 'COPY ingest/main.py ./')
    def test_broad_copy_fails(self):
        valid = '\n'.join(check.REQUIRED_EXCLUSIONS)
        for copy in ('COPY ingest ./', 'COPY ingest/ /app/', 'ADD . /app/'):
            with self.assertRaises(ValueError): check.validate(valid, copy)
