"""Static defense-in-depth lint; not a substitute for inspecting a built image."""
from pathlib import Path

REQUIRED_EXCLUSIONS = {'**/.env', '**/.env.*', '**/*.pem', '**/*.key', '**/*.sqlite3*', '**/__pycache__/', '.git/', 'target/'}

def validate(ignore_text, ingest_dockerfile):
    rules = {line.strip() for line in ignore_text.splitlines() if line.strip() and not line.lstrip().startswith('#')}
    missing = REQUIRED_EXCLUSIONS - rules
    if missing: raise ValueError('Missing credential/state exclusions: ' + ', '.join(sorted(missing)))
    if any(rule.startswith('!') for rule in rules): raise ValueError('Docker context re-inclusions require explicit security review')
    for line in ingest_dockerfile.splitlines():
        words = line.strip().split()
        if words and words[0].upper() in ('COPY', 'ADD'):
            if any(word.rstrip('/') in ('.', 'ingest') for word in words[1:-1]):
                raise ValueError('Broad COPY/ADD of developer ingest tree is prohibited')

if __name__ == '__main__':
    root = Path(__file__).resolve().parents[1]
    validate((root / '.dockerignore').read_text(encoding='utf-8'), (root / 'deploy/Dockerfile.ingest').read_text(encoding='utf-8'))
    print('PASS: static Docker credential/state exclusions and ingest COPY scope (no image built)')
