"""Exercise the PowerShell check entry point with simulated native command failures."""
import shutil
import subprocess
import unittest
from pathlib import Path

class CheckScriptExitCodeTest(unittest.TestCase):
    def test_compile_or_test_failure_cannot_be_hidden_by_successful_import(self):
        shell = shutil.which('pwsh') or shutil.which('powershell')
        if shell is None:
            self.skipTest('PowerShell is required for the Windows check entry point')
        root = Path(__file__).resolve().parents[2]
        script = str(root / 'scripts' / 'check.ps1').replace("'", "''")
        for failing_step in ('compileall', 'unittest'):
            with self.subTest(failing_step=failing_step):
                command = '''
function global:python {
    if ($args -contains '%s') { $global:LASTEXITCODE = 17; return }
    if ($args -contains '-c') { Write-Output 'UNEXPECTED_IMPORT_AFTER_FAILURE' }
    $global:LASTEXITCODE = 0
}
& '%s' -SkipBackend -SkipFrontend
''' % (failing_step, script)
                result = subprocess.run([shell, '-NoProfile', '-Command', command], cwd=root,
                                        capture_output=True, text=True, timeout=30, encoding='utf-8', errors='replace')
                self.assertEqual(1, result.returncode, result.stdout + result.stderr)
                self.assertNotIn('UNEXPECTED_IMPORT_AFTER_FAILURE', result.stdout)
                self.assertIn('Baseline checks failed', result.stdout)
