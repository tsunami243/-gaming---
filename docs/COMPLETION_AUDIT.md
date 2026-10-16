# 上线完成审计（持续更新）

更新：2026-09-22。此文档是发布前的证据清单，不表示当前项目已获上线批准。

| 目标 | 当前证据 | 状态 | 关闭条件 |
|---|---|---|---|
| 四游戏白名单与公开摘要 API | `fps_rules.py`、v1 API/前端组件测试、统一检查通过 | 部分完成 | 四游戏均有合格来源与影子运行证据 |
| 可靠采集交付 | SQLite outbox、internal receipt、真实隔离 MySQL 事务/回滚验证；2026-09-22 MockMvc HTTP filter/controller → MySQL → published public DTO 验证 | 完成（工程链路） | 保持回归；生产配置后做受控运行 |
| 管理员审核与来源暂停 | v1 管理接口、revision/audit、source policy、MockMvc；审核写入绑定 JWT `userId`，无 claims 不写状态/审计；文章级分页审核历史 API 与管理页只读展示；2026-09-22 独立 MySQL 证明 approve→audit 和审计失败回滚 | 部分完成 | 真实 JWT+Redis 与管理端真实认证浏览器链路、来源/运行状态完善、管理员初始化方案确认；HTTP ingest 真实 MySQL 测试受当前 Windows Tomcat loopback 启动故障阻塞 |
| 首期来源 | VALORANT 与 Apex 官方源 `SHADOW`；CS2 暂停；Delta 未验证 | 未完成 | 6–8 个合格来源、四游戏覆盖、使用条件与备用策略 |
| 自动发布 | 质量 evaluator 强制 160/95%/90%/零负例 gate | 未完成 | 真实冻结标注集、影子运行、B04/B05 确认后逐来源启用 |
| 独立 Compose | `deploy/` 与无启动 compose config 验证 | 部分完成 | 镜像 digest、空环境构建/启动、独立配额/备份、B06 批准 |
| 4 GB / 10 GB 资源目标 | Compose memory caps、CapacityGuard 模拟测试 | 未完成 | 项目卷配额、cgroup 实测、日志/镜像/备份口径、负载与 24h 观察 |
| 备份恢复与上线 | 无真实备份目标，未部署 | 未开始 | B01/B02/B03/B06、恢复演练、发布/回滚审批 |

## 不能用代码代替的外部前置条件

1. **B04**：真实 AI 模型、预算、密钥与调用批准；当前不得将任何来源从 `SHADOW` 升为自动发布。
2. **B05**：其余两款游戏和补充来源的可访问性、内容质量及使用条件；不能以综合首页凑覆盖。
3. **B01/B02/B03/B06**：域名/端口、异地备份、告警与管理员初始化、Docker/网络/磁盘配额/部署窗口批准。

在这些条件没有满足前，任何“已上线”“覆盖四游戏”“自动发布已启用”“资源已达标”的表述都是不成立的。

