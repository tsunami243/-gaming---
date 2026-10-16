"""Generate the read-only project capacity snapshot required by ingest admission.

`ingest/runtime/capacity.py` refuses to admit new collection unless it can read a
*fresh*, project-scoped snapshot of storage. A whole-filesystem `used` value is
explicitly rejected by design: it would charge other projects' data to gameboi.

Contract (docs/DEVELOPMENT_PLAN.md:241):

    scope               == 'gameboi-project'
    observedAt          UTC epoch seconds  (older than 120s  ->  UNKNOWN, collection stops)
    components          images / volumes / logs / temporary / backups, non-negative int bytes
    hostAvailableBytes  host free bytes, non-negative int

Project usage is the sum of the five components. Run this immediately before
`ingest/main.py` — a stale snapshot stops collection on purpose, and that is the
gate working as intended rather than a bug.

Usage:
    python scripts/capacity-snapshot.py                    # -> target/capacity-snapshot.json
    python scripts/capacity-snapshot.py --out <path>
"""
from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# Build/test scratch that the project can regenerate at any time.
TEMP_PARTS = ("target", "ingest/runtime/__pycache__", "gaming-news/dist")
TEMP_DIR_NAMES = ("__pycache__", ".vite", ".cache")
SKIP_DIR_NAMES = ("node_modules", ".git")

# Backups live beside the project (db-backup/) as well as inside it.
BACKUP_DIR_NAMES = ("backups", "db-backup")


def _dir_bytes(path: Path) -> int:
    """Recursive byte size; unreadable entries are skipped, never fatal."""
    if not path.exists():
        return 0
    total = 0
    for base, dirs, files in os.walk(path):
        dirs[:] = [d for d in dirs if d not in SKIP_DIR_NAMES]
        for name in files:
            try:
                total += (Path(base) / name).stat().st_size
            except OSError:
                pass
    return total


def _unique_dirs(*paths: Path) -> list[Path]:
    """De-duplicate nested paths so a parent is never counted twice with its child."""
    kept: list[Path] = []
    for path in sorted({p for p in paths}, key=lambda p: len(p.parts)):
        if path.exists() and not any(path == parent or parent in path.parents for parent in kept):
            kept.append(path)
    return kept


def _walk_named(names: tuple[str, ...]) -> list[Path]:
    found: list[Path] = []
    for base, dirs, _files in os.walk(ROOT):
        dirs[:] = [d for d in dirs if d != ".git"]
        for name in list(dirs):
            if name in names:
                found.append(Path(base) / name)
    return found


def _docker_bytes(kind: str) -> int:
    """Real byte size of `docker image|volume ls`, via inspect. 0 when Docker is absent."""
    if shutil.which("docker") is None:
        return 0
    target = "image" if kind == "images" else "volume"
    try:
        listed = subprocess.run(
            ["docker", target, "ls", "--quiet"], capture_output=True, text=True, timeout=20
        )
        ids = [line.strip() for line in listed.stdout.splitlines() if line.strip()]
        if listed.returncode != 0 or not ids:
            return 0
        described = subprocess.run(
            ["docker", target, "inspect", "--format", "{{.Size}}", *ids],
            capture_output=True, text=True, timeout=30,
        )
        return sum(int(line) for line in described.stdout.split() if line.isdigit())
    except (OSError, subprocess.SubprocessError, ValueError):
        return 0


def collect() -> dict:
    logs = sum(
        path.stat().st_size
        for path in ROOT.rglob("*.log")
        if path.is_file() and not any(part in SKIP_DIR_NAMES for part in path.parts)
    )

    temp_roots = [ROOT / part for part in TEMP_PARTS]
    temp_roots += [p for p in _walk_named(TEMP_DIR_NAMES) if "node_modules" not in p.parts]
    temporary = sum(_dir_bytes(p) for p in _unique_dirs(*temp_roots))

    backup_roots = _walk_named(BACKUP_DIR_NAMES) + [ROOT.parent / "db-backup"]
    backups = sum(_dir_bytes(p) for p in _unique_dirs(*backup_roots))

    return {
        "scope": "gameboi-project",
        "observedAt": time.time(),
        "components": {
            "images": _docker_bytes("images"),
            "volumes": _docker_bytes("volumes"),
            "logs": logs,
            "temporary": temporary,
            "backups": backups,
        },
        "hostAvailableBytes": shutil.disk_usage(ROOT).free,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Write the project capacity snapshot.")
    parser.add_argument("--out", default=str(ROOT / "target" / "capacity-snapshot.json"))
    args = parser.parse_args()

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    snapshot = collect()

    # Atomic: the admission gate must never read a half-written snapshot.
    scratch = out.with_suffix(out.suffix + ".tmp")
    scratch.write_text(json.dumps(snapshot), encoding="utf-8")
    scratch.replace(out)

    used = sum(snapshot["components"].values())
    print(f"snapshot -> {out}")
    print(f"  project used : {used / 1e9:.3f} GB  {snapshot['components']}")
    print(f"  host free    : {snapshot['hostAvailableBytes'] / 1e9:.2f} GB")
    print(f"  bytes        : {out.stat().st_size} (limit 16384)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
