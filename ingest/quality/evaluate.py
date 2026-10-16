"""Deterministic rule evaluation. Passing this report alone never enables publication."""
import argparse
import hashlib
import json
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from filters.fps_rules import classify, GAME_ALIASES, TYPE_KEYWORDS

MIN_FROZEN_SAMPLES = 160
MIN_AUTO_PRECISION = 0.95
MIN_POSITIVE_COVERAGE = 0.90


def evaluate(records):
    """对冻结样本集运行确定性规则评估并生成评估报告。

    Args:
        records: 冻结样本记录列表。

    Returns:
        含精度、覆盖率与样本问题等指标的评估报告字典。

    Raises:
        ValueError: 样本包含待人工复核候选，或 expectedDecision 非法时抛出。
    """
    evaluated, issues, ids, fingerprints = [], [], set(), set()
    positives_by_game = dict.fromkeys(GAME_ALIASES, 0)
    languages, sources = set(), set()
    for record in records:
        if record.get("split") == "candidate" or record.get("labelStatus") == "PENDING_HUMAN_REVIEW":
            raise ValueError("pending human-review candidates cannot be evaluated as frozen samples")
        expected = record.get("expectedDecision")
        if expected not in ('ACCEPT', 'REVIEW', 'REJECT'):
            raise ValueError('invalid expectedDecision')
        identifier = record.get('id')
        if not isinstance(identifier, str) or not identifier or identifier in ids:
            issues.append('missing or duplicate sample ID')
        ids.add(identifier)
        text = record.get('text', '')
        title = record['title']
        fingerprint = hashlib.sha256((' '.join(title.split()).lower() + '\n' + ' '.join(text.split()).lower()).encode()).hexdigest()
        if fingerprint in fingerprints: issues.append('duplicate sample content')
        fingerprints.add(fingerprint)
        if record.get('split') != 'frozen': issues.append('non-frozen or undeclared split')
        source = record.get('sourceCode')
        if not source: issues.append('missing source provenance')
        else: sources.add(source)
        language = record.get('language')
        if language not in ('en', 'zh-CN', 'zh-TW'): issues.append('missing or unsupported sample language')
        else: languages.add(language)
        games = record.get('expectedGameCodes')
        content_type = record.get('expectedContentType')
        if not isinstance(games, list) or any(game not in GAME_ALIASES for game in games):
            issues.append('missing or invalid expected game labels')
            games = []
        if expected == 'ACCEPT':
            if len(games) != 1 or content_type not in TYPE_KEYWORDS:
                issues.append('positive needs one game and a supported content type')
            elif record.get('split') == 'frozen': positives_by_game[games[0]] += 1
        actual = classify(title, text)
        evaluated.append((record, actual, expected, games, content_type))
    auto = [row for row in evaluated if row[1].decision == 'ACCEPT']
    correct_auto = [row for row in auto if row[2] == 'ACCEPT' and set(row[1].game_codes) == set(row[3]) and row[1].content_type == row[4]]
    positives = [row for row in evaluated if row[2] == 'ACCEPT']
    covered = [row for row in positives if row[1].decision in ('ACCEPT', 'REVIEW')]
    leaked = [row for row in auto if row[2] == 'REJECT']
    boundary_count = sum(row[2] != 'ACCEPT' for row in evaluated)
    if len(evaluated) < MIN_FROZEN_SAMPLES: issues.append('fewer than 160 samples')
    if any(count < 20 for count in positives_by_game.values()): issues.append('fewer than 20 positive samples for a game')
    if boundary_count < 80: issues.append('fewer than 80 negative or review-boundary samples')
    if 'en' not in languages or not languages.intersection({'zh-CN', 'zh-TW'}): issues.append('both Chinese and English required')
    precision = len(correct_auto) / len(auto) if auto else 0.0
    coverage = len(covered) / len(positives) if positives else 0.0
    reviewed = sum(row[1].decision == 'REVIEW' for row in evaluated)
    return dict(sampleCount=len(evaluated), uniqueContentCount=len(fingerprints), autoAccepted=len(auto),
                correctAutoAccepted=len(correct_auto), autoPrecision=precision, positiveCount=len(positives),
                coveredPositiveCount=len(covered), positiveCoverage=coverage, leakedNegativeAutoAccepts=len(leaked),
                reviewCount=reviewed, reviewRate=reviewed/len(evaluated) if evaluated else 0.0,
                positiveCountsByGame=positives_by_game, negativeOrReviewCount=boundary_count,
                sources=sorted(sources), datasetIssues=sorted(set(issues)),
                eligibleForAutoPublish=not issues and precision >= MIN_AUTO_PRECISION and coverage >= MIN_POSITIVE_COVERAGE and not leaked,
                perDecision={key: sum(row[1].decision == key for row in evaluated) for key in ('ACCEPT','REVIEW','REJECT')})


def load(path):
    """从 JSONL 文件逐行读取样本记录。

    Args:
        path: 样本文件路径。

    Returns:
        解析后的样本记录列表。
    """
    return [json.loads(line) for line in Path(path).read_text(encoding='utf-8').splitlines() if line.strip()]


def main():
    """命令行入口：读取数据集、输出评估报告，并按是否可自动发布设置退出码。"""
    parser = argparse.ArgumentParser(); parser.add_argument('dataset'); args = parser.parse_args()
    report = evaluate(load(args.dataset)); print(json.dumps(report, ensure_ascii=False, indent=2))
    raise SystemExit(0 if report['eligibleForAutoPublish'] else 2)

if __name__ == '__main__': main()

