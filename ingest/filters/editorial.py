"""Combine editorial evidence; model confidence is not a calibrated acceptance gate."""
from filters.fps_rules import GAME_ALIASES, TYPE_KEYWORDS, PreFilterDecision


def editorial_decision(prefilter, processed):
    """综合规则预过滤与模型处理结果，给出最终编辑决策。

    Args:
        prefilter: 确定性预过滤判定结果。
        processed: 处理后的文章结果。

    Returns:
        合并后的最终判定结果。
    """
    if prefilter.decision == 'REJECT' or processed.decision == 'REJECT' or not processed.is_gaming:
        return PreFilterDecision('REJECT', prefilter.game_codes, prefilter.content_type, 'EDITORIAL_REJECT')
    games = tuple(processed.game_codes)
    model_type = processed.content_type
    valid_model = bool(games) and set(games) <= set(GAME_ALIASES) and model_type in TYPE_KEYWORDS
    agree = valid_model and set(games) == set(prefilter.game_codes) and model_type == prefilter.content_type
    if prefilter.decision == 'ACCEPT' and processed.decision == 'ACCEPT' and agree:
        return PreFilterDecision('ACCEPT', prefilter.game_codes, prefilter.content_type, None)
    # Never replace deterministic game evidence with a model's unsupported game claim.
    content_type = prefilter.content_type or (model_type if valid_model else None)
    return PreFilterDecision('REVIEW', prefilter.game_codes, content_type, 'EDITORIAL_REVIEW')
