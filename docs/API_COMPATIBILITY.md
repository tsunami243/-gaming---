# API v1 兼容期与移除条件

更新：2026-09-22。契约来源：[openapi.yaml](api/openapi.yaml)。

## 当前策略

`/api/v1/games`、`/api/v1/articles`、`/api/v1/articles/{id}` 已作为只读 DTO 接口实现。旧 Vue 仍调用无版本前缀的 legacy 接口，直到 WEB-01 迁移完成。

OpenAPI 中 `/api/v1/admin/articles` 已在 ADMIN-01 部分落地：内容管理员可分页查看审核状态，并以 revision 乐观锁执行通过、拒绝和下架。`GET /api/v1/admin/articles/{id}/audit` 现提供单篇文章的分页审核历史，仅返回审核动作、原因、状态、修订、时间及操作者用户 ID，不返回密码、token 或请求头；读取与写入均要求内容管理员或超级管理员。管理员原创文章创建仍待后续子任务。`/internal/v1/ingest/events` 与 source policy 查询已在 ING-02 落地：只接受服务令牌，重复 eventId 同 payload 返回 200 receipt，首次提交返回 201；采集器在配置 internal 地址后会查询来源暂停策略，浏览器客户端和用户 JWT 均不应调用它。

## 旧接口兼容表

| Legacy 路径 | 当前状态 | v1 对应 | 移除条件 |
|---|---|---|---|
| GET `/article` | 保留，已强制只读已发布文章 | GET `/api/v1/articles` | Vue 首页、分页与筛选迁移并有端到端测试 |
| GET `/article/detail?id=` | 保留，已对未发布返回 404 | GET `/api/v1/articles/{id}` | 详情页迁移并验证摘要/原文跳转 |
| GET `/category` | 保留，旧数据库分类 | GET `/api/v1/games` | 前端统一使用 game code，不再使用 category ID |
| POST/PUT/DELETE `/article` | 仅管理员，保留给旧管理兼容 | planned `/api/v1/admin/articles` | ADMIN-01 管理页与审核流上线、迁移期回归通过 |
| POST `/admin/ingest/consume` | 默认禁用；仅在一次性积压迁移窗口显式开启 `ingest.legacy-queue.enabled=true` | POST `/internal/v1/ingest/events` | 使用迁移 runbook 将 Redis List 逐条交付并取得 receipt，队列为零且保留审计证据后删除旧入口 |
| `/push/**` | denyAll，生产不可用 | 无 | 单独归档/移除任务批准后处理 |

## 不兼容点

- v1 成功和错误体均包含 `traceId`；legacy `Result` 没有。
- v1 文章列表为 DTO，不含 `content` HTML；legacy 列表直接序列化 Article。
- v1 tags 是数组；legacy tags 是 JSON 字符串。
- v1 时间字段使用 ISO-8601 的带时区契约；legacy 仍有本地时间格式。
- v1 `gameCode` 是稳定字符串；legacy 使用 category ID。当前只读适配器暂时映射四个已知 ID，ING-02/WEB-01 后由 article_game 关系取代。

## 代码生成决策

本阶段**不启用 OpenAPI Generator**：Vue 仍在 legacy 路径，v1 管理/内部端点尚未落地，立即生成客户端会制造第二套半成品 API 层。待 WEB-01 明确迁移范围后，再锁定 generator 版本并只生成 v1 客户端/类型。


## 旧 Redis List 迁移约束（ING-02）

旧 `ingest:article_queue` 不会被清空或自动消费。生产默认 `ingest.legacy-queue.enabled=false`，旧消费者与 `/admin/ingest/consume` 不注册。若历史积压存在，须先在隔离副本导出数量和样本，再在维护窗口按 FIFO 小批量转换为 v1 event、经 `/internal/v1/ingest/events` 获得 receipt 后才确认一条；失败条目保留原队列并记录原因。完成时需记录初末长度、receipt 数、冲突数和人工复核清单，确认 Redis List 为零后才允许删除旧代码。
