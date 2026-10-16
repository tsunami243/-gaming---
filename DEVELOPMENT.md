> **本文件已作废（2026-10-13）。** 原"工具层操作手册"内容已失效且会误导：它描述的 `PublishView.vue` / `MessageView.vue` / `SwiperBanner.vue` / `CategoryTabs.vue` / `stores/article.js` / `global.css` / `apple.css` 均已不存在，`/article` 的 `state: 草稿|已发布` + `categoryId` 旧数据模型已被 `/api/v1` 契约取代，"后端无单元测试"也不再成立。

# DEVELOPMENT.md — 已废弃

请改用以下权威来源：

| 用途 | 文件 |
|---|---|
| 执行约束与每轮流程 | `AGENTS.md` |
| 产品/架构/资源基线 | `docs/PROJECT_DESIGN.md` |
| 任务顺序与验收定义 | `docs/DEVELOPMENT_PLAN.md` |
| 任务状态唯一记录 | `feature_list.json` |
| 待解决目标清单（本阶段工作入口） | `goal.md` |
| 各目标的解决方案 | `goals/<GOAL-ID>.md` |
| 当前接口与前端调用矩阵 | `docs/API_AUDIT.md` |
| 统一检查入口 | `scripts/check.ps1` |

原文件中的环境排障（Redis/Python/MySQL/端口）与命令示例，如后续仍需要，应按当前 v1 架构重写后再补回，不要直接引用历史内容。
