import sys
from pathlib import Path
import yaml

root = Path(__file__).resolve().parents[1]
spec = yaml.safe_load((root / "docs/api/openapi.yaml").read_text(encoding="utf-8"))
assert spec["openapi"].startswith("3.1")
assert spec["info"]["version"] == "1.0.0-draft"
for path in ("/games", "/articles", "/articles/{id}", "/admin/articles", "/admin/articles/{id}/audit", "/internal/v1/ingest/events", "/internal/v1/ingest/sources/{code}/policy"):
    assert path in spec["paths"], path
assert spec["paths"]["/admin/articles"]["x-implementation-status"] == "partial"
assert spec["paths"]["/admin/articles/{id}/approve"]["x-implementation-status"] == "implemented"
assert spec["paths"]["/admin/articles/{id}/audit"]["x-implementation-status"] == "implemented"
assert spec["paths"]["/internal/v1/ingest/events"]["x-implementation-status"] == "implemented"
schemas = spec["components"]["schemas"]
assert schemas["PageArticleSummary"]["required"] == ["items", "total", "pageNum", "pageSize"]
assert schemas["ArticleSummary"]["properties"]["tags"]["type"] == "array"
print("PASS: OpenAPI v1 contract paths, implementation states and core schemas")


