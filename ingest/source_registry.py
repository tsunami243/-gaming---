"""Explicit registry for source directions that must fail closed before collection."""
from dataclasses import dataclass
from typing import Optional


@dataclass(frozen=True)
class CandidateSource:
    """描述一个当前禁止采集的候选来源方向。

    Attributes:
        code: 来源唯一编码。
        display_name: 面向用户的展示名称。
        allowed_game_codes: 允许关联的游戏编码元组。
        reason: 该来源暂不可采集的原因说明。
    """
    code: str
    display_name: str
    allowed_game_codes: tuple[str, ...]
    reason: str


XIAOHEIHE_COMMUNITY = CandidateSource(
    code="xiaoheihe-community",
    display_name="小黑盒社区",
    allowed_game_codes=("delta-force", "cs2", "apex-legends", "valorant"),
    reason=(
        "no stable publicly accessible, no-login list/detail structure and content-use permission "
        "have been verified"
    ),
)

_CANDIDATE_SOURCES = {XIAOHEIHE_COMMUNITY.code: XIAOHEIHE_COMMUNITY}


def get_candidate_source(source_code: Optional[str]) -> Optional[CandidateSource]:
    """Return a known candidate source that must not be collected yet."""
    return _CANDIDATE_SOURCES.get(source_code or "")
