# goal.md — GameBoi 待解决目标清单

> 面向 coding agent 的工作清单。每条 GOAL 是一个独立、可验收的修复单元，**未获用户确认前不要实施**。
> 约定：先对每条 GOAL 与用户沟通 → 产出方案文档 `goals/<GOAL-ID>.md` → 再编码 → 记录证据。
> 状态取值：`open` / `discussing` / `solution-ready` / `done`。
> 硬约束（沿用 AGENTS.md）：本地用 PowerShell；不自动 git commit/push/部署；不改四游戏白名单、管理员发布与审核策略、环境隔离、资源预算（内存 ≤4 GB、磁盘 ≤10 GB）。

## 环境事实（agent 参考）

- 根目录：`C:\Users\帅比谢银荣\minimax.workingSpace\gaming`
- 后端：`src/main/java/org/example/bigevent`（新契约在 `api/v1`）
- 前端：`gaming-news/src`
- 统一验证入口：`scripts/check.ps1`
- 启动必填环境变量：`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET_KEY`、`INGEST_SERVICE_TOKEN`；且 Redis 必须运行（见 `src/main/resources/application.yml`）
- 后端真值枚举（前端必须对齐）：
  - `article.workflowStatus` ∈ `PENDING_REVIEW | PUBLISHED | REJECTED | UNPUBLISHED`（`api/v1/admin/AdminModerationService.java:21`）
  - `content_source.verificationStatus` ∈ `CANDIDATE | SHADOW | FETCH_VERIFIED | PAUSED`（`api/v1/admin/AdminOperationsMapper.java:11,14,17`）

## 总览

| ID | 标题 | 严重 | 类型 | 状态 |
|---|---|---|---|---|
| GOAL-01 | 管理后台状态筛选值损坏 | P0 | bug | done |
| GOAL-02 | 管理后台来源暂停状态判断错误 | P0 | bug | done |
| GOAL-03 | 站内搜索缺失 | P1 | feature | open |
| GOAL-04 | 热榜 / 轮播入口缺失 | P1 | feature | open |
| GOAL-05 | 浏览埋点未接，浏览量/热度恒为 0 | P1 | bug/feature | open |
| GOAL-06 | 评论提交被禁用，缺处置闭环 | P1 | feature | open |
| GOAL-07 | 账号：缺找回密码与资料编辑入口 | P2 | feature | open |
| GOAL-08 | 互动接口仍走 legacy，未迁 v1 | P2 | debt | open |
| GOAL-09 | 缺管理员建/改文章接口与 UI | P0 | feature | open |
| GOAL-10 | 站点无内容来源；自动发布被门禁阻断 | P0 | process | open |
| GOAL-11 | 未部署：无镜像/域名/配额/备份 | P0 | ops | open |
| GOAL-12 | 视觉重构靠全局 !important 覆盖旧模板 | P2 | debt | open |
| GOAL-13 | 前端打包体积与残留资源 | P2 | debt | open |
| GOAL-14 | legacy 接口/前端死代码未归档 | P2 | debt | open |
| GOAL-15 | 测试环境残留物与 Windows 运行时绕过 | P3 | ops | open |

---

## GOAL-01 · 管理后台状态筛选值损坏

- severity: P0 · type: bug · area: 前端
- 证据：`gaming-news/src/views/AdminView.vue:6`
  ```
  <option value="拒绝ED">已拒绝</option><option value="下架ED">已下架</option>
  ```
- 真值：后端只接受 `REJECTED` / `UNPUBLISHED`（`AdminModerationService.java:21`）
- 现状：疑似批量替换残留，值非法 → 选择"已拒绝/已下架"时列表永远为空
- 期望：`value` 改为 `REJECTED` / `UNPUBLISHED`
- 验收：切换四个筛选项都发出正确 `workflowStatus` 并返回对应列表
- 验证：`AdminView` 相关 vitest + 手工切换
- 范围：仅 `AdminView.vue`，不动后端

## GOAL-02 · 管理后台来源暂停状态判断错误

- severity: P0 · type: bug · area: 前端
- 证据：`AdminView.vue:23` `source.verificationStatus==='暂停D'`
- 真值：后端写入 `'PAUSED'`（`AdminOperationsMapper.java:14,17`）
- 现状：已暂停来源仍显示 `PAUSE`，永远出不来 `RESUME`
- 期望：比较 `'PAUSED'`
- 验收：暂停后按钮变 `RESUME`，恢复后变 `PAUSE`
- 附带（同区块，可一并处理）：暂停原因用 `window.prompt` 收集，体验差，可换为页内输入
- 范围：仅 `AdminView.vue`

## GOAL-03 · 站内搜索缺失

- severity: P1 · type: feature
- 证据：`gaming-news/src/api/public.js` 仅 3 个函数；v1 仅 `GET /api/v1/games|articles|articles/{id}`（`api/v1/PublicApiV1Controller.java:49,57,81`）；前端 grep 无 search
- 期望：按关键词搜标题/摘要，分页，含加载/空/错误态；只返回 `PUBLISHED`
- 待定：新增 `GET /api/v1/articles?keyword=` 复用现有分页 DTO，还是独立 `GET /api/v1/articles/search`（需在方案文档定）
- 验收：关键词命中已发布文章，未发布不可见，分页稳定排序 `publishedAt DESC, id DESC`
- 范围：不引入搜索中间件（设计 §1.3 明确首期不做全文搜索集群）

## GOAL-04 · 热榜 / 轮播入口缺失

- severity: P1 · type: feature
- 证据：legacy `GET /article/hot`、`/article/carousel` 存在（`controller/ArticleController.java:73,102`）但前端无调用；v1 无对应接口；设计 §7.2 期望 `/api/v1/articles/hot`、`/articles/carousel`
- 期望：首页出现热榜/轮播位 + v1 接口（仅 `PUBLISHED`）
- 依赖：与 GOAL-05 强相关——无浏览数据则热榜无意义，建议两者一起设计
- 范围：不新增素材（设计 2026-09-22 明文：只复用现有封面与四游戏 Logo）

## GOAL-05 · 浏览埋点未接

- severity: P1 · type: bug/feature
- 证据：`gaming-news/src/api/article.js:132` 定义 `recordView`，全项目**无任何调用**；`ArticleDetailView.vue` 不记录浏览
- 影响：`views/uniqueViews` 不增长 → `GET /article/heat`（热度公式）与依赖它的排行恒为 0
- 期望：进入详情页时上报浏览（游客允许，不带 token）；24h 去重由后端 `ViewHistoryServiceImpl` 现有逻辑处理
- 验收：打开详情后 `/article-view/count` 增加；重复访问符合后端去重规则
- 范围：只在详情页加入一次幂等上报，不新增埋点服务

## GOAL-06 · 评论提交被禁用，缺处置闭环

- severity: P1 · type: feature
- 证据：`application.yml:59 comments.submission-enabled=false`；`components/ArticleComments.vue:3` 显示"评论提交暂未开放"；设计 §8 要求先具备删除/举报处置与限流才开放
- 缺口：举报入口、管理员删除/处置、提交限流、容量准入
- 期望：上述闭环具备后，服务端与 UI 同步开启；未具备前保持只读
- 范围：不降低现有正文长度/父子关系校验

## GOAL-07 · 账号：缺找回密码与资料编辑入口

- severity: P2 · type: feature
- 证据：后端有 `/user/updatePwd`、`/user/update`、`/user/updateAvatar`（`controller/UserController.java:95,102,108`）；前端无找回密码流程；`views/UserView.vue` 仅"收藏/审核/退出"，无资料编辑
- 待定（需用户决策）：首期是否做邮箱/短信验证；若不做，需在 UI 明确说明而非留白
- 验收：能从个人中心进入资料编辑并保存成功；错误态可见

## GOAL-08 · 互动接口仍走 legacy

- severity: P2 · type: debt
- 证据：`components/ArticleInteractions.vue:16` 调用 legacy `/article-like`、`/article-favorite`；v1 无对应接口
- 现状：功能可用（已生效），但未纳入 v1 契约
- 期望：依据 `docs/API_COMPATIBILITY.md` 决定迁移或登记保留条件与移除条件；不允许长期并存无人维护的第二套逻辑
- 依赖：需先确认 v1 是否补齐 interaction 端点

## GOAL-09 · 缺管理员建/改文章接口与 UI

- severity: P0 · type: feature（**决定站点能否有内容**）
- 证据：`api/v1/admin/AdminArticleV1Controller.java` 仅 `{id}/audit|approve|reject|unpublish`；设计 §7.2 要求 `GET/POST /api/v1/admin/articles`、`PATCH /api/v1/admin/articles/{id}`；前端 `/publish` 已 `redirect: '/home'`（`router/index.js`）；legacy `POST /article` 存在但无 UI
- 现状：机器不自动发、人工也无法补录 → 新站可能空壳
- 期望：CONTENT_ADMIN 可新建/编辑文章的标题、摘要、原文链接、游戏标签、内容类型；带 `revision` 乐观锁与 `moderation_audit` 审计
- 顺带修复：审核页目前只能看外链，看不到正文/图片预览（`AdminView.vue:11`）
- 验收：管理员创建 → 进入待审核/发布流程；普通用户 403；修订冲突不静默覆盖

## GOAL-10 · 站点无内容来源；自动发布被门禁阻断

- severity: P0 · type: process
- 证据：Flyway V1–V11 只 seed `game`/`content_source`，**无文章**；6 个来源 `auto_publish_enabled=0`；`feature_list.json` QUALITY-01 需 ≥160 条冻结标注集，尚未建
- 现状：三层阻断叠加 → 站点没有内容上线路径
- 期望（按序）：① 逐来源影子运行 + 人工核查；② 建 160 条标注集（4 游戏各 ≥20 正例 + ≥80 负例/边界）；③ 达标后按来源开启自动发布；④ 门禁通过前，用 GOAL-09 提供人工补录通道
- 待核实：当前业务库是否已有历史文章/数据（未确认，勿假设）
- 范围：**不得**为凑数量或上线而降低质量阈值

## GOAL-11 · 未部署：无镜像/域名/配额/备份

- severity: P0 · type: ops
- 证据：`deploy/README.md` 自述 "preparation only ... has not been built, started, installed"；M6 `DEPLOY-01`/`OPS-01` in_progress，M7 `RELEASE-01` pending；镜像 digest 未锁定、磁盘配额/内存实测/备份恢复演练未做
- 期望链路：锁定镜像 digest → 隔离 Compose 构建 → cgroup 内存与磁盘实测 → 备份恢复演练 → 解决 B01–B06 → 小范围发布
- **必须用户显式授权**：B01 域名/HTTPS、B02 备份目标与费用、B06 安装容器运行时与宿主改动
- 范围：不复用/清理其他项目容器、卷、网络

## GOAL-12 · 视觉重构技术债

- severity: P2 · type: debt
- 证据：`gaming-news/src/assets/styles/pixel.css` 用大量 `!important` 覆盖旧像素模板；`LoginView/RegisterView/UserView/TopNav/BottomNav/ArticleInteractions/ArticleList/ArticleDetailView` 仍保留 `--gb-*`、`pixel-bg`、`font-pixel`、`px-img` 等旧类名；`main.js:7` 引入 `pixel.css`
- 现状：视觉是"覆盖"出来的，不是组件级实现 → 改样式易互相打架
- 期望：把 token 与组件样式收敛到组件/主题层，去掉 `!important` 覆盖，将 `pixel.css` 重命名为语义化文件名
- 范围：不改路由、接口、授权、审核策略、四游戏白名单（沿用 2026-09-22 视觉批准边界）

## GOAL-13 · 前端打包体积与残留资源

- severity: P2 · type: debt
- 证据：`main.js` 全量 `app.use(naive)`；`src/assets/` 残留 `hero.png`、`vite.svg`、`vue.svg`、占位 `logo.png`；build 报 chunk >500kB（index ≈1.31MB，见 `feature_list.json` WEB-01 证据）
- 期望：评估 naive-ui 按需引入或替换为轻量实现；清理未引用资源；拆分首屏 chunk
- 验收：`npm run build` 成功且首屏 chunk 明显下降；不破坏现有页面

## GOAL-14 · legacy 接口 / 前端死代码未归档

- severity: P2 · type: debt
- 证据：
  - 后端：`push/PushController.java` 的 `/push/**`（生产已禁用但代码在）；`controller/IngestController.java` `/admin/ingest/consume`；`controller/ArticleController.java` `/search|/carousel|/stats|/heat|/hot`
  - 前端：`api/article.js` 多数函数无引用（`addArticle`/`deleteArticle`/`getCarouselList`/`getArticleStats`/`getArticleHeat`/`getCommentList`/`addComment`/`deleteComment`/`likeComment` …）
- 期望：按 `docs/API_COMPATIBILITY.md` 的移除条件，**先做引用检查再归档**；不删除演示代码假装整理完成
- 约束：不直接清空真实队列；不动未备份数据

## GOAL-15 · 测试环境残留物与 Windows 运行时绕过

- severity: P3 · type: ops
- 证据：临时 MySQL 数据目录 `C:\mysql-flyway-test`、`C:\mysql-gameboi-http-test` 因清理命令被策略拒绝而保留；Windows/JDK 下嵌入式 Tomcat loopback 异常改用短 `TEMP`（`C:\jtmp`）绕过
- 期望：对上述目录给出明确的保留/清理决定并写入文档；记录绕过方案，避免误删业务数据
- 范围：**禁止**对这些目录做无条件递归删除

---

## 讨论顺序建议

1. **P0 快修**：GOAL-01、GOAL-02（纯前端、0 依赖、立刻可验证）
2. **P0 内容链**：GOAL-09 → GOAL-10（决定站点是否"有东西看"）
3. **P1 阅读闭环**：GOAL-05 + GOAL-04 + GOAL-03（浏览→热度→榜位→搜索）
4. **P1 互动**：GOAL-06
5. **P0 上线**：GOAL-11（需授权）
6. **P2/P3 收尾**：GOAL-07、GOAL-08、GOAL-12、GOAL-13、GOAL-14、GOAL-15
