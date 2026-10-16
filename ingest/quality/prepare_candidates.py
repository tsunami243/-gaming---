"""Prepare non-frozen, human-label-required quality candidates from bounded source excerpts."""
import argparse
import hashlib
import json
import re
from pathlib import Path

ALLOWED_LANGUAGES = {"en", "zh-CN", "zh-TW"}
MAX_EXCERPT_CHARS = 10_000


def normalized(value):
    """去除首尾空白，并把内部连续空白折叠为单个空格。

    Args:
        value: 待规范化的文本，可为 None。

    Returns:
        规范化后的字符串。
    """
    return re.sub(r"\s+", " ", value or "").strip()


def prepare(records):
    """从来源摘录生成待人工复核的质量候选记录。

    Args:
        records: 原始来源记录列表。

    Returns:
        去重后的候选记录列表。

    Raises:
        ValueError: 记录缺少来源/语言，或缺少标题、摘录、合法链接时抛出。
    """
    candidates, seen = [], set()
    for index, record in enumerate(records, start=1):
        source = record.get("sourceCode")
        language = record.get("language")
        title = normalized(record.get("title"))
        text = normalized(record.get("text"))[:MAX_EXCERPT_CHARS]
        original_url = record.get("originalUrl")
        if not isinstance(source, str) or not source or language not in ALLOWED_LANGUAGES:
            raise ValueError(f"record {index}: sourceCode and supported language are required")
        if not title or not text or not isinstance(original_url, str) or not original_url.startswith(("https://", "http://")):
            raise ValueError(f"record {index}: title, excerpt and originalUrl are required")
        digest = hashlib.sha256((source + "\n" + original_url + "\n" + title + "\n" + text).encode("utf-8")).hexdigest()
        if digest in seen:
            continue
        seen.add(digest)
        candidates.append({
            "id": "candidate-" + digest[:16],
            "split": "candidate",
            "labelStatus": "PENDING_HUMAN_REVIEW",
            "sourceCode": source,
            "language": language,
            "title": title,
            "text": text,
            "originalUrl": original_url,
        })
    return candidates


def load_jsonl(path):
    """从 JSONL 文件逐行读取记录。

    Args:
        path: 输入文件路径。

    Returns:
        解析后的记录列表。
    """
    return [json.loads(line) for line in Path(path).read_text(encoding="utf-8").splitlines() if line.strip()]


def write_jsonl(path, records):
    """将记录写入 JSONL 文件，目标已存在时拒绝覆盖。

    Args:
        path: 输出文件路径。
        records: 待写入的记录列表。

    Raises:
        FileExistsError: 目标文件已存在时抛出。
    """
    path = Path(path)
    if path.exists():
        raise FileExistsError(f"refusing to overwrite existing candidate file: {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(json.dumps(record, ensure_ascii=False, sort_keys=True) + "\n" for record in records), encoding="utf-8")


def main():
    """命令行入口：把输入记录转换为候选文件并输出候选数量。"""
    parser = argparse.ArgumentParser(description="Create pending-human-review quality candidates; never creates frozen labels")
    parser.add_argument("input")
    parser.add_argument("output")
    args = parser.parse_args()
    candidates = prepare(load_jsonl(args.input))
    write_jsonl(args.output, candidates)
    print(json.dumps({"candidateCount": len(candidates), "labelStatus": "PENDING_HUMAN_REVIEW"}, ensure_ascii=False))


if __name__ == "__main__":
    main()
