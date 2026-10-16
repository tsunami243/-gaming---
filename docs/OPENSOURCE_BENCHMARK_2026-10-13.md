# 同类开源项目对标调研与后续开发建议

- 调研日期：2026-10-13（Asia/Shanghai）
- 调研对象：GitHub 上 18 个与 GameBoi（FPS 游戏资讯聚合与审核平台）同类的资讯/内容聚合、CMS、订阅阅读器项目
- 调研方式：GitHub 搜索 API 检索 → 拉取目录树（`git/trees/HEAD?recursive=1`）→ 通过 raw CDN 精读构建文件、README 与核心实现源码
- 约束声明：本次**只读远程仓库**，未克隆、未运行、未复制任何代码到本地项目，未修改本项目任何文件；本报告是新增文档，不改动现有代码、配置、迁移或设计基线
- 对比基准：`docs/PROJECT_DESIGN.md` v1.0/1.1、`docs/DEVELOPMENT_PLAN.md`、`feature_list.json`（截至 2026-09-22）、`pom.xml`、`ingest/` 目录

> 本报告的定位：**为 §15「开源参考与取舍」提供一份经过实际代码核查的扩充版**。所有结论均标注证据文件；star 数、版本号会随时间变化，任何"引入/复制"动作前必须重新核对许可证与依赖可用性。

---

## 1. 调研方法与局限

| 项 | 说明 |
|---|---|
| 检索方式 | GitHub Search API 多轮关键词（news aggregator / rss reader / 资讯聚合 / 舆情 / hot news / headless cms / topic:cms / topic:rss 等）共 36 组查询，累计去重 400+ 候选仓库 |
| 筛选漏斗 | 400+ 候选 → 按"Java/Spring 生态 + 内容聚合或内容平台 + 有可读源码"筛出 20 个 → 精读 18 个（2 个因无源码价值或纯前端剔除） |
| 精读深度 | 每个仓库读取：目录树（含模块划分）、构建文件（pom.xml / build.gradle / libs.versions.toml）、README、核心实现类（采集调度、去重、幂等、状态机、检索、缓存、审计） |
| 未执行 | 未编译、未运行、未部署任何对标项目；未做性能实测 |
| 局限 | ①GitHub 未授权 API（60 次/小时核心配额），可能漏检高星仓库；②部分仓库无 README 或文档非中文；③0 星仓库代码质量未经过社区验证；④star/pushed_at 为调研时点快照 |

---

## 2. GameBoi 基线快照（对比锚点）

| 维度 | 现状（有代码/文档证据） |
|---|---|
| 后端框架 | Spring Boot **4.0.5** + Spring Framework 7 + MyBatis 4.0.1（`pom.xml`） |
| 编译目标 | `maven.compiler.source/target = 17` |
| 安全 | Spring Security 7 + JWT（jjwt 0.11.5） |
| 数据 | MySQL 8 + MyBatis-Plus 分页 → 实际用 PageHelper 2.1.0；迁移用 **Flyway**（V1–V11 已落库） |
| 缓存 | Redis 7（令牌 / 短期缓存 / 热度） |
| 采集 | Python 3.11（`ingest/`：spiders / filters / agent / runtime / quality / mq / storage），DeepSeek 生成中文标题与摘要，阿里云 OSS（首期摘要模式已不实例化 OSS） |
| 交付可靠性 | **SQLite 持久 outbox + HTTP receipt + MySQL 幂等事务**（eventId 唯一索引、payloadHash 冲突 409） |
| 前端 | Vue 3 + Vite + Pinia（已按浅色 FPS 电竞媒体重构） |
| 关键约束 | 四游戏白名单、管理员发布、三类内容决策（ACCEPT/REVIEW/REJECT）、内存 ≤4GB、磁盘 ≤10GB 含 7/8/9GB 容量状态机 |
| 阶段 | WEB-01 / ADMIN-01 / SOURCE-01 / QUALITY-01 / DEPLOY-01 / OPS-01 均 in_progress；RELEASE-01 pending |

**结论先行：GameBoi 在"可靠性交付"和"资源预算治理"两个维度上，已明显超出本次调研的全部 18 个开源项目。** 下文第 6 节会展开说明这一点意味着什么。

---

## 3. 对标项目清单（18 个）

### A 组 · 直接同类：资讯/订阅聚合系统（9 个）

| # | 仓库 | ★ | 语言 | 框架 / 关键栈 | 许可证 | 最后推送 |
|---|---|---:|---|---|---|---|
| A1 | [Athou/commafeed](https://github.com/Athou/commafeed) | 3630 | Java | **Quarkus 3.39.4**、JDK 25、QueryDSL、Liquibase、React/TS | Apache-2.0 | 2026-10-12 |
| A2 | [eschnou/morningdeck](https://github.com/eschnou/morningdeck) | 14 | Java | **Spring Boot 3.3.5**、JDK 21、JPA、Flyway、Spring AI 1.1、Meilisearch | AGPL-3.0 | 2026-09-28 |
| A3 | [DreamTes/RssHub](https://github.com/DreamTes/RssHub) | 0 | Java | **Spring Boot 3.4.4**、JDK 21、MyBatis 3.0.4、PageHelper、Redis、Rome、Jsoup | 无 LICENSE 文件 | 2025-05-04 |
| A4 | [vanillaSky00/briefly](https://github.com/vanillaSky00/briefly) | 0 | Java | **Spring Boot 3.5.5**、JDK 17、JPA、Flyway、OpenFeign、Testcontainers | MIT | 2026-03-21 |
| A5 | [FactFeed/fact-feed](https://github.com/FactFeed/fact-feed) | 0 | Java | **Spring Boot 3.5.5**、JDK 21、Selenium、Jsoup、Spring AI（Gemini） | 无 LICENSE 文件 | 2025-09-25 |
| A6 | [denizsullu/newsflow](https://github.com/denizsullu/newsflow) | 0 | Java | **Spring Boot 4.0.6**、JDK 21、JPA、Spring Cache | MIT | 2026-05-14 |
| A7 | [tagnja/hot-crawler](https://github.com/tagnja/hot-crawler) | 66 | Java | Spring Boot 2.1.6、JDK 8、Jsoup、Redis | MIT | 2023-06-14 |
| A8 | [andyRon/ARLeadnews](https://github.com/andyRon/ARLeadnews) | 0 | Java | Spring Cloud Alibaba 微服务 + Gateway + MinIO + 阿里云内容安全 | Apache-2.0 | 2026-03-27 |
| A9 | [sunflower0305/news](https://github.com/sunflower0305/news) | 3 | Java | Spring Boot 2.7.18、JDK 11、MyBatis、Lucene 8.11.1、Liquibase、Flowable | Apache-2.0（派生自 GPL 的 ujcms，声明不一致） | 2025-06-08 |

### B 组 · 内容平台 / CMS：内容模型、审核、检索、权限（7 个）

| # | 仓库 | ★ | 语言 | 框架 / 关键栈 | 许可证 | 最后推送 |
|---|---|---:|---|---|---|---|
| B1 | [halo-dev/halo](https://github.com/halo-dev/halo) | 39940 | Java | **Spring Boot 4.1.1**、R2DBC + r2dbc-migrate、**Lucene 10.5**、插件/主题/RBAC | GPL-3.0 | 2026-10-12 |
| B2 | [sanluan/PublicCMS](https://github.com/sanluan/PublicCMS) | 2093 | Java | **Spring Boot 4.1.1 / Spring 7.0.9**、JDK 17、MyBatis、Lucene 9.12、Quartz、Hibernate Search+ES、全站静态化 | V4–V6 个人免费/商用付费；V2021 MIT | 2026-10-12 |
| B3 | [diyhi/bbs](https://github.com/diyhi/bbs) | 1086 | Java | **Spring Boot 4.0.3**、JDK 21、JPA、Ehcache、Lucene、分表、敏感词组件 | AGPL-3.0 + 官网声明禁止商用 | 2026-04-15 |
| B4 | [dromara/ujcms](https://github.com/dromara/ujcms) | 667 | Java | **Spring Boot 3.5.16**、JDK 17、MyBatis、Lucene 8.11.4、Liquibase、Quartz、Flowable、ES | GPL-3.0 | 2026-09-21 |
| B5 | [michaelliao/itranswarp](https://github.com/michaelliao/itranswarp) | 796 | Java | Spring Boot 3.0.0、JDK 17、**RediSearch（lettucemod）**、Pebble | GPL-3.0 | 2024-04-10 |
| B6 | [tagbangers/wallride](https://github.com/tagbangers/wallride) | 97 | Java | Spring Boot 1.x 时代、JDK 8、JPA、Infinispan、Thymeleaf、**多语言内容模型** | Apache-2.0 | 2019-04-09（已停更） |
| B7 | [gentics/mesh](https://github.com/gentics/mesh) | 593 | Java | **Vert.x**（非 Spring）、Elasticsearch、headless CMS、插件 API | Apache-2.0 | 2026-10-12 |

### C 组 · 其他 Java 内容系统与工程样板（2 个）

| # | 仓库 | ★ | 语言 | 框架 / 关键栈 | 许可证 | 用途 |
|---|---|---:|---|---|---|---|
| C1 | [201206030/novel](https://github.com/201206030/novel) | 5831 | Java | **Spring Boot 3.3.0**、JDK 21、MyBatis-Plus、ShardingSphere、XXL-Job、ES、RabbitMQ、Redisson、Spring AI、Flyway | Apache-2.0 | Spring Boot 3 生产级工程实践样板（含完整中文教程） |
| C2 | [Naccl/NBlog](https://github.com/Naccl/NBlog) | 2698 | Java | Spring Boot 2.2.x、JDK 8、MyBatis、Quartz、Redis、注解式审计 | MIT | 审核/审计与访问统计的轻量实现参考 |
| C3 | [logamee/lin-cms-spring-boot](https://github.com/logamee/lin-cms-spring-boot) | 953 | Java | Spring Boot 2.5.2、JDK 8、MyBatis-Plus | MIT | 分组-权限-日志模型参考 |
| C4 | [moxi624/mogu_blog_v2](https://github.com/moxi624/mogu_blog_v2) | 1813 | Java | Spring Boot 2.2.2 + Spring Cloud Alibaba（Hoxton）微服务、RabbitMQ、ES | Apache-2.0 | **反向参照**：微服务拆分成本实证 |

> 说明：B7（mesh）与 A1（commafeed）都不是 Spring Boot 项目，作为"跨框架设计参照"保留，不计入"Spring Boot 3+"统计。

**统计口径结论：18 个项目中 Spring Boot 3.x 共 6 个、Spring Boot 4.x 共 5 个、Spring Boot 2.x 及更早 5 个、非 Spring Boot 2 个。**

---

## 4. 对比矩阵

### 4.1 技术栈矩阵

| 项目 | 框架版本 | JDK | ORM | 迁移 | 全文检索 | 缓存 | 前端 | 部署形态 |
|---|---|---|---|---|---|---|---|---|
| **GameBoi** | **SB 4.0.5** | **17** | MyBatis | **Flyway** | 无（二期） | Redis | Vue3+Vite | Compose 五服务 |
| commafeed | Quarkus 3.39.4 | 25 | JPA/QueryDSL | Liquibase | 无（DB like） | 内置 Caffeine/Hazelcast | React+TS | 单 jar / native |
| morningdeck | SB 3.3.5 | 21 | JPA | Flyway | **Meilisearch** | Spring Cache | React+TS | Compose |
| RssHub(DreamTes) | SB 3.4.4 | 21 | MyBatis | Flyway（README 声明） | 无 | Redis | Vue | 单 jar |
| briefly | SB 3.5.5 | 17 | JPA | Flyway | 无 | HikariCP 连接池 | 无（Bot） | Compose |
| fact-feed | SB 3.5.5 | 21 | JPA | Hibernate auto-DDL | 无 | 无 | React+TS | Dockerfile |
| newsflow | SB 4.0.6 | 21 | JPA | 无 | 无 | Spring Cache | React | Compose |
| hot-crawler | SB 2.1.6 | 8 | 无（Redis 快照） | 无 | 无 | **Redis 缓存页面** | Thymeleaf | jar |
| ARLeadnews | Spring Cloud Alibaba | 8 | MyBatis-Plus | 无 | ES（搜索服务） | Redis | Vue | **微服务集群** |
| sunflower0305/news | SB 2.7.18 | 11 | MyBatis | Liquibase | Lucene 8.11.1 | Ehcache+Caffeine | 内置模板+Vue | war/jar |
| halo | SB 4.1.1 | 21+ | R2DBC | r2dbc-migrate | **Lucene 10.5** | 内置 | 自研 UI（Vue3） | Docker |
| PublicCMS | SB 4.1.1 / Spring 7 | 17（V6 需 21） | MyBatis | 自研版本化升级 | Lucene 9.12 + HSearch/ES | 自研多级 | Freemarker + Vue3 | war/jar |
| diyhi/bbs | SB 4.0.3 | 21 | JPA | 无（Init 脚本） | Lucene | **Ehcache** | Vue3 | jar + 前端分离 |
| ujcms | SB 3.5.16 | 17 | MyBatis | Liquibase | Lucene 8.11.4 + ES | Caffeine + Redis | Freemarker + Vue3 | war/jar |
| itranswarp | SB 3.0.0 | 17 | 自研 WarpDB | 自研 DDL | **RediSearch** | RediSearch | Pebble+UIkit | Docker |
| wallride | SB 1.x | 8 | JPA/Hibernate | 无 | ES | Infinispan | Thymeleaf+AngularJS | war |
| mesh | Vert.x | - | Hibernate | 自研 | ES | Hazelcast | 无（API） | Docker/集群 |
| novel | SB 3.3.0 | 21 | MyBatis-Plus | Flyway | ES | Redisson | Vue3 | jar |
| NBlog | SB 2.2.x | 8 | MyBatis | 无 | 无 | Redis | Vue2 | jar + nginx |
| lin-cms | SB 2.5.2 | 8 | MyBatis-Plus | 无 | 无 | 无 | Vue（独立） | jar |
| mogu_blog_v2 | SB 2.2.2 + SCA | 8 | MyBatis-Plus | 无 | ES | Redis | Vue/Nuxt/uniapp | **微服务集群** |

### 4.2 采集与内容管线矩阵

| 项目 | 发现方式 | 抽取方式 | 去重依据 | 幂等 | 失败重试 | 运行预算 | 调度 |
|---|---|---|---|---|---|---|---|
| **GameBoi** | 官方结构化接口/RSS/单游戏栏目 | 适配器 + 待评测 Trafilatura | canonical_url_hash + content_hash + 业务近重复 | **eventId 唯一索引 + payloadHash** | 有界重试 + 退避 + Retry-After | **每日 80 候选 / AI 240 次 / 单任务 30 分钟** | 6/12/24h 固定 + 互斥锁 |
| commafeed | feed URL / HTML 自动发现（`FeedURLProvider`） | RSS/Atom 解析 + 编码检测 | URL + **内容 sha1 + lastPublishedDate + ETag/Last-Modified 三重 NotModified** | 无（订阅模型） | **errorCount 线性退避 + 429 Retry-After 取 max** | 队列批量 `min(100, 3×httpThreads)` | 内存队列 + 15s 补队列循环 |
| morningdeck | RSS / 邮件简报 / 网页 / Reddit | 多源适配 | 有（未精读细节） | 无 | 无（精读范围内） | 无 | Spring @Scheduled |
| RssHub | RSS 源（Rome 解析） | RSS → HTML 清洗（Jsoup） | **`findExistingLinks` 批量 IN 查询** | **无** | 整源 try/catch 标记 `status=error` | 无 | `@Scheduled(fixedDelay=1h)` + 逐源频率 |
| briefly | Feign 调外部 API（HN/Guardian） | JSON 映射 | **urlHash + (source, externalId)** | 无（唯一约束） | 无 | 无 | `@Scheduled(cron)` 可配 |
| fact-feed | Selenium WebDriver | **优先 JSON-LD**，Jsoup 兜底 | URL 存在性过滤 | 无 | 无（异步 join） | 无 | Spring 定时（4 小时） |
| newsflow | 固定三个源 URL | 每源一个 Parser 类 | **Redis 缓存做去重（缓存丢=重复入库）** | 无 | 仅打印日志 | 无 | `@Scheduled(fixedRate=30min)` |
| hot-crawler | 每站一个 Processor 类（50+） | Jsoup 选择器 | **单次请求内内存 Set 去重** | 无 | 无 | 无 | 手动/定时 |
| ARLeadnews | 自媒体端人工投稿 | - | - | 无 | 有（Feign fallback） | 无 | XXL-Job |
| halo | - | - | - | 无 | - | 无 | - |
| ujcms | **采集插件（INPUT_TYPE_COLLECTION）** | 模板/接口采集 | 有 | 无 | 有 | 无 | Quartz |
| diyhi/bbs | 无采集（社区内容） | - | - | 无 | - | - | - |
| novel | 无采集（作者投稿） | - | - | 无 | - | - | XXL-Job |

### 4.3 内容治理矩阵

| 项目 | 状态机 | 来源类型区分 | 审核/审计 | 多语言 | 阅读模式 | 权限模型 |
|---|---|---|---|---|---|---|
| **GameBoi** | DRAFT/PENDING_REVIEW/PUBLISHED/REJECTED/UNPUBLISHED | `reading_mode`（EXTERNAL_SUMMARY/FULL_TEXT） + `source_id` 可空表示管理员原创 | moderation_audit + 规则版本 + reason codes | 原文语言字段（D05） | 摘要 + 原文跳转 | USER/CONTENT_ADMIN/SUPER_ADMIN |
| ujcms | **PUBLISHED(0)/ARCHIVED(1)/READY(5)/DRAFT(10)/PENDING(11)/REVIEWING(12)/DELETED(20)/OFFLINE(21)/REJECTED(22)**，`adjustStatus()` 集中校验 | **INPUT_TYPE_NORMAL/CONTRIBUTE/COLLECTION/INTERFACE/INTERNAL_PUSH/EXTERNAL_PUSH** | OperationLog AOP + 敏感词/易错词 | 站点/组织维度 | 模板渲染 | 用户-角色-组织-栏目四级（RoleArticle/RoleChannel/RoleOrg） |
| halo | 发布/草稿 + **Snapshot 内容修订快照** | - | Role + **RoleBinding** 分离 | 主题级 i18n | 主题渲染 | RBAC（Role/RoleBinding）+ 插件扩展 |
| PublicCMS | 草稿/待审/发布 + 流程（Flowable） | 采集/投稿/接口 | 内容审核 + 操作日志 | 多语言站点 | 模板 + 静态化 + SSI | 组织-角色-权限 |
| diyhi/bbs | 未审/已审（话题/评论/回复分别） | - | 审核 + 敏感词 + 风控词 | - | - | 会员等级 + 员工角色 |
| NBlog | - | - | **@OperationLogger / @VisitLogger / @AccessLimit 注解 + AOP** | - | - | 单管理员 |
| wallride | - | - | - | **BlogLanguage 维度表 + Post/Page/Article 区分** | - | 角色 + 邀请 |
| itranswarp | - | - | 角色（Role 枚举） | - | Markdown 插件渲染 | Role + OAuth2 |

### 4.4 部署与资源矩阵

| 项目 | 容器化 | 资源约束 | 健康/可观测 | 说明 |
|---|---|---|---|---|
| **GameBoi** | ✅ Compose（5 服务，web 唯一对外端口） | ✅ **内存 3.15GB 容器上限、磁盘 10GB + 7/8/9GB 状态机、CPU 3 核** | 结构化日志 + 运维页 + 受限健康接口 | 本次调研中唯一有完整资源预算设计的项目 |
| commafeed | ✅ Dockerfile.jvm + 多平台原生 | README 有 **JVM 内存调优指南**（`-Xmx`、G1 参数、OpenJ9/Semeru） | **Dropwizard Metrics**（队列深度、worker/updater/notifier 活跃数与队列） | 观测设计最接近 GameBoi §13.3 的"轻量"要求 |
| morningdeck | ✅ 三套 compose（quickstart/selfhost/dev） | 无 | **Actuator 自定义 HealthIndicator**（FeedIngestion / NewsProcessing） | 观测方式可直接借鉴 |
| fact-feed | ✅ Dockerfile | **线程池 10 + Selenium（Chrome）** | ApiUsageLog 表 + 用量监控接口 | 资源占用反面教材 |
| RssHub/hot-crawler/newsflow | 部分 | 无 | 无 | - |
| halo | ✅ Docker | 无 | Actuator | - |
| PublicCMS/ujcms/sunflower | ✅ Docker/Compose | 无 | Actuator | 面向大流量（千万 PV）设计，与本项目预算相反 |
| ARLeadnews / mogu | ✅ 集群 | 无 | Zipkin/SBA | 微服务，与 D08 冲突 |
| briefly | ✅ Compose | 无 | Testcontainers 测试 | 测试策略最规范 |

---

## 5. 逐仓库要点（可学 / 不可照搬）

### A1 commafeed（★3630，Quarkus）— 本次最有价值的采集调度参照

**关键实现证据：**

- `FeedRefreshEngine.java`：五种执行器，语义明确区分——`newDiscardingSingleThreadExecutorService()`（重填队列，重复任务直接丢弃）、`newDiscardingExecutorService(threads, queueCapacity)`（推送通知，队列满即丢弃）、`newBlockingExecutorService(threads)`（HTTP 抓取与 DB 写入，**必须阻塞直到有槽位**）。另有 `processFeedAsync` 的 `worker → updater → notifier` 三段 CompletableFuture 流水线。
- `FeedFetcher.java`：三重变更判定——`lastContentHash`（sha1）、`lastModifiedDate`、`lastPublishedDate`，任一未变则抛 `NotModifiedException`；HTML 页面反查 feed URL（`FeedURLProvider`）；`NoFeedFoundException` 语义化。
- `FeedRefreshIntervalCalculator.java`：`onFetchSuccess` / `onFeedNotModified` / `onTooManyRequests(retryAfter, errorCount)` / `onFetchError(errorCount)` 四个入口；经验模式按"距上次发布天数"（≥30 天 → maxInterval，≥14 天 → max/2，≥7 天 → max/4）和"平均发文间隔 ÷ 2"推算；所有结果经 `constrainToBounds` 夹在 `[interval, maxInterval]`；429 取 `max(retryAfter, backoff)`。
- `Digests.java`：sha1Hex/md5Hex 工具类。
- README FAQ：**默认屏蔽本地地址防 SSRF**，可用开关关闭。

**可学：**
1. 条件请求 + 内容指纹的双重"未变更"判定（直接降低来源压力，符合 D10 低频采集）。
2. 自适应刷新间隔 + 错误线性退避 + Retry-After 取最大值。
3. 任务按"可丢弃（通知）"与"不可丢弃（入库）"分层的背压语义。
4. JVM 内存调优参数组合（服务端资源受限场景）。

**不可照搬：**
- 内存队列（`LinkedBlockingDeque`）不满足 GameBoi"Redis 故障不能丢结果"的持久化要求。
- Quarkus 技术栈，与 Spring Boot 4 + MyBatis 路线不同，代码不可移植。
- 无幂等交付概念（订阅模型不需要）。

### A2 morningdeck（★14，SB 3.3.5）— AI 与可观测的最佳参考

**关键实现证据：** `FeedIngestionHealthIndicator.java`、`NewsProcessingHealthIndicator.java`（自定义 Actuator 健康指标）；`MeilisearchConfig`/`MeilisearchProperties`（可选全文检索，未启用时不影响启动）；`AsyncConfig`；`CoreEngineProperties`；`Dockerfile` + 三套 compose；README 说明**两级模型**：`OPENAI_MODEL_LITE`（摘要/分类）与 `OPENAI_MODEL_HEAVY`（简报生成），并支持任意 OpenAI 兼容 `OPENAI_BASE_URL`（OpenRouter/Ollama/Groq）；测试用 Testcontainers（含 `testcontainers-meilisearch`）。

**可学：**
1. 用 Actuator 自定义 `HealthIndicator` 暴露采集与处理健康度——**零新增服务**，完全符合 §13.3"不额外部署大型监控栈"。
2. 按用途分层配置模型 + 可切换 base_url（直接回应 B04"不沿用代码中未验证的模型名"）。
3. 全文检索做成"可选依赖 + 配置开关"，未部署时不影响主流程。

**不可照搬：** AGPL-3.0，代码片段不可进入本项目；Redis 换成 Meilisearch 引入新服务（违反 D08/内存预算）。

### A3 RssHub（★0，SB 3.4.4 + MyBatis + Redis + Vue）— 与本项目栈最接近，但也是最典型的反例

**关键实现证据（`RssService.java`）：** `@Scheduled(fixedDelayString = "${rss.fetch.interval:3600000}")` 全量遍历活跃源；`shouldFetchSource` 用 `lastFetchTime + frequency` 判断；去重用 `articleMapper.findExistingLinks(links)` 后内存过滤；整个源失败即 `source.setStatus("error")`；`fetchRssSourceNow` 记录 `FetchTask`（running/completed/failed + articlesAdded）。

**可学：** 采集任务表（`FetchTask`）的字段设计——startTime/endTime/status/articlesAdded/errorMessage，与 GameBoi 的 `ingest_run` 语义接近，可对照补齐字段。

**不可照搬（重要）：**
- `IN` 列表去重：链接一多就退化为大 SQL，且无法处理同 URL 内容变更。
- 整源 catch-all 后把源标记为 error：一次网络抖动会污染来源可用性状态。
- 无幂等、无持久待交付、无预算上限——这些正是 GameBoi `PROJECT_DESIGN.md` P05–P08 已经修掉的问题。
- 该仓库**无 LICENSE 文件**（README 自称 MIT），不可复制代码。

### A4 briefly（★0，SB 3.5.5）— 六边形架构与测试策略参考

**关键实现证据：** `ContentSourcePort`（域端口：`sourceName()` / `top(limit)` / `byId(id)`）；`FeedIngestionJob`（`@Scheduled(cron=..., zone=...)` → `useCase.topAcrossSources(batchSize)` → `articleService.saveAllFeedItem`）；`ArticleJpaRepository` 同时使用 JPQL（`findBySourceIdAndExternalId`、`findByUrlHashHex`）与**原生窗口函数** `ROW_NUMBER() OVER (PARTITION BY a.source ORDER BY published_at DESC, id DESC)` 取每源最新 N 条；`GuardianFeignConfig` 刻意不加 `@Configuration` 以免 API Key 泄漏到其他 Feign 客户端；README 明确"新增来源 = 新增一个 adapter 类 + 注册 Bean，域与应用层零修改"。

**可学：**
1. **端口-适配器收敛方式**：把 `ingest/spiders/*.py` 收敛为显式 `RawArticle` 契约（§5.1 已要求，但缺少一个"契约即接口"的强制形式）。
2. 每源最新 N 条的窗口函数查询（用于管理端"来源最近内容"预览，避免 N+1）。
3. 凭据按客户端隔离的配置方式。
4. Testcontainers 做真实数据库集成测试（GameBoi 目前是临时 MySQL 实例 + 手工启停，可考虑纳入 `check.ps1`）。

**不可照搬：** 只吃官方 API/RSS，无网页抽取、无分类判定、无审核——业务深度远低于本项目。

### A5 fact-feed（★0，SB 3.5.5 + Spring AI）— AI 管线与"不该做"的双重教材

**关键实现证据：** `ArticleScrapingService.java`（固定 10 线程池 + `CompletableFuture.supplyAsync` 并发抓取 + `@Transactional` 包住整个批次 + `@PreDestroy` 优雅关闭）；README 说明 4 段流水线（Selenium 发现 URL → 批量 15 篇 Gemini 摘要 + **6 个 API Key 轮换** + token 监控 → AI 事件聚类 → 跨源聚合与冲突检测）；`ApiUsageLog` 实体 + `ApiMonitoringController`；`EventMappingService`/`AggregationService`/`EventMergingService`；配置项 `app.startup.auto-scrape=true`（**启动即抓**）。

**可学：**
1. **抽取优先 JSON-LD**（结构化数据比 CSS 选择器抗改版），是对本项目 §5.1 抽取策略的具体补强。
2. AI 用量日志实体字段（对应 §13.3"记录 AI 请求/耗时/实际计费使用量"）。
3. 关闭钩子中优雅停止线程池（GameBoi Python 侧同样需要，`runtime/` 可核对）。

**不可照搬：**
- **Selenium/Chrome 常驻**——直接违反 §5.2"首期不安装常驻 headless browser"，是本次调研中资源代价的最强反例。
- 多 Key 轮换（超出 D09 的低量预算，且放大密钥管理风险）。
- Hibernate 自动建表（拒绝版本化迁移）。
- `app.startup.auto-scrape=true` 的启动即抓（与"每轮任务限 30 分钟 + 互斥"冲突）。
- 无 LICENSE 文件。

### A6 newsflow（★0，SB 4.0.6）— 缓存用法反例

**关键实现证据：** `NewsScheduledTasks`（`@Scheduled(fixedRate=1800000)` + `@CacheEvict(allEntries = true)` 一次性清空四类缓存）；`NewsCacheManager.isUrlCached/cacheUrl` 用**缓存充当去重依据**（`news:<hash>`）；每源一个 `NewsParser` 实现（BBC/NTV/Sozcu）。

**可学：** Spring Boot 4.x 上 JPA + Spring Cache 的实际可用性可以被验证（本项目的 4.0.5 与它的 4.0.6 同代）。

**不可照搬：** 用缓存做去重（缓存失效即重复入库）、全量 `allEntries` 清缓存（高峰击穿）、`System.out.println` 记录失败（无结构化日志）。GameBoi 的「唯一约束 + content_hash」路线显著更稳。

### A7 hot-crawler（★66）— "一源一类"的规模上限实证

**关键实现证据：** `AbstractHotProcessor`（抽象基类里塞入 Jsoup 连接、固定 UA、header 生成、`Connection.Method` 映射）；`HotProcessor` 接口 + **50 余个站点 Processor 实现**（BBC/Reddit/知乎/微博/掘金/InfoQ…）；`HandlerCenter` + `DataHandler` 处理器链，其中 `DeduplicationDataHandler` 仅在**单次请求内**用 `HashSet<String>` 去重。

**可学：** "处理链（handler chain）"思路——把去重/过滤/规范化做成可组合、可单测的小步骤（对 `ingest/filters/` 有参考价值）。

**不可照搬：**
- 抽象基类耦合框架细节（Jsoup、UA、请求方法都在基类），新增来源必须写 Java 类并重新编译——GameBoi 用 Python + `source_registry.py` 数据驱动注册是更正确的方向。
- 内存去重只适用"热榜快照"，不适用资讯归档。
- Redis 缓存页面快照，无持久化语义。

### A8 ARLeadnews（★0）与 C4 mogu_blog_v2（★1813）— 微服务路线实证

ARLeadnews 为 Spring Cloud Alibaba 全套（Gateway + 多服务 + Feign + MinIO + 阿里云内容安全 + XXL-Job + 自媒体端/管理端/App 端三前端）；mogu 为 Spring Boot 2.2.2 + Spring Cloud Alibaba Hoxton 的 12 模块微服务（含 zipkin、monitor、search、spider、picture、sms）。

**可学：** 内容安全（文本/图片审核）的第三方接入形态，可作为二期内容合规的备选；`ArticleConstants`/`WmNewsMessageConstants` 的常量集中管理。

**明确不可照搬：** 微服务拆分与 GameBoi 的 D08（独立 Compose、模块化单体）直接冲突；两项目均为 0 星/停更边缘，且整体资源占用远超 4GB 预算。

### A9 sunflower0305/news（★3）— NewsPub：直接对标但派生自 GPL 项目

**关键实现证据：** README 明确"**无头 CMS** 架构，提供 `模板 + 标签` 与 `Vue(React) + API` 两种开发模式"；后台 `/cp/`，Swagger UI，Freemarker 前台模板，Lucene 全文检索 + 相关文章，修改域名后需"更新全部索引"；pom 中 Spring Boot 2.7.18 + Lucene 8.11.1 + Liquibase + Flowable 6.8.1 + ES + Quartz + MyBatis + `spring-boot-starter-mobile`。

**可学：** "双渲染模式"（模板渲染 + 无头 API）是 GameBoi 二期"站内完整阅读"的一种低成本形态；索引随域名变更需重建，说明**索引中不应存绝对 URL**（对 GameBoi 未来 Lucene/ES 索引设计是直接警示）。

**许可证风险：** 代码结构与类名与 ujcms（GPL-3.0）高度一致，但仓库声明 Apache-2.0——**声明与来源不一致**，任何代码借鉴都不安全。

### B1 halo（★39940）— Spring Boot 4 同期最强参照

**关键实现证据：** `gradle/libs.versions.toml`：`spring-boot = 'org.springframework.boot:4.1.1'`、`lucene = '10.5.0'`、`jsoup 1.23.1`、`r2dbc-migrate-spring-boot-starter 4.1.0`、`springdoc-openapi-starter-webflux-ui 3.1.0`；`settings.gradle` include `api / application / platform:application / platform:plugin / ui`；核心 extension 模型：`Plugin`/`Theme`/`Role`/`RoleBinding`/`Setting`/`Menu`/`Snapshot`/`Post`/`Comment`/`Attachment`/`Policy`/`Notifier`/`ReverseProxy`；`PostContentService`/`ExcerptGenerator`/`PatchUtils`（内容摘要与差异补丁）。

**可学：**
1. **Role + RoleBinding 分离的 RBAC 模型**——直接回应 §8"管理员身份不能由注册/更新资料载荷设置"，比单列 role 更难被越权写入。
2. `Snapshot`（内容修订快照）对应 §4.3 的"候选修订"，可作为修订表设计的字段参照。
3. `api` 模块只放接口与扩展点、`application` 放实现的分层，与 §3.2 的模块边界一致。
4. **重要事实：Lucene 10.5 需要 Java 21+**。GameBoi 编译目标是 17 → 二期若用 Lucene 只能选 8.11.x（ujcms）或 9.x（PublicCMS 用 9.12 配 Java 17），**不能选 Lucene 10**。这是一个可直接落地的兼容性结论。

**不可照搬：** 插件/主题/R2DBC 响应式体系；GPL-3.0，不可复制代码。

### B2 PublicCMS（★2093）— Spring Boot 4.1.1 的另一处实证

**关键实现证据：** `publiccms-parent/pom.xml`：`version.springBoot = 4.1.1`、`version.spring = 7.0.9`、`java.version = 17`、Lucene 9.12.3、Quartz 2.5.2、MyBatis 3.5.19 + 4.1.0、Hibernate Search + Elasticsearch backend；模块划分 `publiccms-core / cache / common / oauth / oss / trade / analyzer`；README 声明"轻松支撑上千万数据、千万 PV"，支持**全站静态化、SSI、动态页面局部静态化、URL 规则自定义**。

**可学：** ①Spring Boot 4.1.1 + Spring 7.0.9 与 JDK 17 组合是可行的（对本项目编译目标 17 是正面证据）；②"动态页面局部静态化"应对突发流量的思路；③模块划分方式。

**不可照搬：** 静态化 + SSI 需要前置 Nginx 与写权限管理，与"容器根文件系统尽可能只读"（§9.2）冲突；授权协议非标准开源（V4/V5/V6 商用付费）。

### B3 diyhi/bbs（★1086）— Spring Boot 4.0.3 + JDK21 的社区系统

**关键实现证据：** README："JDK21 + Spring Boot 4.x + JPA + Ehcache + Lucene"，"针对高频访问数据表引入**分表存储策略**"，"禁止在商业上使用"；源码中 `component/filterWord/`（`SensitiveWordFilterComponent`、`RiskSensitiveWordsComponent`、`StopChar`、`Words`）、`component/lucene/`（Topic/Question 的 Index + Lucene Component + IndexCacheManager）、`component/like/`、`component/favorite/`、`component/follow/`、`component/mediaProcess/MediaProcessQueueCacheManager`、`component/accessSourceDevice`、`annotation/RoleAnnotation` + `DynamicRouteRegistration`。

**可学：** ①敏感词/风控词组件的分层（词库 → 过滤器 → 业务组件）；②"组件 = 缓存管理器 + 业务组件"的成对模式（对 GameBoi 的 Redis 缓存封装有参考价值，避免缓存 key 散落）；③`RoleAnnotation` 注解式角色校验。

**不可照搬：** 分表（数据量与预算不匹配，属过度设计）；Ehcache 作为主缓存（GameBoi 已定 Redis）；AGPL + 禁止商用。

### B4 ujcms（★667，SB 3.5.16）— 内容模型与状态机的最佳参照

**关键实现证据（`Article.java`）：**

```
STATUS_PUBLISHED = 0   STATUS_ARCHIVED = 1   STATUS_READY = 5
STATUS_DRAFT = 10      STATUS_PENDING = 11   STATUS_REVIEWING = 12
STATUS_DELETED = 20    STATUS_OFFLINE = 21   STATUS_REJECTED = 22

INPUT_TYPE_NORMAL = 0  INPUT_TYPE_CONTRIBUTE = 1  INPUT_TYPE_COLLECTION = 2
INPUT_TYPE_INTERFACE = 3  INPUT_TYPE_INTERNAL_PUSH = 4  INPUT_TYPE_EXTERNAL_PUSH = 5

adjustStatus(Short): READY + hasPublishPermission → PUBLISHED，否则 OFFLINE
                     PUBLISHED/ARCHIVED → 无权限时回 READY
```
另有 `ArticleExt`（扩展字段）、`ArticleBuffer`/`ChannelBuffer`/`SiteBuffer`（缓冲/统计）、`ArticleTag`、`customs`（自定义字段 Map）、`src`（原稿引用）、`Org`/`Role`/`RoleArticle`/`RoleChannel`/`RoleOrg`（组织与角色级数据权限）、`SensitiveWord`、`ErrorWord`、`OperationLog`/`OperationLogExt`、`ScheduleTask`、`LoginLog`。模块：`ujcms-cms / common / starter / code-generator / ui-console`，安全用 **OAuth2 Resource Server + OAuth2 Client**，检索 Lucene 8.11.4 与 Spring Data Elasticsearch 并存，调度 Quartz，流程 Flowable，迁移 Liquibase，另有 `compose.yaml` + `Dockerfile` + `upgrade/` 目录。

**可学（本项目 §4 的最大收益点）：**
1. **显式 READY 中间态**：把"内容已就绪"与"已发布"分离，能干净地表达"审核通过但未到发布时间/无发布权限时回退"。
2. **INPUT_TYPE 概念**：等价于 §4.4 `article.created_by` + `source_id` 可空的语义（管理员原创 vs 采集入库 vs 接口推送），比只靠 `source_id IS NULL` 更明确、更可审计。
3. **集中式 `adjustStatus()`**：把所有状态转移规则收在一个方法里校验，替代散落在各 Service 的 `if (status == X)`。
4. OAuth2 Resource Server 作为 JWT 校验的实现方式（本项目的 jjwt 0.11.5 属较旧方案，可评估但不必急换）。
5. `upgrade/` 目录 + Liquibase 的升级组织方式（对应本项目 `sql/` 与 Flyway 的目录规范）。

**不可照搬：** GPL-3.0；Flowable 工作流引擎、组织树、ES + Lucene 双索引均超出首期范围。

### B5 itranswarp（★796，SB 3.0.0）— 全文检索的轻量替代

**关键实现证据：** pom 依赖 `com.redis:lettucemod`（**RediSearch**）、`jcseg`（中文分词）、`commonmark` 系列、`pebble` 模板、自研 `warpdb`、`webauthn4j`、`web3j`；README："Search support / OAuth2 integration / SEO support / REST API"；环境要求 **Redis 5/6 + RediSearch 2**；`markdown/plugin/` 内置 Bilibili/QQVideo/YouTube/TradingView 链接插件；`SettingInput` 注解 + `SettingDefinition` 驱动的设置系统；`I18NBuilder`、`SchemaBuilder`、`MergeResources`。

**可学：** RediSearch 作为"复用已装 Redis"的全文检索方案——对"不新增常驻服务"的约束是一个可评估选项（但 RediSearch 需要 Redis Stack/模块，与 §9.3 锁定的 Redis 7 镜像未必兼容，必须实测）。

**不可照搬：** GPL-3.0；自研 WarpDB；Markdown 插件体系（GameBoi 走摘要模式，不需要编辑器体系）。

### B6 wallride（★97，已停更）— 多语言内容模型

**关键实现证据：** 域模型 `Blog` / `BlogLanguage` / `Post` / `Page` / `Article` / `Category` / `Tag` / `CustomField` / `CustomFieldOption` / `CustomFieldValue` / `Seo` / `GoogleAnalytics` / `PopularPost` / `NavigationItem`(+Category/Page) / `Media` / `PasswordResetToken` / `UserInvitation`；自动配置套件 `WallRideAutoConfiguration` + 十余个 `*Configuration`（Cache/Jpa/Job/Mail/Security/Servlet/Thymeleaf/WebMvc/Schedule/MessageSource）与 `WebAdminComponentScanRegistrar`/`WebGuestComponentScanRegistrar`（同一应用内区分后台/访客上下文）；缓存用 Infinispan；`PhysicalNamingStrategySnakeCaseImpl` 统一命名策略。

**可学：** ①`BlogLanguage` 作为**语言维度表**而非字段，是 D05（中英混合内容）将来扩展翻译时的正确建模方式；②`CustomField` 三表（定义/选项/值）表达"每来源不同字段"；③"同一应用内按上下文（管理端/访客端）拆组件扫描"的思路。

**不可照搬：** 2019 年停更、Spring Boot 1.x 时代、Thymeleaf + AngularJS 混用、JDK 8。

### B7 gentics/mesh（★593）— headless 内容 API 的工业级形态

**关键实现证据：** 目录结构 `api / core / common / common-api / connectors(api,common,hsqldb,mariadb) / elasticsearch / mdm / plugin-api / plugin-bom / plugin-dep / plugin-parent / rest-client / rest-model / services(aws-s3-storage,image-imgscalr) / distributed / distributed-coordinator`；`MeshOptions` 配置体系含 `DiskQuotaOptions`（**磁盘配额**）、`ClusterOptions`、`MonitoringConfig`、`ContentConfig`、`GraphQLOptions`、`S3Options`、`ImageManipulationMode`/`ResampleFilter`；`RestAPIVersion`、`MeshStatus`、`PluginManifest`；集成测试与性能测试均有独立 `docker-compose.yml`。

**可学：** ①`DiskQuotaOptions` 是本次调研中唯一另一个"磁盘配额"概念（GameBoi §11 可以对照其配置项命名与语义）；②`plugin-bom` / `plugin-parent` / `plugin-dep` 三级依赖管理，是插件体系的可选演进形态；③独立的 `integration-tests` / `performance-tests` 模块 + compose（对应 GameBoi §13.2 的受控负载测试）。

**不可照搬：** Vert.x 而非 Spring；ES 为硬依赖；集群/分布式形态远超预算。

### C1 novel（★5831，SB 3.3.0）— Spring Boot 3 生产级工程样板

**关键实现证据（目录）：** `core/annotation/{Key,Lock,ValidateSortOrder}` + `core/aspect/{LockAspect,SortOrderValidationAspect}`；`core/auth/{AuthStrategy,AdminAuthStrategy,AuthorAuthStrategy,FrontAuthStrategy,UserHolder}`（多套认证策略并存）；`core/interceptor/{AuthInterceptor,TokenParseInterceptor,FlowLimitInterceptor,FileInterceptor}`；`core/config/{AiConfig,AmqpConfig,CacheConfig,CorsConfig,EsConfig,LogbookConfig,MybatisPlusConfig,ShardingSphereConfiguration,XxlJobConfig}`；`core/filter/XssFilter` + `XssProperties`；`core/json/deserializer/GlobalJsonDeserializer`、`serializer/UsernameSerializer`；`core/util/JwtUtils`；`core/task/BookToEsTask`；`core/listener/RabbitQueueListener`；控制器按 `front / author / admin` 分端。依赖含 Spring Boot 3.3.0、JDK 21、MyBatis-Plus、ShardingSphere 5.5.1、XXL-Job 2.3.1、Redisson、RabbitMQ、ES、**Spring AI 1.0.0**、Flyway、Spring Boot Admin。

**可学：**
1. `@Key` + `LockAspect` 注解式分布式锁（对应 GameBoi 的"任务互斥"）。
2. `FlowLimitInterceptor`（限流拦截器）+ `XssFilter` + `GlobalJsonDeserializer` 的组合，是一套可直接对照的安全/质量拦截链路。
3. `AuthStrategy` 多策略模式处理"前台用户 / 作者 / 管理员"三套认证——GameBoi 的三级角色未来扩展时可用。
4. `BookToEsTask` + `RabbitQueueListener` 展示"DB 变更 → 消息 → 索引"的最终一致模式（二期检索可用）。

**不可照搬：** ShardingSphere 分库分表 + XXL-Job + ES + RabbitMQ + Redisson 全套，对 4GB/10GB 预算是灾难（单 ES 就超预算）；Apache-2.0 可参考但引入前仍需逐项评估。

### C2 NBlog（★2698）— 注解式审计

**关键实现证据：** `annotation/{AccessLimit,OperationLogger,VisitLogger}` + `aspect/{OperationLogAspect,VisitLogAspect,ExceptionLogAspect}`；`config/{JwtFilter,JwtLoginFilter,MyAuthenticationEntryPoint,SecurityConfig}`；`constant/{CommentConstants,JwtConstants,PageConstants,RedisKeyConstants,SiteSettingConstants,UploadConstants}`；`controller/admin/*` 与前台控制器分层；`properties/{BlogProperties,GithubProperties,ProxyProperties,TelegramProperties,UploadProperties,UpyunProperties}`（多存储后端）；`yauaa` 解析 UserAgent + `ip2region` 离线 IP 库。

**可学：** 把 `moderation_audit` 的写入从 Service 内散落调用改为 `@OperationLogger` 注解 + AOP，可以显著减少样板代码并保证"每个管理动作都有审计"。
**不可照搬：** Spring Boot 2.2 / JDK 8 / Vue2；`RedisKeyConstants` 单类集中管理 key 的做法可以借，但常量类不应成为隐式契约。

### C3 lin-cms（★953）/ C4 mogu_blog_v2（★1813）

- lin-cms：`common/configuration/{CommonConfiguration,WebConfiguration}`、`common/interceptor/{AuthorizeVerifyResolverImpl,RequestLogInterceptor}`、`bo/GroupPermissionBO`、`dto/admin/{DispatchPermissionDTO,RemovePermissionsDTO}`、`model` 与 `mapper` 命名（UserGroup/UserIdentity/GroupPermission/Permission/Log）→ **分组-权限-身份三元模型**，可作为"角色之外还要分组"的参考。SB 2.5.2 / JDK 8，MIT。
- mogu：12 模块微服务 + `AuthorityVerify`/`AvoidRepeatableCommit`/`OperationLogger` 三注解 + `RabbitMqConfig`/`DruidConfig` + `mogu_search`/`mogu_spider`/`mogu_picture` 独立服务 → **反向参照**：`@AvoidRepeatableCommit`（防重复提交）思路可单独借鉴，整体架构不可。

---

## 6. 横向结论（八条）

1. **"Spring Boot 3+ 的成熟资讯聚合平台"这个品类基本不存在。** 高星项目要么是重型 CMS（halo 40k、PublicCMS 2k、ujcms 667），要么是低星学生项目（本次 A 组的 RssHub/briefly/fact-feed/newsflow 均为 0 星）。真正的聚合器标杆 commafeed 用的是 Quarkus。**含义**：GameBoi 找不到可以直接抄的"同类完整架构"，只能按机制逐个吸收；同时这也意味着 GameBoi 的设计选择没有现成的错误参照系，必须靠自己验证。

2. **技术栈罕见度：** Spring Boot 4.x 项目仅 5 个（halo 4.1.1、PublicCMS 4.1.1、diyhi/bbs 4.0.3、newsflow 4.0.6、GameBoi 4.0.5）。好消息是 **PublicCMS 实证了"Spring Boot 4.1.1 + Spring 7.0.9 + JDK 17"可行**，与本项目的编译目标一致；坏消息是 4.x 上可参考的完整业务代码极少，遇到问题只能查官方文档与源码。

3. **GameBoi 的可靠性设计在本批次中是断层领先的。** 18 个项目里，没有任何一个实现了"持久 outbox + 端到端幂等 + receipt 重放"；最接近的 briefly 只有 URL 哈希唯一约束，RssHub 用 IN 查询去重，newsflow 用缓存去重（缓存丢失即重复）。**建议把这一点固定为项目的核心差异化，不要为了"对齐主流"而削弱。**

4. **资源预算治理同样是孤例。** 唯一有可比性的是 commafeed README 的 JVM 调优段落与 mesh 的 `DiskQuotaOptions`。ARM/`mesh` 属于重型 headless CMS，commafeed 是无预算约束的自托管工具。**含义**：§10/§11 的预算体系没有现成模板，M6 的资源实测必须自己设计测量方法（这正是 OPS-01 仍 in_progress 的原因）。

5. **采集侧最值得吸收的三项机制（全部来自 commafeed）：** 条件请求 + 内容指纹的三重 NotModified 判定；自适应刷新间隔（含 errorCount 退避与 Retry-After 取 max）；"可丢弃任务 / 不可丢弃任务"的背压分层。

6. **内容治理侧最值得吸收的是 ujcms 的状态机。** 显式 READY 中间态 + INPUT_TYPE 来源类型 + 集中式 `adjustStatus()` 校验，正好补强本项目 §4.3/§4.4 中相对单薄的部分（当前 DRAFT/PENDING_REVIEW/PUBLISHED/REJECTED/UNPUBLISHED 缺少"已就绪待发布"的表达，原创/采集的区分只能靠 `source_id IS NULL`）。

7. **AI 与可观测侧最值得吸收的是 morningdeck：** 两级模型配置（lite/heavy）+ 可切换 OpenAI 兼容 base_url + Actuator 自定义 `HealthIndicator` + "全文检索可选依赖"。这四项都不引入新服务，完全落在 §13.3 的"轻量观测"边界内。**注意其 AGPL-3.0，只能学做法不能抄代码。**

8. **三条明确的反模式，本批次给出了实证：** ①低星项目普遍在采集层犯错（IN 列表去重、缓存去重、整源 catch-all 标记失败、无预算、启动即抓）；②重型 CMS 普遍过度设计（ELK/微服务/工作流引擎/分库分表/插件体系）；③所有"用 headless browser 抓取"的项目（fact-feed）都付出了常驻 Chrome 的内存代价——这直接印证了 §5.2 排除常驻 headless browser 的决策。

---

## 7. 对 GameBoi 的后续开发建议

> 每条给出：现状 → 参考来源 → 建议动作 → 成本/风险 → 与设计基线的关系。**所有建议均为"设计思路借鉴"，不包含任何代码复制。**

### 7.1 P0 — 当前阶段（M4/M5）即可落地，低成本高收益

| # | 建议 | 现状与依据 | 参考 | 建议动作 | 成本 / 风险 | 与基线关系 |
|---|---|---|---|---|---|---|
| P0-1 | **采集端接入条件请求与内容指纹缓存** | `ingest/spiders/base.py` 目前每次请求来源；§5.2 已规定 5s/30s 超时与重试，但未见条件请求 | commafeed `FeedFetcher`（ETag + Last-Modified + sha1 content hash + lastPublishedDate 四重判定） | 在适配器基类的状态存储（`ingest/runtime/state.py`）中保存每个源/URL 的 ETag、Last-Modified、content_hash；请求时带 `If-None-Match` / `If-Modified-Since`，304 直接跳过 | 低。需注意状态存储纳入 50MB outbox/state 预算 | 强化 D10 低频采集；不违反任何 D 项 |
| P0-2 | **引入显式 READY 中间态并集中状态转移校验** | `PROJECT_DESIGN.md` §4.3 现有 5 个状态；feature_list 中 ADMIN-01 的 approve→audit→readback 已实测 | ujcms `Article.adjustStatus()` + `STATUS_READY` | 新增 `READY`（内容就绪待发布，可由管理员或规则置位）；把状态转移规则收进单一方法并加测试；不得绕过统一发布服务 | 低–中。需要一次 Flyway 迁移 + 状态枚举变更 + 事务内审计；**属于 §4.3 语义扩展，必须先更新 PROJECT_DESIGN.md 并记录原因** | 补强 §4.3；不改变公开可见性边界 |
| P0-3 | **增加内容来源类型字段** | §4.4 `article.created_by` + `source_id` 可空；P09 曾用硬编码 `createUser=1` | ujcms `INPUT_TYPE_NORMAL/CONTRIBUTE/COLLECTION/INTERFACE/INTERNAL_PUSH/EXTERNAL_PUSH` | 首期只需三值：`ADMIN_ORIGINAL` / `COLLECTED` / `INTERFACE_PUSH`；用于审核页展示与审计，不改变授权判断 | 低。一个 nullable 列 + 默认值回填 | 直接对应 P09 的"不硬编码用户 ID"；强化 §4.4 |
| P0-4 | **用 Actuator 自定义健康指标暴露采集与容量状态** | §13.3 要求轻量观测，但当前依赖结构化日志 + 运维页 | morningdeck `FeedIngestionHealthIndicator` / `NewsProcessingHealthIndicator` | 实现 2–3 个 `HealthIndicator`：采集管线最近一次成功时间与积压、outbox 待交付年龄与体积、容量状态机当前档位；全部 fail-closed（UNKNOWN 即 DOWN） | 低。注意健康接口不得暴露密钥/内部路径，且需按 §13.3 限定为"受限健康接口" | 强化 §13.3，避免引入监控栈 |
| P0-5 | **AI 调用用量落表** | §13.3 要求记录"AI 请求/耗时/实际计费使用量（可取到时）"；§5.2 设了日调用上限 240 | fact-feed `ApiUsageLog` 实体 + `ApiUsageMonitoringService` | 新建 `ai_usage_log`（provider/model/用途/输入输出字符数/token（可取时）/耗时/结果/时间），写入走 outbox 同一事务或独立异步写；不记录提示词全文 | 低–中。需与 B04 的供应商确认后再定字段 | 直接落实 §13.3 |
| P0-6 | **模型分层与 base_url 配置化** | `ingest/agent/deepseek_client.py` + B04"不沿用代码中未验证的模型名" | morningdeck `OPENAI_MODEL_LITE` / `OPENAI_MODEL_HEAVY` / `OPENAI_BASE_URL` | 拆出"摘要模型/分类模型"两个配置项（首期可同值），base_url 与 model 全部走环境变量；无密钥/无配置时 fail-closed 保持 PENDING_REVIEW | 低 | 直接落实 B04 与 XHH-01 的 fail-closed 要求 |

### 7.2 P1 — M5/M6 阶段（配合 SOURCE-01 / DEPLOY-01）

| # | 建议 | 现状与依据 | 参考 | 建议动作 | 成本 / 风险 | 与基线关系 |
|---|---|---|---|---|---|---|
| P1-1 | **来源级自适应抓取间隔** | §5.2 固定 6/12/24 小时三档 | commafeed `FeedRefreshIntervalCalculator`（发布越频繁抓取越频繁、≥7/14/30 天阶梯拉长、失败按 errorCount 线性退避、429 取 max(retryAfter, backoff)、结果夹在 [min,max]） | **保留**现有固定周期与每日预算作为上界，新增来源级"下次可抓取时间"计算；429/失败沿用已有 `delivery_control`/退避记录并纳入计算 | 中。必须保持可解释、可审计——不要引入学习型/黑盒算法；每源 interval 需在管理页可见 | 强化 D10；不改每日 80 候选预算与 240 次 AI 上限 |
| P1-2 | **抽取策略加入 JSON-LD 优先** | §5.1 目前是"官方结构化接口/RSS → 单游戏栏目 → 媒体补充"，抽取用选择器 + 待评测 Trafilatura | fact-feed `ArticleExtractor` 优先解析 `application/ld+json` | 适配器抽取顺序调整为：来源结构化接口/RSS → 页面 JSON-LD（headline/datePublished/author/image）→ 专用选择器 → Trafilatura | 中低。JSON-LD 可能缺失或字段不规范，必须保留回退路径并加离线 fixture 测试 | 具体化 §5.1；不替换现有适配器 |
| P1-3 | **处理链显式化（handler 链）** | `ingest/filters/editorial.py`、`fps_rules.py`、去重逻辑分散在 pipeline 与 outbox | hot-crawler `HandlerCenter` + `DataHandler` 链 | 把"规范化 URL → 元数据初筛 → 已交付检查 → 白名单/类型判定 → 去重"表达为有序、可单独单测的步骤列表；**用 Python 组合函数/协议，不用继承树** | 低–中。纯内部重构，但**必须避免一次性大搬包**（§3.2 明令）——按触及即收敛的原则做 | 符合 §3.2/§5.1；不改行为 |
| P1-4 | **适配器端口契约显式化** | §5.1 已要求"适配器统一输出 RawArticle"，但缺少强制形式 | briefly `ContentSourcePort`（+ 注册为 Bean 自动注入） | 为 `ingest/spiders/*` 定义显式协议（Python `Protocol`/ABC）+ 强制返回 `RawArticle` 的校验；新适配器只实现该协议，零改管线 | 低 | 落实 §3.2「Python adapters 只做发现与抽取」 |
| P1-5 | **审计写入改为注解 + AOP** | ADMIN-01 目前在 Service 内显式写 `moderation_audit`（含 actor ID 校验） | NBlog `@OperationLogger` + `OperationLogAspect` | 定义 `@ModerationAudit(action, reason)` 注解，AOP 统一落库；**保留**现有的"必须先校验有效 userId 才能写审计"的硬约束 | 低。注意 AOP 与事务顺序：审计必须在同一事务、且失败要回滚（现有测试已覆盖审计插入失败回滚） | 强化 §4.4 moderation_audit 与 ADMIN-01 验收 |
| P1-6 | **管理端"来源最近内容"用窗口函数查询** | 管理端来源/运行页需展示每源最近若干条 | briefly `ArticleJpaRepository.findLatestOfEach`（`ROW_NUMBER() OVER (PARTITION BY source …)`） | 为管理端新增一个只读 Mapper 方法（每源最新 N 条），避免 N+1；仅管理端，不进公开 DTO | 低。MySQL 8 支持窗口函数；需回归索引使用 | 支撑 ADMIN-01 §7.2 的 `/admin/sources`、`/ingest/runs` |
| P1-7 | **优雅停机校验** | §5.2 要求"单轮任务 30 分钟内停止前保存进度" | fact-feed `@PreDestroy` 线程池优雅关闭；commafeed `stop()` 对 6 个执行器逐一 `shutdownAndAwaitTermination` | 核对 `ingest/main.py` 与 `runtime/` 的 SIGTERM 处理：必须在收到终止信号后停止取新候选、保存游标、提交已就绪 outbox | 低 | 落实 §5.2 + §6.1 |

### 7.3 P2 — 二期或条件触发（不要现在做）

| # | 建议 | 触发条件 | 参考 | 关键约束 |
|---|---|---|---|---|
| P2-1 | **全文检索选型：Lucene 9.x 或 RediSearch，不用 ES 集群** | §1.3 明确指出"暂不做全文搜索集群"；D06 二期站内完整阅读触发 | ujcms（Lucene 8.11.4 + 双索引）、PublicCMS（Lucene 9.12）、itranswarp（RediSearch）、halo（Lucene 10.5） | **硬约束：Lucene 10.x 需要 Java 21+，本项目编译目标 17 不可用**；若用 Lucene 只能 8.11.x/9.x。RediSearch 需 Redis Stack，与锁定镜像的兼容性必须实测。ES/ELK 明确排除（内存预算） |
| P2-2 | **索引中不存绝对 URL** | 同上 | sunflower0305 README："修改域名后需更新全部索引，否则全文检索与相关文章的 url 仍为原域名" | 索引只存 ID + 相对路径，展示时拼接 base URL |
| P2-3 | **多语言内容模型（语言维度表）** | D05 扩展到全文翻译时 | wallride `BlogLanguage` 维度表 + `CustomField` 三表 | 首期已有 `source_language`/`original_title`，不要提前建表 |
| P2-4 | **RBAC 改为 Role + RoleBinding 分离** | 角色数量增长到需要"临时授权/多角色绑定"时 | halo `Role` / `RoleBinding` | 当前三级角色足够，提前改造属过度设计 |
| P2-5 | **内容修订快照表** | §4.3"已发布文章的抓取更新先创建候选修订"进入实现时 | halo `Snapshot`、ujcms `ArticleBuffer`/`src` | 首期不静默覆盖线上内容的要求已明确，实现时再建表 |
| P2-6 | **独立集成/性能测试模块 + compose** | §13.2 受控负载测试启动时 | mesh `integration-tests/` `performance-tests/` + 各自 compose；briefly Testcontainers | 必须在隔离环境，不得在生产主机压测（§13.2 已规定） |
| P2-7 | **JWT 方案评估** | 下次重大安全评审时 | ujcms（OAuth2 Resource Server） | 现状 jjwt 0.11.5 较旧；更换属大改动，需要单独任务与回滚方案，不顺手做 |

### 7.4 明确不建议做（反向清单，含实证）

| 反模式 | 实证来源 | 理由 |
|---|---|---|
| 迁移到 Quarkus / 微服务拆分 | commafeed（Quarkus）、ARLeadnews / mogu（Spring Cloud） | 违反 D08 独立 Compose + 模块化单体；微服务项目整体资源占用远超 4GB |
| 引入常驻 headless browser（Selenium/Playwright 常驻） | fact-feed | §5.2 已排除；fact-feed 的 Chrome + 10 线程池是内存代价实证 |
| 用缓存/RDIS 做去重依据 | newsflow（`NewsCacheManager`） | 缓存失效即重复入库；GameBoi 的唯一约束 + content_hash 更可靠 |
| 用 IN 列表批量查重 | RssHub（`findExistingLinks`） | 链接规模上升后退化为大 SQL；无法处理同 URL 内容变更 |
| 整源 catch-all 后标记来源失败 | RssHub（`source.setStatus("error")`） | 一次网络抖动污染来源可用性；GameBoi 已有阶段化状态与 QUARANTINED 语义 |
| 启动即抓 + 无限并发 | fact-feed（`app.startup.auto-scrape=true`）、commafeed（多线程池并发抓取） | 违反 §5.2"全项目一个采集任务、一个详情处理、一个 LLM 并发" |
| 多 AI Key 轮换 | fact-feed（6 keys） | 超出 D09 低量预算，且放大密钥管理风险 |
| 自动建表 / Hibernate ddl-auto | fact-feed | 与 DATA-01 的"版本化迁移是唯一基线"冲突 |
| 全量清空缓存 | newsflow（`@CacheEvict(allEntries=true)`） | 缓存击穿；应做 TTL + 有界 key（§10.1 已规定） |
| 分库分表 / ShardingSphere / XXL-Job / ES / RabbitMQ 全家桶 | novel、mogu、diyhi/bbs | 数据规模与 4GB/10GB 预算完全不匹配 |
| 全站静态化 + SSI | PublicCMS | 与"容器根文件系统尽可能只读"（§9.2）冲突，且需前置 Nginx 写权限 |
| 插件/主题体系 | halo、mesh | 首期无此需求，属架构过度设计 |
| 事件聚类 / 跨源冲突检测 / 内容创作 | fact-feed、morningdeck | 二期以后，且需要远高于当前的 AI 预算 |

---

## 8. 许可证与合规风险

| 风险级别 | 项目 | 许可证 | 对本项目的含义 |
|---|---|---|---|
| 🔴 高 | halo、ujcms、itranswarp | GPL-3.0 | 复制任何代码片段都可能使本项目受 GPL 约束（尤其若本项目未来对外分发）。**只读设计思想，不复制代码** |
| 🔴 高 | morningdeck、diyhi/bbs | AGPL-3.0 | 网络服务条款更严；diyhi/bbs 官网另声明"禁止商业使用" |
| 🔴 高 | PublicCMS | V4/V5/V6 个人免费、企事业单位付费 | 非标准开源许可，商用需付费；V2021 才是 MIT |
| 🟠 中 | fact-feed、DreamTes/RssHub | **无 LICENSE 文件** | 默认保留所有权利，**不可复制任何代码**（RssHub 的 README 自称 MIT 但无许可文件，不具备法律效力） |
| 🟠 中 | sunflower0305/news | 声明 Apache-2.0，但代码结构与 ujcms（GPL-3.0）高度一致 | 许可声明与来源不一致，**不可借鉴代码** |
| 🟢 低 | commafeed、novel、mogu、wallride、mesh、ARLeadnews | Apache-2.0 | 可参考，保留声明即可；仍需逐文件核对 |
| 🟢 低 | briefly、NBlog、lin-cms、hot-crawler、newsflow | MIT | 最宽松，可参考 |

**一个必须先行解决的问题：GameBoi 自身尚未声明许可证**（`pom.xml` 中 `<licenses><license/></licenses>` 为空）。在任何代码借鉴、依赖引入或对外分发之前，应先确定本项目的许可策略（开源/闭源），否则无法判断"能否吸收 GPL 系代码"这一前置问题。

---

## 9. 本次调研未完成的事项与下一步

| 项 | 说明 |
|---|---|
| 未编译/未运行任何对标项目 | 所有对比基于静态代码阅读，不含运行时行为验证（尤其是 Spring Boot 4.x 上的实际表现） |
| 未核实依赖可用性 | 例如 Lucene 9.x 在 JDK 17 + Spring Boot 4 下的实际可用性、RediSearch 与锁定 Redis 镜像的兼容性，均需实测 |
| GitHub 未授权 API | 核心配额 60 次/小时，可能漏检部分高星仓库；建议后续用授权 token 复跑一次检索 |
| 未做量化评测 | 各项目的实际内存/磁盘占用未测量；§13.2 的负载目标需要自己在隔离环境实测 |
| 建议的下一步 | ①先决定 P0-1～P0-6 中哪几项纳入下一个任务（建议 P0-4 与 P0-6 成本最低、P0-2 收益最大）；②P0-2/P0-3 涉及 §4.3/§4.4 语义扩展，需先更新 `PROJECT_DESIGN.md` 并记录变更原因；③把本报告并入 §15 的开源参考表（需用户确认后再改 `PROJECT_DESIGN.md`） |

---

## 附录 A：检索关键词与候选漏斗

| 阶段 | 内容 |
|---|---|
| 第一轮关键词（16 组） | `spring boot news aggregator` / `news aggregator spring boot 3` / `rss reader spring boot` / `spring boot news crawler` / `springboot 资讯 聚合` / `spring boot 新闻 采集` / `spring ai news summary` / `topic:news-aggregator language:Java` / `topic:rss-reader language:Java` / `topic:spring-boot topic:cms` / `headless cms spring boot` / `content aggregation platform spring boot` / `spring boot 3 blog system` / `news portal java spring` / `webmagic spring boot crawler` / `topic:aggregator language:Java` |
| 第二轮关键词（20 组） | `topic:cms language:Java stars:>500` / `topic:blog language:Java stars:>300` / `topic:news language:Java stars:>100` / `spring boot 舆情` / `spring boot 头条 资讯` / `hot news springboot stars:>50` / `topic:news-aggregator` / `topic:rss` / `news aggregator self-hosted` / `spring boot feed reader` / `content management system spring boot stars:>200` / `topic:spring-boot topic:news` / `news crawler 采集 springboot stars:>20` / `spring boot 3 blog cms stars:>50` / `AI news summary spring boot 3` / `资讯聚合 系统 springboot stars:>10` / `topic:news-portal` / `news recommendation spring boot` / `舆情监控 springboot` / `topic:spider language:Java stars:>300` |
| 去重结果 | 第一轮 206 个唯一仓库，第二轮再补充；合计 400+ |
| 筛选标准 | ①Java/Kotlin 或 Spring 生态 ②提供资讯聚合、内容管理或订阅阅读语义 ③有可读源码（非纯前端/纯文档）④优先 Spring Boot 3+ |
| 剔除 | 纯 Android 客户端、纯爬虫工具库、无源码仓库、以及与本项目语义无关的高星仓库 |

## 附录 B：精读文件清单（证据索引）

| 仓库 | 精读文件 |
|---|---|
| commafeed | `pom.xml`、`commafeed-server/pom.xml`、`README.md`、`FeedRefreshEngine.java`、`FeedFetcher.java`、`FeedRefreshIntervalCalculator.java`、`FeedEntryContent.java`、`Digests.java` |
| morningdeck | `README.md`、`backend/pom.xml`、目录树（含 HealthIndicator/MeilisearchConfig/AsyncConfig） |
| RssHub | `pom.xml`、`README.md`、`RssService.java`、`RssParserService.java`、`ArticleRecommendServiceImpl.java`、`FetchTask.java` |
| briefly | `pom.xml`、`README.md`、`ContentSourcePort.java`、`FeedIngestionJob.java`、`ArticleJpaRepository.java` |
| fact-feed | `README.md`、`backend/build.gradle.kts`、`ArticleScrapingService.java`、`SummarizationService.java`、`EventMappingService.java` |
| newsflow | `Readme.md`、`backend/JavaNewsRss/pom.xml`、`NewsScheduledTasks.java`、`NewsCacheManager.java` |
| hot-crawler | `pom.xml`、`README.md`、`AbstractHotProcessor.java`、`DeduplicationDataHandler.java`、`HotProcessor.java` |
| ujcms | `README.md`、`pom.xml`、`ujcms-cms/pom.xml`、`Article.java`（域模型全量） |
| halo | `README.md`、`build.gradle`、`settings.gradle`、`gradle.properties`、`gradle/libs.versions.toml`、`platform/application/build.gradle`、目录树 |
| PublicCMS | `README.zh-CN.md`、`publiccms-parent/pom.xml`、目录树 |
| diyhi/bbs | `README.md`、`pom.xml`、目录树（components/annotation/cache/lucene） |
| sunflower0305/news | `README.md`、`pom.xml`、目录树 |
| itranswarp | `README.md`、`pom.xml`、目录树（bean/model/markdown 插件） |
| wallride | `README.md`、`pom.xml`、`Post.java`、目录树（domain/autoconfigure） |
| mesh | `README.md`、目录树（api/core/connectors/services/plugin-*/distributed） |
| novel | `README.md`、`pom.xml`、目录树（core/*） |
| NBlog | `README.md`、`blog-api/pom.xml`、目录树 |
| lin-cms / mogu / ARLeadnews | `README.md`、`pom.xml`、目录树 |

---

*报告结束。本文件为本次调研的唯一新增产物，未修改本项目任何既有代码、配置、迁移或设计文档。*
