"""Check configured per-service budgets; does not assert runtime cgroup enforcement."""
from pathlib import Path
import yaml
import sys

LIMITS = {'backend':1200000000, 'mysql':1200000000, 'ingest':450000000, 'redis':200000000, 'frontend':100000000}

def validate(spec):
    services = spec['services']
    if set(services) != set(LIMITS): raise ValueError('service inventory differs from approved budget')
    total = 0
    for name, limit in LIMITS.items():
        service = services[name]
        value = service.get('mem_limit')
        if type(value) is not int or not 0 < value <= limit:
            raise ValueError(f'{name}: integer byte cap exceeds design or missing')
        if service.get('memswap_limit') != value: raise ValueError(f'{name}: swap budget mismatch')
        if 'logging' not in service: raise ValueError(f'{name}: log retention required')
        total += value
    if total > 3150000000: raise ValueError('container budget exceeds 3.15 GB')
    if sum(float(service.get('cpus', 100)) for service in services.values()) > 3:
        raise ValueError('CPU budget exceeds 3 cores or missing')
    return total

if __name__ == '__main__':
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[1]/'deploy/compose.yaml'
    spec = yaml.safe_load(path.read_text(encoding='utf-8'))
    print(f'PASS: configured container caps total {validate(spec)} bytes; actual cgroup and peak use NOT verified')
