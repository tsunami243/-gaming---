# 当前 API 与前端调用矩阵（BASE-01）

- 审计日期：2026-09-20
- 审计方法：静态读取 Spring Controller、SecurityConfig、Vue Router、`src/api` 和调用点；未启动 HTTP 服务或连接本地数据库/Redis。
- 代码基线：`feature/push-platform`，HEAD `d9537c1`；不包含本轮后续安全整改的预期接口。
- 目标：记录**现存**路由与调用关系，作为 SEC-01、DATA-01、API-01 的输入；不是对外 API 文档或安全保证。

## 1. 当前传输约定

- Vue Axios 基地址为 `/api`；开发代理将 `/api` 去除并转发到 `http://localhost:8080`。后端当前 Controller 路径**没有** `/api` 前缀。
- 当前成功响应大多是 `{ code: 0, message: "操作成功", data }`；业务失败仍可能返回 HTTP 200 和 `code: 1`。安全过滤器会返回 401。此行为会在 API-01 重构，当前不能假定 4xx/5xx 语义完整。
- JWT 使用 `Authorization: Bearer <token>`。当前前端路由守卫只检查 localStorage token；后端仍是最终授权边界。
- 当前 `PageBean` 序列化字段是 `total` 和 `items`，**不是** `records`。
- API 中的 article/category 整数 ID 是旧数据库键；新契约要转向稳定 game code，详见设计基线第 7 节。

## 2. 安全配置的静态判定

| 路径/方法规则 | 当前安全规则 | BASE-01 结论 / 后续动作 |
|---|---|---|
| `/user/login`、`/user/register` | permitAll | 保留为公开入口，SEC-01 补限流/校验测试 |
| `/article-view/**` | permitAll | 需确认访客统计的反滥用与仅发布文章限制 |
| `/push/**` | permitAll | P0：生产必须禁用或改为受保护管理接口 |
| POST/PUT/DELETE `/category` | CONTENT_ADMIN 或 SUPER_ADMIN | 当前唯一明确内容管理角色规则 |
| POST/PUT/DELETE `/article/**` | authenticated | P0：普通用户仍可写，需改为内容管理员 |
| POST/DELETE `/comment/**` | authenticated | 保留前需验证文章可见性、对象权限、限流 |
| `/upload` | authenticated | 需加入管理员/用途策略，当前前端未发现调用 |
| GET `/article/**`、`/category/**`、`/comment/**` | permitAll | P0：服务/Mapper 未强制 published，可能泄露草稿 |
| 其余 | authenticated | 包含收藏、点赞、用户资料和 `/admin/ingest/consume`；后者不是管理员专用规则 |

`/admin/ingest/consume` 当前只落在 `anyRequest().authenticated()`，普通登录用户可尝试调用；`/push/**` 是无认证 demo。这两项均由 SEC-01 收敛。

## 3. Controller 路由矩阵

“前端使用”仅根据当前 `gaming-news/src` 静态引用；“未知”不是未使用证明，可能有外部客户端、手动调用或未扫描到的动态拼接。

### 3.1 文章与分类

| 方法/现有路径 | 参数或主体（摘要） | 前端使用 | 当前状态/风险 |
|---|---|---|---|
| POST `/article` | Article JSON | PublishView | 普通已登录用户可发布；首期应改管理员 |
| GET `/article` | pageNum/pageSize/categoryId/state | ArticleList、旧 article Store | 列表可传任意 state；不传 state 时未强制 published |
| GET `/article/search` | pageNum/pageSize/tag | 未发现 | 标签搜索也未限定发布状态 |
| PUT `/article` | Article JSON | 未发现 | 仅创建者可改，管理员不能审核系统文章 |
| GET `/article/detail` | id | ArticleDetailView | 未限定发布状态 |
| DELETE `/article` | id | API 客户端导出，页面未发现 | 仅创建者删除；需管理下架模型 |
| GET `/article/carousel` | limit | SwiperBanner 间接使用 | Mapper 已显式 state=`已发布`，但需统一新状态枚举 |
| GET `/article/stats` | articleId | ArticleDetailView | 未确认目标文章可见性 |
| GET `/article/heat` | articleId | API 客户端导出，页面未发现 | 计算接口应检验文章可见性 |
| GET `/article/hot` | limit | 未发现 | 服务 fallback/查询需强制 published |
| POST `/category` | Category JSON | 未发现 | 管理员角色规则存在 |
| GET `/category` | — | category Store | 动态分类是当前首页实际来源 |
| GET `/category/detail` | id | 未发现 | 需迁移为 game dictionary/管理分类策略 |
| PUT `/category` | Category JSON | 未发现 | 管理员 |
| DELETE `/category` | id | 未发现 | 管理员 |

### 3.2 用户与媒体

| 方法/现有路径 | 参数或主体（摘要） | 前端使用 | 当前状态/风险 |
|---|---|---|---|
| POST `/user/register` | username/password | RegisterView | 公开；注册角色赋值与密码规则待 SEC-01 核验 |
| POST `/user/login` | username/password/rememberme | LoginView | 返回裸 JWT 字符串作为 data |
| GET `/user/userInfo` | — | user Store | 已登录 |
| GET `/user/list` | — | 未发现 | anyRequest authenticated，但不表示应向所有用户返回用户列表 |
| PUT `/user/update` | User JSON | UserView/user API | 已登录；对象字段白名单待审计 |
| PATCH `/user/updateAvatar` | avatarUrl query | user API | 已登录 |
| PATCH `/user/updatePwd` | old_pwd/new_pwd/re_pwd JSON | user API | 已登录 |
| POST `/user/refresh` | — | 未发现 | 已登录；刷新 token 语义待 API-01 对齐 |
| POST `/upload` | multipart file | 未发现 | 已登录、仅扩展名+体积检查；未用于首期摘要跳转 |

### 3.3 互动与浏览

| 方法/现有路径 | 参数或主体（摘要） | 前端使用 | 当前状态/风险 |
|---|---|---|---|
| POST `/article-like` | articleId query | ArticleDetailView | 已登录；需要校验已发布对象 |
| DELETE `/article-like` | articleId query | ArticleDetailView | 同上 |
| GET `/article-like/list` | — | 未发现 | 需确定是否仅本人 |
| GET `/article-like/check` | articleId | ArticleDetailView | 前端只在登录时调用 |
| GET `/article-like/count` | articleId | ArticleDetailView | 公开读需校验可见性 |
| POST `/article-favorite` | articleId | ArticleDetailView | 已登录 |
| DELETE `/article-favorite` | articleId | ArticleDetailView | 已登录 |
| GET `/article-favorite/check` | articleId | ArticleDetailView | 已登录 |
| GET `/article-favorite/count` | articleId | 未发现 | 公开读可见性待校验 |
| GET `/article-favorite/list` | — | 未发现 | 本人收藏列表 |
| POST `/comment` | Comment JSON | ArticleDetailView | 已登录；需要发布状态和限流 |
| POST `/comment/{articleId}/reply` | Comment JSON | 未发现 | 已登录 |
| GET `/comment/{articleId}` | — | ArticleDetailView | 公开；需只返回公开文章有效评论 |
| GET `/comment/{articleId}/tree` | — | 未发现 | 同上 |
| DELETE `/comment/{id}` | — | API 客户端导出，页面未发现 | 已登录；删除对象权限待测试 |
| PUT `/comment/{id}/like` | — | 未发现 | 与 `/comment-like` 并存，疑似旧/重复能力 |
| POST `/comment-like` | commentId | API 客户端导出，页面未发现 | 已登录 |
| DELETE `/comment-like` | commentId | API 客户端导出，页面未发现 | 已登录 |
| GET `/comment-like/list` | — | 未发现 | 已登录 |
| GET `/comment-like/check` | commentId | API 客户端导出，页面未发现 | 已登录 |
| GET `/comment-like/count` | commentId | API 客户端导出，页面未发现 | 公开读规则需要确认 |
| POST `/article-view` | articleId query | ArticleDetailView（不等待结果） | permitAll；IP 从转发头/remoteAddr 提取，代理信任策略待审计 |
| GET `/article-view/count` | articleId | 未发现 | 公开读可见性待校验 |
| GET `/article-view/unique-count` | articleId | 未发现 | 同上 |

### 3.4 采集与推送 demo

| 方法/现有路径 | 参数或主体（摘要） | 前端使用 | 当前状态/风险 |
|---|---|---|---|
| POST `/admin/ingest/consume` | —，从 Redis LPOP | 未发现 | P0：仅 anyRequest authenticated；与定时消费者重复；直接 LPOP 无可靠 receipt |
| POST `/push/tasks` | userId/content JSON | 未发现 | permitAll demo，生产禁用 |
| POST `/push/simulate/batch` | count | 未发现 | permitAll，可制造任务；生产禁用 |
| GET `/push/tasks/stats` | — | 未发现 | permitAll demo |
| GET `/push/tasks/dead` | — | 未发现 | permitAll demo |
| POST `/push/deadletters/reprocess` | — | 未发现 | permitAll demo |
| GET `/push/mock/metrics` | — | 未发现 | permitAll demo |

## 4. 前端路由与活跃数据路径

| 路由/组件 | 当前数据路径 | 审计结论 |
|---|---|---|
| `/` LandingView | 静态视觉页 | 可保留；上线前核验入口跳转 |
| `/home` HomeView + ArticleList | `/category`、`GET /article`，组件读 `data.items` | 当前首页实际分页字段与 PageBean 对齐；ArticleList 的 watch + onMounted 会重复请求 |
| `/article/:id` ArticleDetailView | detail/stats/comment/like/favorite/view | 需在 SEC-01 后验证未发布 404 和互动对象可见性 |
| `/publish` PublishView | `POST /article` | P0：当前仅 requiresAuth；首期移为管理员页面/路由 |
| `/message` MessageView | 本地硬编码数组 | 首期隐藏，不将演示数据作为真实消息 |
| `/user` UserView | user Store/API | 已登录；需要字段白名单与错误反馈复核 |
| `/login`、`/register` | user API | 公开 |
| `stores/article.js` | `GET /article` 后读 `data.records` | 旧路径字段错误，目前主页不使用；引用检查后迁移或删除 |
| `utils/category.js` | 静态四分类 ID | 与动态 category Store、Python CATEGORY_MAP 重复；新模型统一 game code 后移除/改造 |

## 5. 契约差异、遗留和行动项

| ID | 观察 | 后续任务 |
|---|---|---|
| A01 | 现有 `/article` 公开查询和详情没有显式 published 条件 | SEC-01 |
| A02 | `POST/PUT/DELETE /article/**` 只需登录，和管理员发布决策冲突 | SEC-01 |
| A03 | 管理员无法按角色编辑/审核 `createUser=1` 的采集文章 | SEC-01、DATA-01 |
| A04 | Python payload 中 summary/source/original_url/crawled_at 未进入现有 Article | DATA-01/API-01 |
| A05 | 新旧 Store 使用 records/items 两种分页字段 | WEB-01；API-01 固定 items |
| A06 | 评论点赞有 `/comment/{id}/like` 与 `/comment-like` 两套接口 | SEC-01 先安全覆盖；API-01 决定保留一种并写弃用计划 |
| A07 | 当前演示 push 和手动 ingest 入口不适合作为生产 API | SEC-01、ING-02 |
| A08 | API 不区分公开、管理、内部命名空间 | API-01，迁移到 `/api/v1`、`/internal/v1` |
| A09 | 当前无前端真实消息后端，MessageView 是 mock | WEB-01：隐藏入口，不在首期补造消息系统 |
| A10 | 用户列表、上传、评论/互动权限与敏感字段尚无端到端安全测试 | SEC-01/BASE-01 后续测试矩阵 |

## SEC-01 修复记录（2026-09-20）

BASE-01 的矩阵记录的是修复前静态状态。SEC-01 已完成以下边界修复；新旧路由的最终替换仍由 API-01 负责。

| 项目 | 已实施行为 | 验证 |
|---|---|---|
| 文章写操作 | POST/PUT/DELETE `/article/**` 仅 CONTENT_ADMIN/SUPER_ADMIN；匿名为 401、普通用户为 403 | SecurityBoundaryTest |
| 旧采集入口 | `/admin/**` 仅内容管理员或超级管理员；旧入口仍未成为新采集方案 | SecurityBoundaryTest |
| 推送 demo | `/push/**` denyAll，生产不可用 | SecurityBoundaryTest |
| 旧评论点赞计数更新 | PUT `/comment/**` denyAll；保留的 comment-like 接口仍需登录 | SecurityBoundaryTest |
| 公开文章 | list/search 强制 `已发布`；detail、stats、heat 对非已发布返回 HTTP 404；热榜按已发布文章查询 | ArticleVisibilityServiceTest + SecurityBoundaryTest |
| 互动目标 | 点赞、收藏、评论、浏览及评论点赞在触达 article/comment 前要求所属文章已发布 | 编译/回归；完整 HTTP/DB 集成测试留给后续 |
| 管理员系统文章 | 内容管理员/超级管理员编辑或删除时可越过 `createUser`，但保留原创建者 | ArticleVisibilityServiceTest |
| 普通用户发布 UI | 移除导航/个人中心/首页入口；`/publish` 直接重定向 `/home` | router Vitest |
| HTTP 状态 | GlobalExceptionHandler 专门保留 ResponseStatusException 的 404/403 等 HTTP 状态，不再统一吞成 200 | SecurityBoundaryTest |

限制：此阶段没有新增正式管理员后台，也没有启用新的 `/api/v1`、`/internal/v1` 契约；管理员可通过现有受保护接口操作，完整管理流程属于 ADMIN-01。用户资料字段白名单、登录/评论限流、上传安全与真实 Redis/JWT/数据库联调仍未完成。

## 6. BASE-01 验证结果与限制

| 检查 | 本轮结果 |
|---|---|
| `./mvnw.cmd test` | PASS：13 tests，0 failures/errors/skips；Mockito 动态 agent 有未来 JDK 兼容警告，非本轮阻断 |
| `npm run build` | PASS：Vite 8.0.10；产生 >500 kB chunk 警告，WEB-01 优化，不以此阻断 |
| `npx vitest run`（改造前） | FAIL：无匹配测试文件，退出码 1；本轮已增加最小 util 测试与 `npm test` 命令，需复跑 |
| `python -m compileall -q .` + imports | PASS：未调用网络、Redis、OSS 或 AI |
| HTTP/DB/Redis 联调 | 未执行：当前任务不连接本地或生产数据服务 |

后续正式契约以 `docs/api/openapi.yaml` 为准；该文件属于 API-01，当前尚未创建。
