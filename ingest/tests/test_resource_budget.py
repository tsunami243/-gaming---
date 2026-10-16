import copy
import importlib.util
import unittest
from pathlib import Path
import yaml
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('resource_check', ROOT/'scripts/check-resource-budget.py')
check=importlib.util.module_from_spec(spec); spec.loader.exec_module(check)

class ResourceBudgetTest(unittest.TestCase):
    def config(self): return yaml.safe_load((ROOT/'deploy/compose.yaml').read_text(encoding='utf-8'))
    def test_design_budget_matches_exact_decimal_bytes(self):
        self.assertEqual(3150000000,check.validate(self.config()))
    def test_missing_oversized_or_unit_ambiguous_caps_fail(self):
        for value in (None, '1200m', 1200000001):
            cfg=self.config(); cfg['services']['backend']['mem_limit']=value
            with self.assertRaises(ValueError): check.validate(cfg)
    def test_extra_services_cannot_bypass_total_budget(self):
        cfg=self.config();cfg['services']['extra']=copy.deepcopy(cfg['services']['mysql'])
        with self.assertRaises(ValueError): check.validate(cfg)
    def test_swap_and_cpu_cannot_silently_expand_budget(self):
        cfg=self.config();cfg['services']['ingest']['memswap_limit']=900000000
        with self.assertRaises(ValueError): check.validate(cfg)
        cfg=self.config();cfg['services']['ingest']['cpus']=2
        with self.assertRaises(ValueError): check.validate(cfg)
