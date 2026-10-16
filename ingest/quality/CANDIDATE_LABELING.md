# 质量候选集与人工标注流程

`prepare_candidates.py` 只把已验证来源的必要标题/摘录规范化为 `split: candidate`，并固定为 `PENDING_HUMAN_REVIEW`。它**不会**写入 `expectedDecision`、游戏标签或内容类型，也不能生成 frozen 集或开启自动发布。

## 输入

JSONL 每行至少包含：`sourceCode`、`language`（`en` / `zh-CN` / `zh-TW`）、`title`、`text`、`originalUrl`。正文只保留最多 10,000 个规范化字符的必要摘录，避免把整页网页放入 Git。

## 人工标注进入 frozen 的规则

1. 两名审核者或一名审核者加复核，按首期白名单判断 `ACCEPT` / `REVIEW` / `REJECT`。
2. 对 `ACCEPT` 明确填写唯一 `expectedGameCodes` 和设计允许的 `expectedContentType`。
3. 写入独立 frozen JSONL 前，检查来源、语言、URL、摘录是否真实；不得复制调参集、不得用模型自标。
4. 运行 `evaluate.py`。少于 160 条、任一游戏少于 20 条正例、边界样本少于 80 条或未涵盖中英文时，结果必须保持不合格。

候选文件不是评测证据；它只缩短人工收集和去重工作。
