"""Deterministic FPS allow-list and editorial type pre-filter."""
from dataclasses import dataclass

GAME_ALIASES = {
    "delta-force": ("三角洲行动", "delta force"),
    "valorant": ("无畏契约", "valorant"),
    "cs2": ("cs2", "counter-strike 2", "反恐精英2"),
    "apex-legends": ("apex legends",),
}
TYPE_KEYWORDS = {
    "announcement": ("公告", "announcement"),
    "patch_notes": ("更新", "补丁", "版本", "patch", "update", "update notes"),
    # Official Chinese announcements say 庆典/周年/嘉年华 rather than the literal 活动.
    "event": ("活动", "event", "庆典", "周年", "嘉年华", "联动"),
    "new_content": ("新地图", "新角色", "新武器", "new map", "new legend"),
    "guide": ("攻略", "技巧", "guide", "tips"),
    # Domestic esports copy uses 联赛/总决赛/冠军 family wording, not just 赛事/战队.
    "esports": ("赛事", "战队", "电竞", "tournament", "esports", "champions", "vct", "game changers", "algs", "pro league",
                "联赛", "总决赛", "半决赛", "预选赛", "锦标赛", "晋级", "冠军", "夺冠", "赛程"),
}
# Official announcements routinely mention streamers; only the trading/boosting terms are
# hard rejections. "主播" was removed because it rejected legitimate official event posts.
DISALLOWED = ("代练", "账号交易", "卖号", "显卡评测", "硬件评测")

@dataclass(frozen=True)
class PreFilterDecision:
    """确定性预过滤的单条判定结果。

    Attributes:
        decision: 判定结论（ACCEPT / REVIEW / REJECT）。
        game_codes: 命中的游戏编码元组。
        content_type: 命中的内容类型，未命中时为 None。
        reason_code: 判定原因编码，通过时为 None。
    """
    decision: str
    game_codes: tuple[str, ...]
    content_type: str | None
    reason_code: str | None


def classify(title: str, text: str = "") -> PreFilterDecision:
    """基于游戏别名与类型关键词对文章做确定性预过滤。

    Args:
        title: 文章标题。
        text: 文章正文，默认空字符串。

    Returns:
        预过滤判定结果。
    """
    normalized = f"{title} {text}".lower()
    games = tuple(code for code, aliases in GAME_ALIASES.items() if any(alias.lower() in normalized for alias in aliases))
    if not games:
        return PreFilterDecision("REJECT", (), None, "GAME_NOT_ALLOWED")
    if any(term in normalized for term in DISALLOWED):
        return PreFilterDecision("REJECT", games, None, "DISALLOWED_TYPE")
    title_normalized = title.lower()
    title_types = [kind for kind, words in TYPE_KEYWORDS.items() if any(word.lower() in title_normalized for word in words)]
    # A single explicit headline type is stronger evidence than incidental words in the article body.
    types = title_types if len(title_types) == 1 else [kind for kind, words in TYPE_KEYWORDS.items() if any(word.lower() in normalized for word in words)]
    if "patch_notes" in types:
        types = ["patch_notes"]
    if "announcement" in types and len(types) > 1:
        types.remove("announcement")
    if len(games) != 1 or len(types) != 1:
        return PreFilterDecision("REVIEW", games, types[0] if len(types) == 1 else None, "LOW_EVIDENCE")
    return PreFilterDecision("ACCEPT", games, types[0], None)


