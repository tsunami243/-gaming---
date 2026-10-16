# 持续开发计划与验收清单

- 基线：[PROJECT_DESIGN.md](PROJECT_DESIGN.md) v1.0（2026-09-20）
- 本文件负责执行顺序与验收定义；任务状态只记录在 [../feature_list.json](../feature_list.json)，不要维护两份互相矛盾的勾选清单。
- 当前接口与前端调用审计：[API_AUDIT.md](API_AUDIT.md)。
- 本轮产出仅是设计文档。下述代码、接口、迁移、部署文件除非任务状态另有证据，均为待实现。

## 1. 每轮开发流程

1. 读根目录 AGENTS.md、设计相关章节、本计划、feature_list.json 和 claude-progress.md 最新记录。
2. PowerShell 执行 git status/diff，识别并保留用户原有修改。本次设计起点已有 HeatService.java 的空白改动，不顺带回滚。
3. 选择依赖已完成、优先级最高的一项任务，标记 in_progress；除非用户要求，不并行展开多项业务改造。
4. 明确本轮写入范围与验收，不改无关目录；先补失败用例，再实现最小闭环。
5. 执行该任务的检查。涉及数据库测试使用独立测试实例，禁止连接线上数据库运行测试/迁移。
6. 记录命令、退出结果、测试数、必要摘要、未验证项。不得记录服务器密码、密钥、token、完整个人数据。
7. 只有验收全部通过才设 done，并附证据；否则保持 in_progress 或 blocked，明确下一步。
8. 更新进度日志和必要设计版本。禁止未经请求自动提交、推送、部署或删除旧数据。

状态：pending / in_progress / blocked / done。blocked 必须有原因和解除条件；不能用 done 表达“代码写完但没测”。

## 2. 阶段与依赖

```text
M0 设计基线（本轮）
  → M1 可重现基线 + P0 安全修复
  → M2 数据迁移 + 接口契约
  → M3 单游戏可靠采集纵向链路
  → M4 前端与管理闭环
  → M5 四游戏来源和质量门禁
  → M6 独立部署、容量约束、备份与资源验收
  → M7 上线审批与渐进发布
```

不设置未经验证的工期。阶段交付不要求大重构；安全修复不等待整体架构完成。来源可读性调研可提前做，但正式采集与自动发布必须受预算和阶段门禁约束。

## 3. 任务定义

### DOC-00：设计基线与交接入口（M0）

范围：本目录设计/计划/来源表、根目录文档入口、任务清单和本轮进度。

验收：业务约束齐全；首期/后续、已观察/待验证有区分；相对文档链接有效；JSON 能解析、ID 唯一、依赖完整无环；未变更业务代码或服务器；不包含交接凭据。

### BASE-01：项目基线与完整接口矩阵（M1，P0）

依赖 DOC-00。范围：构建/测试配置、必要开发说明、接口盘点；不新增业务功能。

交付：
- `docs/API_AUDIT.md`：全部当前前端请求、后端方法/路径、权限、参数、响应、实际页面调用情况、保留/修复/隐藏决策；既覆盖活跃路径也标记无引用代码。
- 可重复的 Java、Vue、Python 测试入口；确认运行时与依赖，不把旧 README 版本下界当成已支持版本。
- `scripts/check.ps1` 等 Windows 检查入口（如需要）；不要求用户为本地开发启用 Bash/WSL。
- 当前构建失败/环境不足的精确记录；能独立跑的离线测试先跑。

验收：Java 编译/现有测试、Vue build/测试入口、Python 导入/基础测试分别记录成功或阻塞；知道后续回归起点。若基础失败，应在明确范围内修复再标 done，不伪造成功。

### SEC-01：权限与公开内容边界（M1，P0）

依赖 BASE-01。范围：SecurityConfig、文章读取/写入、IngestController、push demo、对应路由权限。

验收：
- 匿名写入 401；普通用户的文章写入/审核/采集管理 403；管理员可操作系统采集文章，不限于 createUser。
- 所有公开文章读/搜索/热榜/轮播和互动目标只允许已发布内容；未发布详情 404。
- `/push/**` 和模拟批量请求在生产不可用；旧手动消费接口禁用或在统一管理员策略内受保护。
- 前端普通用户无发布入口，直接访问也无权限；后端直连测试不能绕过。
- 不改变既有登录与基本互动正确性。至少建立角色×方法×对象状态的集成测试矩阵。

### DATA-01：版本化数据库与新文章模型（M2，P0）

依赖 SEC-01。范围：实体/DTO基础、SQL迁移、初始化与旧数据映射策略。

交付：选择并锁定迁移工具；完整空库基线；game/source/article及关系、receipt、run、audit 的增量迁移；测试数据；旧库迁移预演说明。

验收：空库可启动；第二次运行不重复写入种子/DDL；数据库副本升级可保留原数据和关联；无法识别分类进入清单；来源/原文时间/summary/readingMode 可完整存取；密码不入 Git。真实旧库未获迁移许可时只使用脱敏结构/测试副本，不自行连接改表。

### API-01：统一契约与兼容期（M2，P1）

依赖 DATA-01。范围：`docs/api/openapi.yaml`、DTO、错误码、公开/管理/内部接口边界、契约测试。

验收：分页固定 items/total/pageNum/pageSize；games 使用稳定代码；列表不传全文；详情区分摘要/全文；错误 HTTP 状态正确；不存在和不可见一致；tags 数组；时间带时区。前后端契约样例可测试，旧接口兼容层有明确移除条件。是否启用客户端生成在本任务记录，不无谓迁移技术栈。

### ING-01：来源适配与有界处理阶段（M3，P1）

依赖 API-01。范围：Python 适配器、模型、抽取/规则/摘要、运行互斥和预算配置。

验收：游戏白名单与六类型分离；歧义进入 REVIEW；禁入内容拒绝；正文大小/超时/输入长度受限；重试不乘法放大；同一时刻单任务；每日预算持久化；dry-run 无生产写入/付费调用；AI 不可用不发布原始兜底正文。

使用固定 HTML/JSON 样本离线测试；模型可用性和价格待 B04 确认，不自动调用收费服务。容量状态读取不可用时 fail-closed。

### ING-02：持久 outbox 与后端幂等交付（M3，P0）

依赖 ING-01。范围：Python SQLite outbox、内部服务身份、统一交付服务、MySQL receipt事务、旧消费者迁移开关。

验收：
- 处理结果落盘后才发送；收到提交成功 receipt 才 ACK；重启恢复同 eventId。
- 同事件重复 10 次只有一次业务效果；不同 payload 同 eventId 409。
- 服务端提交后响应丢失、worker 强制退出、数据库不可用、401、429、坏 JSON 等均可解释和恢复。
- 未 ACK 队列不能按 TTL 清理；outbox 软上限触发暂停；拒绝/待审核不伪装发布成功。
- Python 不持有业务数据库凭据；公网/用户 token 不能调用 internal API。
- 旧 List 入口退役开关与积压迁移方案可测试；不直接清空真实队列。

### ING-03：单游戏纵向样板（M3，P1）

依赖 ING-02。工程默认 CS2；若来源实测不通过，记录原因后换另一白名单游戏，不等待某一个站永久阻塞。

验收：一个真实验证来源 + 离线样本，串行采集→分类→待审核/发布（测试环境）→查询 DTO 全链路；URL/标题/原始时间/摘要无丢失；再次运行不重复。真实来源样本/使用条件登记，不能用固定 mock 替代来源验证。

### WEB-01：公开页面与接口对齐（M4，P1）

依赖 ING-03。范围：首页/详情、分类 Store、客户端、分页、普通用户发布入口、演示消息导航。

验收：四游戏/类型筛选、分页加载、空/错误/加载状态、摘要详情/原文跳转、登录失效/403 反馈；首次请求不重复；禁用页面无假数据；移动端可用；关键端到端测试。引用检查后才移除旧工具/Store。前端生产 build 成功。

### ADMIN-01：审核与轻量运营后台（M4，P1）

依赖 WEB-01。范围：管理员文章、来源、运行、暂停状态、审核动作和审计。

验收：待审核可看游戏/类型证据、原因、原文；通过/拒绝/下架持久且有审计；修订冲突不给静默覆盖；暂停/重试仍受容量/预算/互斥限制；无凭据展示；重载页面状态一致。管理员账号初始化方案不硬编码密码，B03 的告警渠道可到 M6 前最终确认。

### SOURCE-01：四游戏来源注册与接入（M5，P1）

依赖 ADMIN-01。范围：SOURCE_EVALUATION.md 的证据、来源配置与对应适配器。

验收：首期目标 6–8 个质量合格来源，四游戏均有明确覆盖；来源类型、语言、频率、最后验证、备用/暂停策略齐全；中文/英文官方样本可正确抽取；所有未验证或无来源的游戏不得标“已覆盖”。来源不足时报告范围风险，不启用综合站首页凑数。

### XHH-01：小黑盒社区补充来源受控接入（M5，P1，2026-09-22）

依赖 SOURCE-01 与 ADMIN-01。该任务仅在 `xiaoheihe-community` 仍为 `CANDIDATE` 的前提下实施验证与受控接入；它不是官方来源替代品，也不改变四游戏白名单。

已确认的产品与合规边界：

- 范围只限三角洲、CS2、Apex Legends、VALORANT；只使用公开、免登录、无需绕过限制的网页或接口。不登录、不保存 Cookie、不逆向 App 私有接口、不伪造签名、不绕过验证码或反爬；无法验证稳定公开入口即暂停。
- 只采集必要元数据及生成后的短摘要：标题、原始发布时间、四游戏/类型判定、作者（页面公开时）、`sourceName=小黑盒社区`、来源性质（community）、canonical 原文链接和审核证据。不得自动复制全文、图片或评论；用于摘要的受限原文摘录不作为公开正文展示或长期业务正文保存。
- 全部候选固定为 `PENDING_HUMAN_REVIEW`，只能由管理员审核后发布；前台固定显示“文章来源：小黑盒社区”和原文跳转，审核页必须可看到来源、原文和规则/去重原因。
- 官方源优先：若同一事实已有 Valve/EA/Riot/Garena 等第一方来源，社区转载候选必须被去重拒绝；只有有独立社区价值且不构成官方重复的内容才可进入人工审核。
- 短摘要由 AI 生成，但实际 AI 调用默认关闭且没有密钥不运行。B04 预算、供应商、密钥和内容使用许可未确认前，候选不得因缺摘要发布。启用后采用严格限额：单候选最多 1 次生成、无自动重试；默认每日最多 20 篇、输入摘录最多 1,200 字符、输出最多 220 个中文字符；超限、无密钥、超时或模型失败均停止摘要，仅保留待审核状态并记录原因，绝不降级展示原始正文。

实施顺序：

1. 以单请求、无 Cookie 的方式确认一个稳定公开列表入口和一个公开详情入口，并记录允许域名、重定向、限流、缺字段、重复/更新及内容展示许可证据；在此前不新增生产适配器或数据库 source seed。
2. 为经验证的公开结构制作脱敏 HTML/JSON fixture 和离线解析/规则/去重测试；每次真实验证最多 1 个候选、不开启 AI、Redis、SQLite outbox 或内部投递写入。
3. 完成 `sourceName`、来源性质、原文链接、`PENDING_HUMAN_REVIEW`、官方优先去重和 AI 摘要预算的契约/单元测试；AI 凭据只从运行环境注入，不进 Git、日志、fixture 或测试输出。
4. 只有公开可访问性、内容展示许可、四游戏相关率、重复策略、AI B04 和管理员审核链路均有证据时，才将来源提升为 `FETCH_VERIFIED`；此后仍不得自动发布，须另行影子运行和质量门禁。

验收：无 Cookie 的真实受控请求和离线 fixture 都能证明仅接收四游戏相关候选；任何登录/验证码/签名/访问规则不明、摘要失败、预算超限、官方重复或来源字段缺失均 fail-closed 并留痕；公开 DTO 不返回受限原文摘录/全文；管理员通过前，匿名公开接口不可见该文章。
### QUALITY-01：质量门禁与影子运行（M5，P1）

依赖 SOURCE-01。范围：冻结评测集、规则/模型版本、自动发布来源开关。

验收：至少160条规定组成样本、调参与冻结集分开；报告 precision/覆盖率/各游戏混淆及审核率；满足设计阈值后才启用对应来源自动通过；真实影子运行记录。AI 调用预算已获确认。未达标进入 REVIEW，不降低门槛或删除难样本。

### DEPLOY-01：隔离构建与 Compose（M6，P0）

依赖 QUALITY-01。范围：`deploy/`、独立运行时镜像/锁文件、Compose、本地部署说明、示例配置、健康检查。

验收：空环境可启动；版本/digest锁定；只有web业务入口；独立账号/卷/网络；无源码或密钥泄露；容器限制在 inspect/cgroup 生效；生产禁用demo；重启不丢MySQL/outbox；现有服务无需升级或复用。

先本地/独立测试机验证。B01/B06 未确认前不能在目标服务器安装/部署；Docker 网络影响/原有服务回归方案必须写明。

### OPS-01：容量保护、备份与资源实测（M6，P0）

依赖 DEPLOY-01。范围：宿主最小容量采样、后台容量状态、日志/保留配置、异地备份/恢复说明和测试记录。

验收：
- 项目完整口径≤4 GB内存；容器、备份、采集等峰值实测，无OOM。
- 镜像/卷/日志/临时/备份全口径统计；7/8 GB状态模拟可验证；UNKNOWN停采；接近9 GB禁止非必要写入；恢复需复核。
- ≤10 GB 的限额/准入策略实际验证，不能只靠周期性采样声称硬保证；禁止用写满服务器做测试。
- 20并发/5读RPS/30分钟受控负载 + 24小时低频观察；目标主机压测需批准。
- 异地备份恢复到独立测试库成功，记录RPO/RTO；未确认B02/B03不得标done。
- 所有自动清理只触及本项目已解决/过期非业务材料；未 ACK/未审核/已发布/用户数据均保留。

### RELEASE-01：上线批准与小范围发布（M7，P0）

依赖 OPS-01。范围：发布检查表、迁移/回滚记录、管理员交接与受控生产发布。

发布门禁：P0/P1阻断问题关闭；B01–B06均已解决或用户明确缩小范围；版本、密钥、安全、备份、来源、质量、资源验收有证据；安装/迁移/部署均获批准。

验收：小范围发布先人工审核，再逐来源自动；浏览/管理员/采集/容量告警验证；原有服务器服务仍健康；提供暂停/恢复/回滚/备份操作手册。没有获得实际部署授权或没有运行验证，就不能标done。

## 4. 清理原则与二期边界

可以在相关任务中清理：已证明无引用的重复 Store/工具、演示入口、过期配置说明、被新链路替代且已有迁移方案的消费者。每次清理带引用检查和回归证据。

不能顺带清理：用户未提交代码、其他项目服务/目录、旧队列积压、未备份数据库、服务器日志以外的未知文件、历史文章/用户数据。

二期另立设计：站内完整阅读与素材存储、全文内容使用条件、更多游戏、普通用户投稿、真实消息/推送、搜索和高可用。不得以二期为理由让首期架构无法扩展，也不得把二期任务偷偷塞入首期。

## 5. 证据格式与当前下一步

建议进度记录字段：日期、task_id、目标、变更文件、实际执行命令、结果/测试数、证据路径、未验证项、风险、下一步。截图/日志只保留必要且脱敏部分，禁止仓库无限堆积运行产物。

当前下一项：**BASE-01**。先完整核对接口和可运行基线，再实施 SEC-01；不要先重写爬虫或部署服务器。

## ING-02 当前实施说明（2026-09-20）

已实现 SQLite outbox、HTTP receipt ACK、内部服务令牌、幂等 receipt 和入站文章事务写入。新事件默认进入 `PENDING_REVIEW`，不会因 `ACCEPT` 而绕开来源审核设置直接公开。`content_source` 不存在时以候选来源登记，`article_game` 只允许四个首期游戏，`article_content` 中的暂存内容不通过首期公开 DTO 输出。

还缺少一次**隔离 MySQL**上的 V4 + Mapper 真实事务回滚验证；未完成前 ING-02 保持进行中。旧 Redis List 的启用条件和零清空迁移约束见 `API_COMPATIBILITY.md`。

### ING-02 completion record — 2026-09-20

A new temporary MySQL 8.0.45 instance was initialized at `127.0.0.1:33067` with the separate data directory `C:\mysql-flyway-test`; no existing `MySQL80` service, existing database, remote server, or Docker project was used. `MySqlFlywayIntegrationTest` verified V1–V4 empty-schema idempotence and legacy V1 upgrade. `IngestReceiptMySqlTransactionTest` verified real MyBatis writes for receipt/article/article_content/article_game, replay without duplicate business effect, and complete transaction rollback when a later game mapping fails. The test server was shut down and `C:\mysql-flyway-test` was removed after verification.

### ING-03 completion record — 2026-09-20

The initial CS2 Steam API path was tested but is `PAUSED` for this local runtime because Python httpx/requests received timeout or disconnect failures despite a PowerShell HTTP probe succeeding; this was documented rather than bypassed. Per the plan's source-failure fallback, the vertical sample moved to the verified official VALORANT update route.

`valorant-official` uses `https://playvalorant.com/en-us/news/game-updates/`. A live `python main.py --source valorant-official --dry-run` run discovered six Riot official entries and deterministically classified the five patch-note entries as `ACCEPT / valorant / patch_notes`; no AI, OSS, outbox, Redis dedup, or automatic publication occurred in dry-run. Captured real list/detail samples live under `ingest/tests/fixtures/` and tests preserve original URL, original ISO publication time, title and source body through the versioned event. Isolated MySQL verification then wrote the corresponding review article, performed a test-only publish state transition, and queried it via the normalized `article_game + content_type` read mapper; replay did not duplicate the article.

ING-03 is complete. WEB-01 is next: move the Vue public experience to the v1 client and real summary/original-link behavior.

### WEB-01 / ADMIN-01 progress — 2026-09-20

WEB-01 public Vue path now uses `/api/v1/games`, `/api/v1/articles` and `/api/v1/articles/{id}`; it exposes four-game and content-type filters, paging, initial-request de-duplication, loading/empty/error states and the summary-to-original reading flow. Legacy carousel, message-center demo, public publish view and unused legacy article store were removed only after a source import search and route replacement. `ArticleList.test.js` validates the v1 filter request and error state.

ADMIN-01 has begun with authenticated content-admin endpoints for review list and revision-locked approve/reject/unpublish actions. Each state change is transactionally audited in `moderation_audit`; state transitions reject illegal workflow changes and stale revisions. Admin source/run status UI and the operator-facing moderation page remain pending.

### SOURCE / ADMIN policy progress — 2026-09-20

Two official English sources are now registered as `SHADOW` through versioned migrations: VALORANT game updates and Apex Legends EA news. The crawler has no business-database credential; when an internal endpoint/token is configured, it asks the service for the source policy before fetching. The content administrator's pause/resume state therefore directly gates new source execution. A new isolated MySQL 8.0.45 verification applied V1–V6 and confirmed the ingest transaction plus pause/resume-to-policy behavior; the temporary instance was shut down and `C:\mysql-flyway-test` removed afterwards.

The CS2 Steam News API remains documented as PAUSED in the local runtime because Python HTTP connectivity was not stable. Delta Force is still CANDIDATE, so neither is represented as coverage complete.

### QUALITY-01 bootstrap — 2026-09-20

`ingest/quality/evaluate.py` now evaluates a JSONL frozen set against the deterministic FPS rules and fails the command when auto-publish eligibility is not proven. It enforces the design gates: at least 160 frozen samples, auto-ACCEPT precision ≥95%, positive coverage ≥90%, and zero known negative automatic accepts. The present four-record smoke set only verifies the evaluator and intentionally exits nonzero for eligibility; it is not represented as a quality result or authorization to enable automatic publishing.

### DEPLOY-01 preparation — 2026-09-20

Added an unexecuted isolated Compose preparation under `deploy/`: backend/frontend/ingest Dockerfiles, internal-only MySQL/Redis/backend network, named GameBoi volumes, front-end as the only host-port publisher, sample environment file and profile-gated ingest execution. `docker compose -f deploy/compose.yaml config --no-interpolate` validates the configuration without building or starting containers. This is not deployment approval and does not meet the image-digest, storage-quota, capacity-measurement, backup or host-change gates required to close DEPLOY-01.

### Completion audit baseline — 2026-09-20

Added `docs/COMPLETION_AUDIT.md` as the release proof ledger. It explicitly separates verified engineering work from blocked external requirements and forbids treating SHADOW sources, unmeasured resource limits, or unapproved Compose preparation as production readiness.

### 评论写入保护补充

评论服务端默认正文上限为1000个Java字符（与浏览器maxlength口径一致），空白正文、无效文章/用户ID不落库；parentId/replyId必须属于当前文章。此上限只限制请求载荷，不替代频率限制、容量准入或运营处置。评论提交入口在举报/删除处置与限流验收完成前仍不开放前端。

### 容量观测接口澄清

不得用shutil.disk_usage(root).used充当项目占用：它是宿主分区全部占用，包含其他项目。采集门禁改为读取只读JSON快照（CAPACITY_SNAPSHOT_PATH）：scope=gameboi-project、observedAt为UTC epoch秒、components包含images/volumes/logs/temporary/backups五项非负整数字节，hostAvailableBytes为宿主可用字节。项目用量为五项之和；缺失/非法/超过120秒或未来时间失真快照一律UNKNOWN停采。宿主可用小于5,000,000,000字节额外STOP。
采样器需要独立核验完整口径后才写快照，此契约不等于已经实现Docker/卷/备份采样，不替代配额、写入预留及4GB内存实测。没有真实观测时明确停采，而不是使用演示读数允许生产运行。

### 来源发布时间精度补充

来源仅提供日历日期时使用originalPublishedDate（YYYY-MM-DD），originalPublishedAt保持null，不擅自构造午夜或时区；来源提供完整带offset时间时沿用originalPublishedAt。入站不得同时给出两个字段。数据库新增可空DATE列，公开DTO保留日期字段，前端显示“仅日期”。旧数据不自动填充或转换。本变更解决Apex列表日期被错误包装成无时区datetime并遭入站拒绝的问题，不扩大首期全文展示范围。

### WEB-01 / ADMIN-01 visual direction update — 2026-09-22

产品负责人已批准将活跃的公开页面与管理后台统一重构为浅色 FPS 电竞资讯媒体。实现范围为独立 GAMEBOI 落地页、首页、资讯详情、登录/注册、个人中心、收藏及审核后台；必须保留现有路由、API、认证/授权与审核工作流。视觉使用白/雾灰/墨黑基础色和竞技红重点色，中文阅读和系统字体优先；桌面端采用编辑精选资讯门户布局，移动端保证核心阅读/筛选/账户操作。禁止为本任务下载外部图片、生成新视觉素材、引入在线字体或改变四游戏内容白名单。验证仍需覆盖 Vue build、既有单元测试、减少动态效果，以及已有业务状态和错误/空状态。


