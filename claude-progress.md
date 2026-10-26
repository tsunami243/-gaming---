# claude-progress.md

## 当前已验证状态

**仓库根目录：** 项目结构完整

**标准启动路径：**
- `./mvnw compile` → BUILD SUCCESS
- `./mvnw spring-boot:run` → 后端启动在 :8080，冒烟测试通过
- `cd gaming-news && npm install && npm run dev` → 前端启动在 :5173，Vite 代理正常
- 全栈集成：前端 :5173/api → 后端 :8080，HTTP 200 ✓

**标准验证路径：**
- `curl http://localhost:8080/article?pageNum=1&pageSize=5` → 200，返回 34 条数据（+5 from Session 002）
- `curl http://localhost:5173/api/article?pageNum=1&pageSize=3` → 200

**当前最高优先级未完成功能：**
- 见 feature_list.json（当前为空，无待实现功能）

**当前 blocker：**
- Python `python3` 路径损坏（`D:\python.exe` 不存在），需用 `python`（conda）替代，`init.sh` 会因 `set -e` 崩溃
- `@Scheduled` 消费者存在偶发不触发问题（Session 002 中 10+ 分钟未消费队列，之后又恢复正常）
- `ArticleIngestConsumer` 需 Redis 运行，但 Redis 未设置为开机自启
- 项目无单元测试（src/test 目录不存在）

**已解决的 blocker（Session 002）：**
- ✅ Redis 已安装并运行（winget install Redis.Redis 3.0.504）

---

## 会话记录

### Session 001 — 2026-07-04: 全项目运行验证

**日期：** 2026-07-04

**本轮目标：** 跑一遍整个项目，记录所有失败原因

**已完成：**

1. **环境检查**
   - Java 21.0.8 ✓ (要求 JDK 17+)
   - Node.js v24.15.0 ✓ (要求 18+)
   - Python 损坏 ✗ — `D:\python.exe` 文件不存在，`python3 --version` 报 `0x80070002`
   - MySQL ✓ — 可连接数据库 `big_event`
   - Redis ✗ — `redis-cli` 不在 PATH，`localhost:6379` 拒绝连接
   - node_modules ✓ — 已安装

2. **后端编译** → BUILD SUCCESS（所有类最新，无需重新编译）

3. **后端测试** → BUILD SUCCESS 但 "No tests to run"（`src/test/` 目录不存在）

4. **后端启动** → Spring Boot 在 2.28s 内启动成功，端口 8080

5. **前端启动** → Vite 开发服务器启动成功，端口 5173

6. **全栈集成** → Vite Proxy `/api` → `localhost:8080` 正常转发，HTTP 200

**运行过的验证：**
- `./mvnw compile` → BUILD SUCCESS
- `./mvnw test` → No tests to run
- `./mvnw spring-boot:run` → 启动成功
- `curl http://localhost:8080/article` → 200, 返回 29 条数据
- `npm install` → up to date
- `npx vite --host` → 启动成功
- `curl http://localhost:5173/` → 200
- `curl http://localhost:5173/api/article` → 200

**已记录证据：**

### 失败项明细：

| # | 失败项 | 根本原因 | 影响范围 | 修复方法 |
|---|---|---|---|---|
| 1 | **Redis 未启动** | Redis 服务未运行，`localhost:6379` 拒绝连接 | `ArticleIngestConsumer` 定时任务每 10s 抛 `RedisConnectionFailureException`；JWT Token 双验证的 Redis 部分不可用；爬虫文章入库队列不可用 | 启动 Redis 服务（Windows: `redis-server` 或在 WSL/Docker 中运行） |
| 2 | **Python 损坏** | `D:\python.exe` 文件不存在（已卸载或路径残留），`init.sh` 中 `python3 --version` 因 `set -e` 导致脚本退出 | 爬虫模块 (`ingest/`) 完全无法运行 | 重装 Python 3.11+ 或移除损坏的 PATH 条目，指向正确的 python.exe |
| 3 | **无单元测试** | `src/test/` 目录不存在，项目未编写任何测试用例 | `./mvnw test` 实际不验证任何逻辑；回归安全网缺失 | 按模块逐步添加测试（先覆盖 Controller 和 Service 层） |
| 4 | **Health endpoint 无响应** | 未引入 Spring Boot Actuator 或未配置 health 端点 | `/actuator/health` 返回空 | 按需添加 `spring-boot-starter-actuator` 依赖 |

### 通过项明细：

| # | 通过项 | 详情 |
|---|---|---|
| 1 | Java 环境 | `java version "21.0.8" 2025-07-15 LTS` |
| 2 | Node.js 环境 | `v24.15.0` |
| 3 | MySQL 数据库 | `big_event` 可连接，root@localhost:3306 |
| 4 | Maven 编译 | `./mvnw compile` — BUILD SUCCESS |
| 5 | 后端启动 | Spring Boot 4.0.5, Tomcat 11.0.20, 端口 8080, 启动耗时 2.28s |
| 6 | 文章 API | `/article?pageNum=1&pageSize=5` → 200, 29 条数据 |
| 7 | 前端依赖 | `node_modules` 已安装 |
| 8 | 前端启动 | Vite 8.0, 端口 5173 |
| 9 | API 代理 | Vite Proxy `/api` → `localhost:8080`, HTTP 200 |
| 10 | 数据库连接池 | HikariPool-1 正常启动 |

**提交记录：** 无（验证轮次，未产生代码变更）

**更新过的文件或工件：** claude-progress.md（本文件）

**已知风险或未解决问题：**
- Redis 是本项目核心依赖（JWT Token 存储 + 爬虫消息队列），未启动会导致认证和爬虫功能异常
- Python 损坏使爬虫模块完全不可用
- 无测试 = 任何改动没有安全网

**下一步最佳动作：**
1. 启动 Redis 服务（最高优先级 — 影响 JWT 和爬虫）
2. 修复 Python 环境或安装 Python 3.11+
3. 为关键 Service 和 Controller 补充单元测试

---

### Session 002 — 2026-07-04: 全项目运行 + 爬虫采集 + 文章入库

**日期：** 2026-07-04

**本轮目标：** 跑一遍整个项目，完成新文章的爬取和插入，与 Session 001 结果对比，记录失败点

**已完成：**

1. **Redis 安装与启动**
   - `winget install Redis.Redis` → 安装 Redis 3.0.504 for Windows ✓
   - `redis-server` 启动成功，端口 6379 ✓

2. **后端启动** → 端口 8080，无 Redis 错误（Session 001 的 RedisConnectionFailureException 已消除）

3. **前端启动** → 端口 5174（5173 被旧实例占用），代理正常

4. **爬虫全管线运行** → `python main.py`
   - 爬取 10 篇 → AI 处理 → 4 篇过滤（非游戏）→ 6 篇入 Redis 队列
   - 图片上传至 OSS ✓
   - DeepSeek V4 Flash API 调用正常 ✓

5. **文章入库**（手动触发 `/admin/ingest/consume`，因 @Scheduled 未触发）
   - 6 篇队列文章 → 5 篇有效入库，1 篇（空标题/内容）删除

6. **与 Session 001 对比**

| 指标 | Session 001 | Session 002 | 变化 |
|------|-------------|-------------|------|
| 文章总数 | 29 | 34 | +5 |
| Redis | 未运行 | 运行中 | ✅ 修复 |
| 爬虫能力 | 不可用 | 可用 | ✅ 修复 |
| 后端 Redis 错误 | 每 10s 报错 | 无错误 | ✅ 修复 |

**运行过的验证：**
- `./mvnw spring-boot:run` → 启动成功，无 Redis 错误
- `curl /article?pageNum=1&pageSize=3` → 200, 34 items
- `npx vite --host` → 启动成功，HTTP 200
- `curl localhost:5173/api/article` → proxy 200
- `python main.py` → 10 crawled, 6 enqueued, 4 filtered
- `redis-cli ping` → PONG
- `POST /admin/ingest/consume` → 5 篇有效入库

**本次新入库文章（ID）：** 821, 822, 879, 880, 882（均为草稿状态，category_id=NULL）

### 失败项明细（Session 002 新发现）：

| # | 失败项 | 根本原因 | 影响范围 | 修复方法 |
|---|---|---|---|---|
| 5 | **@Scheduled 消费者未触发** | 定时任务未在爬虫入队后自动 LPOP 消费（测试文章后来能被消费，疑似竞态/连接池初始化时序问题） | 爬虫入队的文章无法自动入库，需手动调 `/admin/ingest/consume` | 重启后端或等待连接池恢复；长期方案：添加健康检查重连 + `TaskScheduler` 线程池配置 |
| 6 | **DeepSeek AI category_id 始终为 0** | 模型返回的 `category_id` 全部为 0，AI 未按 CATEGORY_MAP 正确分类 | 所有采集文章的 `category_id` 为 NULL，无法按分类筛选 | 优化 prompt 中的 CATEGORY_MAP 描述，或在 ingest 管线中添加确定性分类规则 |
| 7 | **AI 误将体育新闻分类为游戏** | "姆巴佩能否超梅西"文章 confidence=10% 但仍 `is_gaming=True` | 1 篇体育新闻入库（标题和内容为空，已删除） | 提高 `is_gaming` 阈值或添加二级过滤（如对低 confidence 文章标记为待审核） |
| 8 | **Python `python3` 路径损坏** | `D:\python.exe` 不存在，但 conda 的 `python` 可用。`init.sh` 使用 `python3 --version` 触发 `set -e` 崩溃 | `init.sh` 无法完整运行；`python3` 命令不可用 | 修复 PATH 中损坏的 `D:\python.exe` 条目，或修改 `init.sh` 改为检查 `python`（不是 `python3`）|
| 9 | **旧进程端口残留** | 前次会话的后端 (8080) 和前端 (5173) 进程未清理 | 新后端/前端无法在原端口启动 | 每次启动前先 `netstat -ano | grep ":8080\|:5173"` 并 kill 旧进程 |
| 10 | **文章 ID 881 空标题/空内容** | AI 对体育新闻的 content 提取失败，返回空字符串 | 1 篇无效文章入库（已删除） | 在 ingest 管线中校验 title 和 content 非空，空内容不入队 |

### 问题总结：非模型（Claude）原因的系统性问题

以下是本次运行中发现的**与 AI 助手本身无关**的问题，即项目自身的基础设施、代码或外部依赖导致的问题：

#### 基础设施层（3 项）
1. **Redis 未预装**：项目依赖 Redis 但没有自动安装/启动脚本，Windows 环境下需手动 `winget install Redis.Redis`
2. **Python 路径损坏**：用户 PATH 中 `D:\python.exe` 不存在，导致 `python3` 命令失效，`init.sh` 崩溃
3. **进程残留**：前后端进程未在会话结束后自动清理，端口持续占用

#### 代码/Bug 层（3 项）
4. **@Scheduled 消费者不稳定**：`ArticleIngestConsumer` 的 `@Scheduled` 方法在首次 Redis 连接后未能正常轮询，队列中的文章 10+ 分钟未被消费，但后续测试中又恢复正常 — 疑似 Lettuce 连接池在初始连接失败后进入静默死状态
5. **无 ingress 数据校验**：`ArticleIngestConsumer.toArticle()` 未校验 title/content 是否为空，空数据直接入库
6. **无单元测试**：`src/test/` 目录不存在，任何改动无法自动回归

#### 外部依赖/AI 模型层（2 项）
7. **DeepSeek V4 Flash 分类能力不足**：所有文章 `category_id` 返回 0，未使用提供的 CATEGORY_MAP（共 10+ 分类）
8. **DeepSeek 游戏/非游戏判定过于宽松**：10% confidence 的体育新闻仍被标记为 `is_gaming=True`，过滤阈值形同虚设

#### 配置/工程层（2 项）
9. **`init.sh` 脆弱性**：`set -euo pipefail` + `python3 --version` 导致一个非关键检查阻塞全部初始化流程
10. **`ingest/.env.example` 缺失**：新开发者需自行摸索创建 `.env` 文件

**已记录证据：** 所有验证输出已在本文件中记录。

**下一步最佳动作：**
1. 修复 @Scheduled 消费者（添加 Lettuce 重连逻辑或显式 TaskScheduler 配置）
2. 在 ingest 管线中增加 title/content 非空校验
3. 优化 DeepSeek prompt 提升 category_id 分类准确率和 is_gaming 判定阈值
4. 修复 `init.sh` 中的 `python3` → `python` 回退逻辑
5. 为 `ArticleIngestConsumer` 和 `ArticleService` 补充单元测试

---

### Session 003 — 2026-07-05: 按 Session 002 结论修复 Bug + 建立故障文档 + 单元测试教学

**日期：** 2026-07-05

**本轮目标：** 1) 创建 Failures.md 记录所有 Bug；2) 修复消费者+数据校验；3) 修复 init.sh；4) 从零编写第一个单元测试；5) 输出原始 prompt 供讨论

**已完成：**

1. **创建 Failures.md** — 面向工作者的故障记录文件，共 8 个 Bug（BUG-001 ~ BUG-008），含表现/根因/解决方案/方法论

2. **BUG-003 修复: @Scheduled 消费者**（3 处改动）
   - `pom.xml`：新增 `commons-pool2` 依赖（Lettuce 连接池需要）
   - `application.yml`：增加 Lettuce 连接池配置（max-active=8, min-idle=2, connect-timeout=3s, timeout=5s）
   - 新增 `config/SchedulingConfig.java`：显式 `TaskScheduler` Bean（poolSize=2 + errorHandler）
   - `BigEventApplication.java`：移除冗余 `@EnableScheduling`（统一到 SchedulingConfig）

3. **BUG-006 修复: 空 title/content 入库**（2 处改动）
   - Python `main.py`：enqueue 前校验 title 非空 + content ≥ 50 chars，不满足则记 `total_filtered` 跳过
   - Java `ArticleIngestConsumer.toArticle()`：title blank 或 content < 50 chars 时抛 `IllegalArgumentException`，触发 push-back

4. **BUG-002/BUG-008 修复: init.sh**（重写）
   - `set -euo pipefail` 后移到编译阶段前（check-only 模式不受影响）
   - Python 检查改为：先测 `python3`（`|| true` 保护），失败再回退到 `python`
   - check-only 模式下所有检查降级为 warning，不终止脚本

5. **BUG-007 修复: 建立单元测试**（教学 + 实战）
   - 创建 `src/test/java/org/example/bigevent/service/impl/ArticleIngestConsumerTest.java`
   - 8 个测试用例，分两组：
     - **A 组 (5 个)**：纯逻辑测试 `toArticle()`，无 Mock，覆盖正常映射/零值/缺字段/空标题拒绝/短内容拒绝
     - **B 组 (3 个)**：Mockito 测试 `consume()`，Mock Redis + Service，覆盖空队列/正常入库/异常推回
   - `./mvnw test` → BUILD SUCCESS, 8/8 pass

6. **BUG-007 部分: confidence 阈值**（用户自行实现）
   - 用户在 `main.py` 中自行添加了 `confidence < 0.2 → is_gaming=False` 过滤（已确认生效在 main.py 第 88-90 行）

7. **输出 DeepSeek 原始 prompt** — 待与用户讨论 prompt 优化方案（分类目录对应爬取平台的架构重构）

**运行过的验证：**
- `./mvnw compile` → BUILD SUCCESS
- `./mvnw test` → 8/8 pass, BUILD SUCCESS
- `bash init.sh --check-only` → 全部 6 项通过（Python 3.12.9 通过 conda 回退检测）

**修改的文件清单：**

| 文件 | 操作 | 关联 Bug |
|------|------|----------|
| `Failures.md` | 新建 | 全部 |
| `pom.xml` | 编辑（+commons-pool2） | BUG-003 |
| `application.yml` | 编辑（+lettuce pool 配置） | BUG-003 |
| `config/SchedulingConfig.java` | 新建 | BUG-003 |
| `BigEventApplication.java` | 编辑（移除 @EnableScheduling） | BUG-003 |
| `ingest/main.py` | 编辑（+data validation；+confidence 阈值-用户） | BUG-006, BUG-007 |
| `ArticleIngestConsumer.java` | 编辑（toArticle 校验 + package-private） | BUG-006 |
| `init.sh` | 重写（python3 回退 + 宽松 check-only） | BUG-002, BUG-008 |
| `ArticleIngestConsumerTest.java` | 新建（8 测试用例） | BUG-007 |
| `claude-progress.md` | 更新 | — |

**已记录证据：** 所有编译和测试输出已在上述记录中确认。

**当前 blocker / 未完成：**
- DeepSeek prompt 优化（BUG-004, BUG-005）— 用户待讨论后自行修改
- 爬虫"分类对应平台"架构重构 — 用户待自行实现
- `Failures.md` 中 BUG-004, BUG-005, BUG-007, BUG-008 状态待用户自行更新

**下一步最佳动作：**
1. 用户自行完成爬虫分类架构 + prompt 优化
2. 用户自行运行一次完整管线验证所有修复
3. 更新 Failures.md 中剩余 Bug 状态

---

### Session 004 — 2026-09-20：真实上线设计基线与接管

**目标：** 将用户逐项确认的产品、采集、隔离部署和资源约束转化为后续可持续开发文档。本轮不改业务代码、不安装/部署服务器。

**本轮核查：**
- Git 基线 d9537c1，feature/push-platform 比已知远端领先一个提交；原有 HeatService.java 空白改动保留。
- 权限链、文章/采集 Controller、文章服务/Mapper、前端页面/路由/Store、SQL、Python 管线、已有测试入口做静态核查。
- 发现公开草稿可见性、普通用户写权限、push demo、手动消费入口等上线阻断项；证据写入设计第2节。
- 延续此前同一接管讨论的服务器只读快照：约11 GiB可用内存、22 GB可用磁盘、已有多个服务、8080占用；本轮编写文档没有重新连接或改动服务器。

**变更：**
- 新增 docs/PROJECT_DESIGN.md：业务决策、现状证据、目标架构、数据/接口/管线、可靠交付、资源/容量、部署门禁。
- 新增 docs/DEVELOPMENT_PLAN.md：15项任务、依赖、验收、清理边界与证据要求。
- 新增 docs/SOURCE_EVALUATION.md：来源候选与验证模板；没有将源码参考冒充可用来源。
- 新增 AGENTS.md；README/CLAUDE/DEVELOPMENT 增加当前入口或历史提示。
- feature_list.json 建立任务清单，任务状态独立于历史进度；只有文档任务可在本轮验证后完成。

**设计区分：** 用户硬约束固定；HTTP+持久outbox、具体版本候选和验收负载为工程默认。正文能力预留而非首期全文搬运。4 GB/10 GB是整个项目预算，实际限制需后续验证，不能靠文档宣称已保证。

**本轮没有执行：** Java/Vue/Python构建测试、数据库迁移、来源实抓/收费AI、容器部署、服务器清理、git commit/push。未创建任何自动化任务。

**下一步：** BASE-01——完整接口矩阵与可重现基线；之后 SEC-01 权限/可见性修复。不要直接重写爬虫或部署。


**文档验证结果（本轮实际执行）：**
- Python 文档检查：15 个任务 ID 唯一、依赖完整且无环、各任务验收章节存在；6 个新文档本地链接有效；代码围栏与 JSON 示例有效；新文档无行尾空白。
- 文档凭据扫描：交接密码未出现在新增/修改的文档与任务记录中，扫描不输出密码值。
- `git -c core.autocrlf=false diff --check -- README.md CLAUDE.md DEVELOPMENT.md claude-progress.md feature_list.json`：通过。第一次检查发现新增文档空白/换行问题，已仅在文档内修正并复查。
- 全仓库 diff 仍有用户原先 HeatService.java 空白修改，本轮未改动，不宣称全仓库 whitespace check 通过。
- DOC-00 标为 done；其余 14 项保持 pending。业务构建/测试未执行，下一任务 BASE-01。


---

### Session 005 — 2026-09-20：BASE-01 可重现基线与接口审计

**目标：** 完成 DEVELOPMENT_PLAN 中 BASE-01；不改变业务行为，不连接本地/生产数据库、Redis 或 HTTP 服务。

**完成：**
- 新增 `docs/API_AUDIT.md`，静态记录全部现有 Controller 路由、SecurityConfig 规则、Vue 实际调用/路由、旧接口差异及 SEC-01 后续项。
- 新增 `scripts/check.ps1`（PowerShell）统一运行 Java 测试、前端生产构建/测试与 Python 编译/导入冒烟。
- 为前端加入 `npm test`；新增 `format.test.js` 的 2 个基础用例。
- 修复前端测试基础设施：`src/test/setup.js` 原先从 @vue/test-utils 调用不存在的 `cleanup()`，所有 Vitest 测试都会 afterEach 失败；改为说明性空 setup。此修复不改变产品行为。
- BASE-01 标记 done，下一项为 SEC-01。

**实际验证：**
- `./scripts/check.ps1`：PASS。
  - Maven：13 tests、0 failures/errors/skips；Mockito 动态 agent 警告仍存在，记录为未来 JDK 兼容风险，非本轮阻断。
  - Vite production build：PASS；主 chunk 约 1.31 MB（gzip 351 KB）并有 >500 KB 警告，记入 WEB-01，不作为基线阻断。
  - Vitest：1 file、2 tests PASS。
  - Python：compileall + main/agent/spider imports PASS；未调用网络、Redis、OSS 或 AI。
- 首次 `npx vitest run` 在没有测试文件时为 exit 1；添加最小测试后发现 setup 的 cleanup 错误，修复后完整检查通过。

**未验证：** HTTP/DB/Redis 联调、角色授权、真实来源、迁移、部署、性能/资源。`scripts/check.ps1` 使用已存在的本地依赖，不安装任何包。

**下一步：** SEC-01——权限与公开内容边界：限制文章/采集/演示入口，所有公开读取和互动目标仅允许已发布文章，并建立角色×方法×对象状态测试矩阵。


---

### Session 006 — 2026-09-20：SEC-01 权限与公开内容边界

**目标：** 完成首期管理员发布边界、公开文章可见性和演示接口关闭；不做数据迁移、采集改造或服务器部署。

**完成：**
- `SecurityConfig`：文章写操作与 `/admin/**` 限 CONTENT_ADMIN/SUPER_ADMIN；`/push/**` 和旧 PUT 评论点赞计数接口 denyAll；保留正常公开读取与既有登录/互动基础路由。
- 文章服务/Mapper：公开 list/search/detail/hot 强制只看 `已发布`；detail/stats/heat 及互动目标对未发布返回 HTTP 404；管理员可更新/删除系统采集文章且保留原创建者。
- 互动 Controller：文章点赞/收藏、评论、浏览、评论点赞均在操作目标前检查所属文章公开状态。
- `GlobalExceptionHandler`：ResponseStatusException 不再被通用处理器压成 HTTP 200，404/403 等状态可保留。
- 前端：移除普通用户发布入口；`/publish` 改为 `/home` 重定向，添加对应 Vitest。
- 测试：新增 Spring MVC SecurityBoundaryTest（9 项）与 ArticleVisibilityServiceTest（3 项）；为 Spring Boot 4 增加仅 test scope 的 webmvc test starter。测试首次暴露 Boot 4 WebMvcTest 模块拆分、测试上下文 PasswordEncoder 缺失和异常状态被吞为 200，均已修复并回归。

**实际验证：**
- `./scripts/check.ps1`：PASS。
  - Maven：25 tests、0 failures/errors/skips。
  - Vite build：PASS；主 chunk 约 1.31 MB（gzip 351 KB）警告保留给 WEB-01。
  - Vitest：2 files、3 tests PASS。
  - Python compile/import smoke：PASS；未发起网络/AI/Redis/OSS 调用。
- 角色测试：匿名 POST 文章 401；ROLE_USER 文章 POST/PUT/DELETE 和旧 ingest 403；CONTENT_ADMIN POST 文章 200；push/旧评论计数写接口 403；草稿详情 404。

**未验证/后续：** 真实 JWT+Redis、数据库/MyBatis SQL、完整互动数据库写入、用户资料字段白名单、评论/登录限流、上传安全及管理员后台。DATA-01 将建立版本化 schema、文章来源/摘要/时间模型和迁移基线。

**下一步：** DATA-01——版本化数据库与新文章模型。此阶段不直接迁移真实数据库；先选择迁移工具、建立空库基线和测试副本策略。


---

### Session 007 — 2026-09-20：DATA-01 版本化数据库与首期内容模型

**目标：** 建立可重复的 MySQL 数据库基线、首期 FPS 内容存储结构和旧数据预演；不连接真实本地 MySQL80、远程服务器或任何生产库。

**完成：**
- 引入 Flyway（Spring Boot 4.0.5 管理的 Flyway 11.14.1）与 MySQL 模块，配置 `classpath:db/migration`。
- V1：完整 legacy 空库基线；V2：game/content_source、article 元数据/审核字段、article_game/content、ingest receipt/run/record、moderation audit、legacy review；V3：四个首期游戏与兼容 category seed。
- `Article` 增加 summary、source、原文时间/链接、阅读模式、内容类型、工作流与 revision 字段；新增 Game、ContentSource records 和三个稳定枚举。
- 增加 `legacy_article_review`：category ID 不在 5/8/9/10 的旧文章进入 `UNMAPPED_LEGACY_CATEGORY` 清单，不删除、不自动发布。
- 增加数据库迁移/旧库预演文档；移除 application.yml 与 init 脚本中的提交式数据库密码/JWT 默认 secret，改为环境变量。

**实际 MySQL 验证：**
- 创建仅用于测试的 MySQL 8.0.45 临时实例：localhost:33067、数据库 `gameboi_flyway_test`；未使用现有 MySQL80 服务。
- `MySqlFlywayIntegrationTest`：空库迁移 V1–V3 成功，第二次 migrate 为 0；验证 4 个启用游戏。
- 同一临时实例中，V1 旧库副本插入 legacy user/category/article/comment/article_like/article_favorite，再迁移 V2/V3；验证 article ID/category、三类关联保留，未知 category=77 写入 review 表。
- 测试结束后停止临时 mysqld，并删除 `C:\mysql-flyway-test`，确认目录不存在。

**完整基线：** 后续 `./scripts/check.ps1` 将运行 Flyway 资源与模型测试；MySQL 集成测试默认安全跳过，只有设置 localhost 且数据库名为 `gameboi_flyway_test` 的环境变量才可执行清理型验证。

**未做：** 未对任何已有数据库执行 baseline/import/迁移；实际旧库仍需按 DATABASE_MIGRATION.md 的审批和副本流程处理。当前 Mapper 仍写 legacy article 列，API-01/后续任务将引入新 DTO/持久化方法。

**下一步：** API-01——统一 `/api/v1`、`/internal/v1` 契约、分页/错误语义、公开/管理/内部接口边界与契约测试。


---

### Session 008 — 2026-09-20：API-01 契约与公开 v1 读接口（进行中）

**已完成的局部工作：**
- 新增 `docs/api/openapi.yaml`，定义 `/api/v1` 与 `/internal/v1` 的首期边界、响应封装、分页、文章 DTO、错误码和 ingest 事件契约。PyYAML 已解析检查关键 path/schema。
- 新增只读 `GET /api/v1/games`、`GET /api/v1/articles`、`GET /api/v1/articles/{id}`。返回 DTO，不暴露列表正文；摘要阅读模式详情不返回 HTML。四游戏筛选暂通过旧 category ID 兼容映射；`contentType` 在未完成 article_game/content 持久化前明确 422，不静默忽略。
- 新增 v1 API 角色/DTO MockMvc 测试；`./scripts/check.ps1` 当前 PASS：后端 36 tests（2 个隔离 MySQL test 因未设置环境变量安全跳过）、前端 build+3 tests、Python import smoke。

**未完成：** 管理端 v1 写/审核接口、内部服务认证/ingest DTO 落库、traceId 统一错误响应、前端切换和旧接口移除条件。因此 API-01 保持 in_progress，下一轮继续该任务。


---

### Session 009 — 2026-09-20：API-01 统一契约与兼容期

**完成：**
- 冻结 `docs/api/openapi.yaml`：公共/管理员/内部路径、页面 DTO、分页、错误、ingest event。未实现的 admin/internal 路径显式 `planned`，不伪装已上线。
- 实现公开只读 `/api/v1/games`、`/api/v1/articles`、`/api/v1/articles/{id}`；DTO 隔离数据库实体，列表不返回 HTML，摘要阅读不返回正文，旧分类 ID 仅作四游戏兼容映射。
- v1 trace filter、成功/错误 envelope、X-Trace-Id 和 422 测试。
- 新增 API_COMPATIBILITY 文档和 OpenAPI 验证脚本，接入 scripts/check.ps1。

**验证：** `./scripts/check.ps1` PASS；后端 36 tests、2 个未配置隔离数据库时的 MySQL 测试安全跳过，前端 build+3 tests、Python smoke PASS。

**未实现且已明确：** admin v1 写/审核在 ADMIN-01；internal ingest 交付在 ING-02；Vue 切换在 WEB-01。旧接口不删除，按 API_COMPATIBILITY 的移除门禁推进。

**下一步：** ING-01——将 Python 管线拆为来源适配、确定性抽取、白名单/类型判定、预算/互斥/干跑保护，先不做后端可靠交付。

---

### Session 010 — 2026-09-20：ING-02 可靠交付链路（实施中）

**本轮完成：**
- Python 采集结果以 SQLite outbox 持久化；仅收到 internal API 的 HTTP 200/201 后 ACK。网络、401/403、429、5xx 与非成功响应都会保留 READY 状态并累加失败信息。
- outbox 在采集前先尝试投递既有积压；待投递数量达到 `MAX_OUTBOX_PENDING`（默认 200）时暂停新增采集。Redis 去重 URL 移到 receipt 成功后的 ACK 回调，避免“只入队未入库”被错误跳过。
- 固化 Python/Java 内部事件契约：schemaVersion、来源、四游戏、内容类型、原文 URL、抓取时间、标题、摘要、tags、暂存内容、决策、规则版本。删除 legacy categoryId 的交付依赖。
- `POST /internal/v1/ingest/events` 已使用 `X-Ingest-Service-Token`；新 event 返回 201、同 eventId 同 payload 返回 200、同 eventId 不同 payload 返回 409。
- receipt、来源自动登记、文章、article_content、article_game 与 receipt.article_id 在一个 Spring 事务内写入。ACCEPT/REVIEW 均落为 `PENDING_REVIEW` 草稿，REJECT 只留 receipt；首期外链摘要模式不公开暂存正文。
- 新增 V4 migration，提供 `ingest-system` 专用内容管理员账号；旧 Redis List consumer 和 `/admin/ingest/consume` 默认不注册，迁移约束记录在 `docs/API_COMPATIBILITY.md`，不清空既有 Redis。

**验证：**
- `scripts/check.ps1` 已通过：OpenAPI、后端 43 项测试（2 个需显式隔离 MySQL 的测试安全跳过）、前端 build/3 项 Vitest、Python 6 项 unittest 和导入冒烟。
- V4 已加入 Flyway 布局与隔离 MySQL 集成测试断言；本机未设置 `GAMEBOI_FLYWAY_TEST_URL`，因此未重新启动任何 MySQL 实例，也未连接已有数据库或服务器。

**待完成：**
- 在新建的隔离 MySQL 实例上执行含 V4 的 Flyway 集成测试，并进行 Java Mapper 的真实事务回滚验证；完成后才将 ING-02 标记完成并进入 ING-03 单游戏纵向样板。

### Session 011 — 2026-09-20：ING-02 隔离 MySQL 验证完成

**完成：**
- 使用全新临时 MySQL 8.0.45（127.0.0.1:33067、`C:\mysql-flyway-test`）执行 V1–V4 Flyway 空库、二次迁移和 legacy V1 升级验证。
- 新增真实数据库事务测试：确认 receipt、article、article_content、article_game 一次性写入；同 payload replay 不创建重复业务数据；后续游戏关联失败时 receipt、来源、文章及已写关联全部回滚。
- 测试仅针对临时实例；测试完成后已关闭 mysqld 并删除 `C:\mysql-flyway-test`，未连接 MySQL80、远程服务器或现有数据库。

**结论：** ING-02 完成。下一任务切换为 ING-03：以 CS2 为默认游戏，落实一个真实验证来源与离线样本的纵向采集、分类、待审核入库和公开查询验证。

### Session 012 — 2026-09-20：ING-03 官方来源纵向样板完成

**来源验证：**
- CS2 Steam News API 的官方返回曾实测成功，但 Python 运行时出现 timeout/disconnect；按计划登记为 `PAUSED`，没有通过 PowerShell 或硬编码结果绕过 Python 运行失败。
- 改用 Riot 官方 VALORANT game-updates 路径。真实 dry-run 发现 6 篇，5 篇 Patch Notes 均被确定性判定为 `ACCEPT / valorant / patch_notes`；第 6 篇无明确类型进入 REVIEW。
- 将真实列表/详情快照固化为 tests fixture，验证 URL、标题、正文和原始 ISO 发布时间解析；离线纵向测试生成 versioned event，保留中文编辑标题、摘要、tags、原文 URL 和时间。

**后端纵向：**
- internal receipt 的真实 MySQL 测试以 VALORANT Patch Notes URL 写入待审核文章，确认 `article_content`、`article_game`、receipt、原始时间；测试环境显式发布后，v1 归一化查询按 `valorant + patch_notes` 返回这一文章，重放没有重复数据。
- 临时 MySQL 实例已再次关闭并删除，未连接现有 MySQL80、远程服务器或生产数据。

**下一步：** WEB-01——前端迁移到 `/api/v1/games`、`/api/v1/articles`、摘要详情与原文跳转；清除 legacy 分类和演示数据依赖。

### Session 013 — 2026-09-20：WEB-01 公开客户端迁移与 ADMIN-01 审核骨架

**WEB-01 已推进：** 公共首页、分类 Store、分页文章卡片、详情摘要/原文跳转迁移到 v1 API；提供类型筛选、错误/空/加载状态、401/403 明确反馈。旧轮播、消息中心演示、发布页面和无引用 Store 已从路由/构建图移除。前端 Vitest 现为 6 项，通过生产 build。

**ADMIN-01 已启动：** 新增 `/api/v1/admin/articles` 审核列表与 approve/reject/unpublish，使用内容管理员角色、revision 乐观锁和 `moderation_audit`。服务和权限 MockMvc 测试通过。来源管理、运行记录、容量状态和管理端页面仍需继续。

### Session 014 — 2026-09-20：来源暂停策略打通与 Apex 官方源接入

- 新增 Apex Legends EA 官方 source adapter、真实列表/详情 fixture、列表原始发布时间透传测试和真实 dry-run 验证。
- 新增 V5/V6 source seed：VALORANT、Apex 均为 `SHADOW`，禁止自动发布。
- 后端内部 source policy endpoint 用服务令牌保护；管理员 pause/resume 改变 `content_source` 后，Python 采集器在配置 internal endpoint 时先读策略，失败默认停采，不持有 MySQL 凭据。
- 新建临时 MySQL 验证 V1–V6、系统采集写入、文章查询、来源 pause/resume policy；实例已删除。

### 2026-09-22：纠正 dry-run 副作用与验证脚本假阳性

重新检查当前代码发现：旧 dry-run 仍构造 RedisClient、读取去重集合、在来源失败时累加 total_errors，并在结束时写 last_run_at。历史“无生产状态副作用”的说法不准确，不能作为验收证据。
本轮 dry-run 改为不构造 Redis/SQLite/AI/OSS/投递器/容量依赖；禁止同时使用 --stats。增加四项离线 CLI 回归覆盖正常、空列表、失败和参数冲突，无真实外部连接。
同时修复 scripts/check.ps1：compileall 或 unittest 非零即报错，后续 import 成功不能覆盖前面的失败。新增 PowerShell 子进程故障注入测试，分别模拟编译和测试退出 17，确认统一脚本退出 1 且未继续 import。
本轮实际运行 scripts/check.ps1：PASS；Python 22 项。后端及前端原有基线也通过；隔离数据库测试仍按环境条件跳过，本轮未启动数据库、Docker、服务器任务或付费 API。剩余功能与部署门禁不因本次回归通过而标记完成。

### WEB-01：筛选并发与分页重试回归

本轮核对文章列表发现两个未被原有测试覆盖的问题：旧筛选请求晚返回会覆盖新筛选结果；加载下一页失败后页码已经递增，会导致重试跳页。
修复：筛选变更使用请求代次隔离结果/异常/loading；切换时清空旧列表与总数；成功后才更新页码；翻页失败保留已加载列表和明确错误提示；请求中阻止重复加载，卸载后不再提交结果。
实际验证：前端 npm test 共10项通过，新增4项覆盖乱序成功、过期失败、失败重试同页和重复点击；npm run build通过，仍有既有大包警告。
本次为组件级交互回归，不是浏览器端到端或真实后端联调证明。WEB-01保持in_progress，未部署、未调用外部服务。

### WEB-01：详情请求隔离与原文展示回归

修复详情页等待游戏列表导致延迟和重复加载的问题，详情独立立即请求；路由变更用请求代次阻止旧文章覆盖新文章，卸载后不提交结果。原文日期未知时明确显示“未知”，不拿站内发布日期冒充原文日期；缺失、非法协议、含账号密码的原文 URL 不生成跳转链接。404 显示文章不存在或尚未发布，允许原路由重试。
本轮 npm test：19项通过；npm run build：通过（既有大包警告仍在）。新增9项详情组件用例覆盖摘要不渲染正文、首请求不重复、六类无效链接、乱序返回、404重试。不等同于浏览器端到端/真实后端联调验收；WEB-01继续进行中。

### WEB-01：修复实际 HTTP 代理路径不匹配

本轮发现并修复开发代理把 /api/v1/games 改写成 /v1/games 的错误。Vite 现在保留 v1 前缀，仅对白名单旧接口去掉 /api；部署 Nginx 同步保留 v1、兼容 /api/user/login 等旧接口，并明确阻断 internal 和未知 API 路径。
新增 gaming-news/scripts/check-proxy.mjs：使用随机本地端口启动临时 HTTP 回显服务和真实 Vite 代理，检查6项路径/查询参数/方法/403透传，另检查3项内部/demo请求不转发。finally关闭所有临时服务，不启动真实后端或数据库。接入 scripts/check.ps1。
本轮全量验证通过：后端54项（5项隔离数据库测试按条件跳过）、前端19项、HTTP代理9项检查、Python22项。Nginx配置尚未在容器实际加载，不能将Vite代理通过等同于生产反代或浏览器E2E通过。WEB-01仍进行中。

### WEB-01：登录失效和登录失败恢复

修复401仅删除localStorage却保留Pinia登录状态的问题；失效时清除用户信息和响应式token，保留返回路径。同一请求发出后用户重新登录，旧token的迟到401不再退出新会话。403保留会话并返回权限反馈；匿名登录失败不强制跳转。删除Axios错误对象日志，避免输出请求里的密码/token。用户信息迟到响应不覆盖已经退出/切换的会话。
修复登录请求异常后按钮一直loading的问题：finally恢复、明确错误提示、允许再次提交；登录后仅采用站内返回路径。
新增5项HTTP拦截器/表单测试。本轮scripts/check.ps1通过：前端24项、Python22项、实际Vite代理9项检查；后端通过（隔离数据库条件未配置的测试仍跳过）。本轮没有服务器/数据库/付费服务操作，WEB-01仍需浏览器E2E与其余用户功能验收。

### WEB-01：真实 Chromium 桌面/移动端 UI 回归

确认本机已有 Playwright/Chromium，未安装额外依赖。新增 scripts/check-web-browser.py，使用生产Vue构建、临时随机localhost端口和显式fixture API，阻断所有外网请求。在1440px/390px验证首屏文章请求一次、游戏和类型筛选、分页、摘要原文链接、404、503重试、空态、旧消息路由重定向，无浏览器未捕获异常。临时浏览器/服务在finally关闭。
实际运行 scripts/check.ps1 -Browser 全部通过。此证据是浏览器真实运行而非组件模拟，但API仍是fixture，不替代Spring/MySQL/Nginx端到端验证。WEB-01保持进行中。详细运行方式与范围见 docs/WEB_BROWSER_TESTS.md。

### WEB-01：公开游戏标签改为真实关联

修复列表虽按article_game筛选、返回标签却仍按legacy categoryId推断的不一致。现在列表一次批量读取该页文章的关联游戏（主游戏优先），详情也读取真实关联；不再用旧分类ID编造标签。空页不执行空IN查询；分页查询结束主动清理PageHelper上下文，避免污染后续关联查询。
本轮实际运行 mvnw.cmd test -Dtest=PublicApiV1ControllerTest：8项通过，新增多文章跨游戏关联/旧分类冲突、详情关联、空页三项回归。新增Mapper SQL本轮未在真实MySQL执行，不将MockMvc通过称为数据库联调完成；下一次隔离MySQL验收需要覆盖该批量SQL。WEB-01保持进行中。

### WEB-01：真实MySQL关联与分页验证补齐

在全新临时MySQL实例127.0.0.1:33067（datadir=C:\mysql-flyway-test）执行IngestReceiptMySqlTransactionTest，4项通过且0跳过。新增真实Mapper+Controller测试验证跨游戏关联主次顺序、旧分类不影响DTO标签、每页1条时两个游戏标签均保留、详情标签一致以及PageHelper上下文清理。新测试以测试事务回滚隔离，避免污染既有用例。
同时收紧该测试的破坏性操作边界：仅接受指定localhost/127.0.0.1:33067和固定数据库URL，测试数据库已存在则拒绝创建，不再复用清理未知库；只有本轮创建成功才执行DROP。
测试结束已通过mysqladmin关闭本次实例，确认33067无监听。删除临时目录的PowerShell调用被执行策略拒绝，未绕过策略；C:\mysql-flyway-test仍保留，后续需授权清理或换新的明确临时路径。未修改/停止其他mysqld进程，未连接现有业务数据库。WEB-01仍需实际HTTP后端和浏览器整链路验收。

### WEB-01：游戏列表请求合并与失败重试

游戏Store现在按实例合并正在进行的请求（包括重复force refresh），请求结束移除pending，网络失败不阻断后续重试。已加载标签在刷新失败时保留。首页在桌面/移动端内容区明确显示游戏列表失败原因和重试按钮，不再只有ALL却隐藏错误。
新增3项Store回归，并扩展真实Chromium桌面/移动端脚本，模拟游戏API 503后点击重试、四游戏入口恢复。实际scripts/check.ps1 -Browser通过，前端27项。浏览器使用fixture API，仍未替代真实Spring/MySQL整链路；WEB-01保持进行中。

### WEB-01：补齐来源名称跨接口与详情展示

按设计EXTERNAL_SUMMARY要求，公开列表/详情DTO新增sourceName，批量从content_source读取登记名称，不根据页面文本或LLM自报来源。空页不执行查询，单页使用一次来源查询而非逐篇查询。详情显示来源，缺失时明确“未知”。OpenAPI原有sourceName字段现在有实现对应。
实际scripts/check.ps1 -Browser通过：公开接口8项、前端27项及桌面/移动端fixture浏览器流程；新增来源名称返回/展示断言。新来源Mapper SQL本轮尚未隔离MySQL实测，不将MockMvc断言当SQL执行证据；WEB-01保持进行中。

### WEB-01：真实HTTP接收与MySQL公开查询联调

扩展IngestReceiptMySqlTransactionTest为随机本地HTTP端口的完整Spring应用：服务令牌提交事件返回201，重放10次均200且只有1篇文章；待审核详情404；测试专用发布后，经真实HTTP读取分页列表/详情，验证sourceName来自来源表、多游戏标签一致、中文摘要/原文链接保留、摘要模式不返回正文；测试下架后再次404。
本轮全新MySQL datadir=C:\mysql-gameboi-http-test，绑定127.0.0.1:33067；实际5项集成测试全部通过，0跳过。此前两次启动因JDK Unix-domain临时socket创建失败中断，通过仅对测试JVM设置 -Djdk.net.unixdomain.tmpdir=C:\mysql-gameboi-http-test 后通过；未改系统Java配置。运行命令的关键参数为 -Dtest=IngestReceiptMySqlTransactionTest 与上述argLine，同时设置受限GAMEBOI_FLYWAY_TEST_URL。
测试MySQL已mysqladmin关闭且33067无监听。因上一轮清理被策略拒绝，本轮不尝试绕过；新旧临时目录均保留，后续需用户授权清理。其他MySQL进程未操作。
该测试已验证新增sourceName SQL的真实执行，不再只有MockMvc证据。但尚未把生产Vue浏览器接到该真实HTTP后端，且真实管理员发布/登录/互动和Nginx仍需单独验收；本轮通过不代表WEB-01全部完成。

### ADMIN-01：审核版本号比较与输入校验

修复Integer对象用!=比较导致高版本号（例如1024）即使值相同也误报409的问题，改为按数值相等判断。revision限制在0到2147483646，缺失/负数/溢出值在接口和服务层拒绝，不访问数据库。v1字段校验错误统一422并携带traceId，不返回legacy错误封装。
实际运行AdminModerationServiceTest与AdminArticleV1ControllerTest：7项通过，0跳过，覆盖高版本号批准/审计、非法revision拒绝、管理员权限和旧版本冲突。本轮未启动数据库/服务器；管理员真实审核事务与后台完整验收仍待继续。

### WEB-01：个人中心刷新与资料失败恢复

个人中心在已有token的刷新/直接访问场景主动加载资料，不再把缺失内存用户信息显示为“未登录”。加入加载状态、失败提示与重试；退出时清理资料状态；旧会话的迟到响应或失败不能覆盖新会话信息。管理员入口仍仅根据返回角色显示，后端权限校验不变。
新增3项Store回归覆盖持久token资料加载及重试、退出后迟到响应、新会话不被旧失败污染。本轮npm test共30项通过，npm run build通过（既有大包警告保留）。尚未进行真实登录Redis/HTTP浏览器整链路，不关闭WEB-01。

### WEB-01：生产Vue到真实Spring/MySQL浏览器链路通过

本轮添加scripts/check-web-live.py，并以GAMEBOI_BROWSER_E2E=1在真实HTTP数据库测试期间调用：无头Chromium→本地测试代理→Spring随机端口→独立MySQL，非mock API。1440px/390px游戏类型筛选、文章详情导航、来源/多游戏标签、中文摘要/原文入口通过。5项数据库集成测试通过，0跳过，浏览器2种尺寸日志均PASS。
已查看两张截图；首次截图截到路由淡入动画，改为等待opacity=1后重跑并检查最终截图。无横向溢出、来源/时间/摘要正常展示。未来2099日期和示例标题是排序测试夹具，不是上线数据。
复用的仅为本线程先前创建的C:\mysql-gameboi-http-test隔离测试目录，未复用业务环境；确认datadir和空测试库再执行。测试MySQL已shutdown、33067无监听；目录保留。生产部署未进行。
WEB-01真实公开阅读链路证据已补齐，但用户互动、安全/上线等全项目要求尚未完成，不标完整目标完成。管理员/登录Redis真实链路与Nginx生产反代不在本次覆盖中。

### WEB-01：恢复详情点赞和收藏，不用假成功替代互动

根据设计“保留并回归互动”的要求，将已有受保护点赞/收藏接口重新接回摘要详情，独立为ArticleInteractions组件。匿名不请求个人状态；登录后先读取已点赞/已收藏再允许操作；提交期间禁重复点击；只有确认成功才更新按钮。超时/失败不猜测服务端结果，禁用再次修改直到重新读取；会话退出或文章切换使旧响应失效。
新增4项组件回归验证匿名零个人请求、收藏确认后更新/防双击、点赞失败后重读、退出清空。本轮scripts/check.ps1 -Browser全通过，前端34项（包括详情9项）。浏览器回归仍主要覆盖匿名公开阅读，真实Redis登录互动尚未联调；评论、收藏列表、浏览/热度等保留功能仍需继续，不能据此关闭WEB-01。

### 用户互动：评论跨文章关联与载荷校验

补齐CommentServiceImpl统一写入校验：正文非空白且≤1000字符，文章/用户ID有效；父评论和被回复评论必须存在且属于同篇文章。不合格请求在数据库写入和热度增加之前拒绝，避免将其他文章的评论拼入回复树。
实际CommentWriteValidationTest四项通过：空/超长正文、跨文章父评论/回复、缺失父评论、同文章合法边界正文。未启动真实数据库、Redis或开启前端评论提交。设计要求的举报/删除处置、限流和容量门禁仍未全部完成，因此不能以此宣称评论已可上线。

### 评论运营门禁落实到后端

设计第328行要求不具备举报/删除处置及最小运营流程时关闭评论提交。此前只有前端未提供入口，直接POST仍可写入。本轮comments.submission-enabled默认false，在评论及回复业务处理前返回真实503“评论提交暂未开放”；公开已发布文章评论读取和已有删除入口保持不变。配置打开不代表已获上线批准，仍需先完成运营/限流/容量验收。
实际CommentSubmissionGateTest与CommentWriteValidationTest合计7项通过，验证登录用户直接POST/回复不能绕过门禁、无业务服务写入调用、匿名401、评论读取仍校验文章公开状态。未迁移数据库或操作线上环境。

### WEB-01：个人收藏入口接入真实分页接口

修复“我的收藏”按钮只是跳回/user的无效入口，新增登录保护的/favorites页面和GET /api/v1/me/favorites。服务端只取认证claims里的userId，不接收客户端用户ID；分页上限50，查询仅包含state/workflow均已发布文章，列表DTO不含正文或用户资料。前端显示标题/摘要、分页/失败重试，失败不丢弃已加载结果或递增页码。
本轮统一scripts/check.ps1通过，新增2项后端控制器身份/参数单测和2项前端分页/错误回归。OpenAPI路径已登记。新收藏SQL本轮未在隔离MySQL执行，用户间隔离及下架收藏真实数据验收仍需补齐，不因模拟测试通过就标完成。

### 收藏HTTP权限边界补测（数据库实测未完成）

计划补充收藏真实数据库分页/可见性验证时，包含文件修改与启动临时MySQL的组合调用被执行策略拒绝。健康检查确认对应测试未写入、33067未启动；没有换工具绕过启动限制。
改为补充FavoriteHttpBoundaryTest，通过真实MVC安全链验证匿名带userId仍401、登录请求伪造userId=99时只使用认证claims用户3查询、超大分页422且不访问Mapper，响应携带traceId、不含正文。
实际运行FavoriteHttpBoundaryTest及FavoriteV1ControllerTest合计5项通过。JWT claims和Mapper为测试替身，此证据不证明Redis认证或SQL过滤；收藏真实MySQL可见性/分页测试仍未完成，后续需在允许的隔离测试执行范围内补齐。

### ING-02回归：ACK必须验证receipt，不仅看HTTP状态

发现旧投递器遇到任意HTTP200/201即ACK，可能把HTML错误页、其他事件receipt或无文章ID的伪成功确认掉。现校验JSON对象、相同eventId、64位payloadHash格式、有效decision，以及ACCEPT/REVIEW必须有正整数articleId；REJECT可确认处理但不得关联文章。非法响应保持READY，不调用URL去重回调。注意这里只验证服务端hash字段格式，尚未统一跨语言canonical hash比较，不能声称内容哈希已跨端核验。
确认receipt后Redis回调失败只给不含敏感信息的警告，不撤销ACK、不误记投递失败。持久错误信息只保留状态码/异常类型，不存任意响应正文或异常消息。
新增4项用例（含9种非法receipt子场景），本轮scripts/check.ps1通过，Python26项。测试中的SQLite连接使用closing关闭，Windows文件锁问题已解决。未访问真实采集接口或改动服务器。

### ING-02：本地事件不可变与有界投递批次

修复outbox INSERT OR IGNORE掩盖“同eventId不同载荷”的问题：事务内读取并检查既有payload，相同内容幂等返回，不同内容明确失败，原数据不覆盖；已ACK事件重入不变回READY。不再修改调用方payload字典。ready默认每次取50条，允许范围1–200，避免一次把全部积压正文加载到内存；未读部分保留。fail只更新READY，不污染已确认事件。
新增4项SQLite实际文件测试：冲突原文保留/同载荷重放、调用方对象不变、有界读取55条分批确认、4线程重复入队10次仅一行。Python完整30项通过，不连接外部服务。本轮未验证跨语言canonical hash或进程强杀恢复，仍不视为所有可靠性交付门禁已完成。

### 来源策略fail-closed修复

修复三处停采门禁：非dry-run缺internal地址/token此前会放行，现启动即退出，且不构造Redis、AI或outbox；策略JSON必须enabled严格为true且状态SHADOW/AUTO_ENABLED，字符串false/数字1/未知状态不再被truthiness放行；Java未知来源null状态不再触发Set.of.contains空指针，而返回MISSING/disabled。
新增3项Python回归（含多种畸形policy子场景）和2项Java策略回归。实际scripts/check.ps1通过，未访问真实服务或付费模型。dry-run仍只采样，不请求生产策略。容量/计划频率/逐候选准入及完整来源质量门禁仍待补齐，不把本次修复当作自动采集上线批准。

### 采集互斥与异常恢复

旧SQLite worker_lock记录在异常退出后永久残留，且AI/OSS/outbox初始化在finally外，初始化失败也无法释放。本轮将客户端初始化纳入finally保护，新增同一路径.lock文件的OS互斥（Windows字节锁/Linux flock），进程退出由OS释放，不删除锁文件以免破坏文件身份。保留SQLite旧锁检测；若存在历史锁行则停采，绝不自动清除可能属于旧运行中的worker。
每日预算改为BEGIN IMMEDIATE事务内检查与增量，防止并发超订。
新增4项测试：子进程os._exit不执行finally后仍可重获锁、8线程20次预算申请仅3次成功、初始化失败释放锁、历史SQLite锁保留。实际scripts/check.ps1全通过，Python37项。Windows路径实际验证；Linux flock分支仍待隔离容器验证。旧锁需确认无旧worker再人工处理，不擅自清理现存状态。

### OPS-01口径纠错：宿主分区占用不是项目占用

旧CapacityGuard使用disk_usage(root)把其他项目磁盘用量也算进本项目，无法证明4GB/10GB要求。本轮改为只读完整项目快照：images/volumes/logs/temporary/backups字节和；范围不对、字段缺失/非法、超过120秒、未来时间异常、快照不可读均UNKNOWN停采；宿主余量低于5GB独立STOP。硬阈值不能配置超过10GB。
新增/重写5项容量测试覆盖边界、完整求和、宿主余量、未知快照、禁止提高上限。scripts/check.ps1通过，Python40项。Compose采集配置改为CAPACITY_SNAPSHOT_PATH，没有放入假快照。
这只实现观测消费契约；实际宿主/Docker容量采样、只读挂载、写入预留、目录配额和全口径资源实测尚未完成，不能宣称≤10GB已保证。没有可靠完整快照前实际采集会暂停，而非错误放行。

### 首期采集：逐候选准入与停止图片搬运

在每个候选AI处理前重新检查完整容量快照和outbox待投递上限，准入拒绝发生在预算消耗和AI调用之前，避免单批次不断生成队列超出软上限。首期EXTERNAL_SUMMARY不再构造OSS客户端或调用图片下载上传；后续全文/媒体许可方案确认前不恢复此阶段。
新增3项离线管线回归：UNKNOWN容量零AI/零预算、outbox到限后第二候选停采、不调用OSS、候选间容量恶化停止。实际scripts/check.ps1全通过。未访问付费模型、源站或对象存储。
这仍不是10GB硬配额：实际容量采样、预留空间、服务端写入准入、镜像/备份峰值等门禁继续保留。已抓取文本和暂存正文的后续保留策略也需按设计审计。

### ING-02：入站事件提前校验，避免坏消息进入事务

schemaVersion只接受1，eventId最大36与receipt列宽一致；gameCodes元素不能为null/空白且不能重复；原文URL只接受HTTP(S)、有效host且不得携带用户名密码；fetchedAt和非空originalPublishedAt必须为带offset的ISO时间。非法事件在receipt事务前返回400，不再等到数据库长度错误、空指针或日期解析500。
新增2项HTTP校验测试覆盖8类坏输入；InternalIngestControllerTest共6项通过。当前eventId仅限制长度，不宣称已强制UUID格式。Apex仅有日期的来源需后续显式处理日期精度，不能凭空补时区；跨端时区输出规范仍是待验收项。本轮无数据库或真实采集服务操作。

### FPS最终判定：不再丢弃模型REJECT/REVIEW

发现prompt要求的decision/game_codes/content_type此前未映射到ProcessedArticle，主流程仅检查is_gaming和confidence，可能忽略模型明确拒绝。本轮保留结构化字段并加入规则+模型保守合并：任一明确拒绝优先；只有双方ACCEPT且游戏/类型一致才能ACCEPT；缺字段/冲突/规则歧义均REVIEW，模型不得覆盖确定性游戏证据或提升规则REVIEW。confidence不再当作固定0.2真假阈值。
模型两次失败的原文兜底显式PROCESSING_FAILED/REJECT且is_gaming=false，不进入文章投递；此处为流程失败保护，不将其当负样本质量标签。
新增6项离线合并/字段映射/失败保护回归，scripts/check.ps1通过，Python49项。未调用真实模型。仍需持久化失败/待人工处理记录，未知类型不能伪造；冻结质量集与自动发布来源门禁尚未满足。

### ING-01：来源响应解析前的解码大小上限

HTML与Steam JSON改用同一流式文本读取入口，累计解码后响应超过2,000,000字节时拒绝交给解析器；响应上下文退出自动关闭连接，默认最多3次重定向。移除旧的强制UTF-8覆盖，按HTTPX识别编码读取。
新增4项离线HTTPX响应测试覆盖正常中文、精确边界/超1字节、gzip小体积膨胀超限、HTTP503资源关闭；Python完整53项通过。官方适配器fixture读取改用HTTPX MockTransport，未发真实网络请求。
限制说明：此测试证明送入HTML/JSON解析器的解码累计大小有界，不代表压缩解码过程的峰值内存已实测，也未实现DNS绑定/私网地址/跨域重定向完整SSRF防护。这些仍保留为上线前安全/资源门禁。

### 来源请求：逐跳来源域名检查

取消HTTP客户端自动跟随重定向。每次请求前验证URL属于适配器配置LIST_URL的确切hostname，只接受HTTP(S)标准端口、不带账号密码，HTTPS来源不能降级HTTP；相对同源跳转允许，超过3跳停止，跨域/私网文字地址等目标在发请求前拒绝。避免恶意列表链接/302将采集器带到未配置主机。
新增4项MockTransport回归：7类非法初始URL不触发请求、跨域/降级重定向不发送第二跳、同源相对跳转成功、循环4次请求后停止。Python完整57项通过，未实测源站访问。
此处仅限制URL主机/协议与跳转；允许域名的DNS解析结果绑定及私网重绑定防护尚未完成，不宣称全面SSRF验收通过。合法跨域迁移应先验证来源配置再扩展白名单，不自动放宽。

### DATA-01启动迁移纠错（重要）

此前pom只有flyway-core/mysql；实际本地依赖检查确认Boot4.0.5的FlywayAutoConfiguration位于spring-boot-flyway独立模块。已将core直接依赖替换为Boot同版本管理的spring-boot-starter-flyway，保留mysql扩展，不升级框架。
新增FlywayStartupConfigurationTest验证应用上下文创建Flyway和flywayInitializer，并在启动时调用迁移策略；显式enabled=false不调用。DataSource与迁移策略为mock，刻意不连接数据库。该测试防止只手动Flyway.migrate的旧测试掩盖应用自动迁移未注册问题。
实际4项迁移布局/自动配置测试通过；完整scripts/check.ps1通过，后端80项中7项隔离库条件未配置而跳过。手动SQL迁移已有历史实测，本次自动初始化尚未对空MySQL启动应用实测，不能混为一项证据。新增starter意味着正常启动将执行配置的Flyway迁移，切勿在未经批准的现有业务数据库上启动应用。无服务器或业务数据库操作。

### DEPLOY-01：构建上下文密钥与状态隔离

原COPY ingest ./会把本地.env及开发杂物送入镜像上下文。本轮.dockerignore补齐任意层级.env/.env.*、私钥、SQLite含WAL文件、日志、缓存/虚拟环境排除；采集Dockerfile改为显式代码目录和runtime/*.py，不复制整个开发目录。没有读取或输出任何真实密钥。
新增静态检查scripts/check-build-context.py，阻止必需排除项缺失、否定规则重新包含密钥、采集镜像宽泛COPY/ADD。新增3项静态配置回归，统一scripts/check.ps1已接入并通过。
这是静态防护，不是实际镜像层/构建上下文扫描：镜像仍未构建、digest和依赖锁仍待验证，不据此宣称无密钥镜像已获验收。没有启动Docker、安装软件或部署。

### QUALITY-01：落实样本组成与正确率口径

原evaluator只检查总数160，重复样本、单游戏样本也可能误判通过，且只看ACCEPT标签而不看游戏/类型是否正确。本轮加入唯一ID/内容去重、frozen声明、来源/语言标签、四游戏各20正例、80负例或审核边界、中英文覆盖门禁；自动通过precision必须游戏与类型也匹配。覆盖率分母改为明确正例，不把REVIEW标签当正例；报告明确分子/分母、逐游戏正例数、审核率及datasetIssues。
新增4项合成单测防止重复凑数、错游戏类型拿高分、分母误计和标签拼写错误。5项评估器测试通过；完整scripts/check.ps1通过，Python64项。
未创建真实160条冻结语料，也未宣称质量达标。当前frozen为样本声明而非独立审核证明；调参集交叉检查、来源逐项覆盖、真实人工标签与影子运行仍待实现。测试fixture只能验证程序拒绝不合格数据，不能作为自动发布批准。

### SEC-01：旧公开入口补齐审核状态约束

旧列表/标签搜索/轮播/热榜/热度重建部分SQL仍只检查legacy state。本轮统一为state='已发布'且workflow_status='PUBLISHED'，调用者传null或草稿筛选也不能移除公开约束；管理员待审核查询仍走独立AdminArticleMapper，不受此处影响。
新增MyBatis真实XML/注解SQL渲染测试，覆盖8条公开查询，防止任一入口遗漏工作流条件。实际PublicArticleSqlBoundaryTest、ArticleVisibilityServiceTest、SecurityBoundaryTest合计14项通过。SQL渲染不是MySQL实际执行，新的过滤条件仍需在下次允许的隔离库测试中覆盖矛盾状态数据；不声称本轮已完成数据库实测。
兼容注意：旧管理写接口只改state时不再足以公开文章，正式发布必须走同步更新工作流并记录审计的审核路径；旧接口退出前需继续迁移检查。

### SEC-01：本人资料对象权限与用户目录保护

发现UserServiceImpl.update直接按客户端提交ID写入，可越权修改他人昵称/邮箱。本轮在服务层绑定已认证claims：缺用户身份401，ID不匹配403，只有本人ID才能写入。mapper不更新角色/密码，未扩展管理员代改资料能力。
/user/list从普通登录可读收紧为SUPER_ADMIN，普通用户和内容管理员均禁止枚举用户目录；匿名仍401。
实际UserProfileOwnershipTest与SecurityBoundaryTest合计13项通过，覆盖他人ID零数据库调用、无身份拒绝、本人更新成功及目录权限。未连接真实用户库、未修改现有账户，仍需持续审查其余敏感接口。

### SEC/API错误响应去敏

未知异常不再把exception.message（可能包含SQL/连接信息）返回客户端，不再直接printStackTrace；日志仅记录异常类型和v1 traceId。JSON解析、缺参、类型不匹配返回400；v1未知错误500仍保留统一封装和traceId。业务显式ResponseStatusException语义保持不变。
新增3项错误去敏回归及1项真实MVC坏JSON400/零receipt业务调用测试。先运行17项相关测试通过，随后完整scripts/check.ps1通过。未触及真实密钥或业务数据；应用其他日志/外部SDK日志仍需后续统一审计，不能认为所有日志泄露风险都已解决。

### ING-02：认证/背压停批次与坏事件隔离

400/409/413/422现在持久进入QUARANTINED，保留载荷但不自动重投；未解决预算改为全部非ACK事件计数，避免隔离记录绕过outbox软上限。401/403/429/503停止当前整批投递，并让主流程立即停止新增采集，不对后续事件继续刷请求。错误记录只保留状态码。
新增2项测试覆盖8种HTTP状态，确认重启后隔离保留、隔离计入预算、认证/背压仅请求一条且全部事件未丢。实际scripts/check.ps1全通过，Python66项。
仍需完善持久next-at退避、全局认证暂停/告警和受控人工重放工具；当前只是本次运行停批次，不能声称跨调度周期已永久暂停或已具备运营重放闭环。未清空任何真实队列。

### ING-02：跨运行周期的认证暂停与背压退避

outbox增加delivery_control持久表（不改/删已有事件），401/403记录无限期认证暂停，重启/更换token本身不会自动恢复；429/503记录resume_at，至少等待900秒并尊重合法数字Retry-After更长秒数。启动投递器先查控制状态，有暂停则不发送请求，主流程沿用halted_reason停止新增采集。
新增2项SQLite真实文件回归：认证暂停跨重启及长时间仍保持；429等待期零HTTP，到期以同eventId重试并确认。完整scripts/check.ps1通过，Python68项。
尚需受控人工恢复/重放入口及告警渠道。当前不得直接删delivery_control或改数据库模拟恢复；完整HTTP-date形式Retry-After、网络失败指数退避和重试上限仍待补齐，不能宣称所有重试场景完成。

### ING-02运营排查：只读队列检查入口

新增inspect_outbox.py，SQLite URI mode=ro只读取已有文件，输出状态计数、全部未解决数量、认证/背压暂停和有界事件元数据；不打印载荷/原文/服务令牌，也不回显旧版本任意错误摘要。缺文件不会创建新状态。用法与禁止绕过暂停的边界记录于docs/OUTBOX_OPERATIONS.md。
实际3项临时SQLite测试通过：检查前后数据库字节一致、载荷敏感标记不输出、缺库不创建、有界列表与旧暂停详情脱敏。尚未实现人工恢复/重放，不能把此只读工具称为运营闭环完成。本轮不读取真实运行数据库或连接外部服务。

### DEPLOY/OPS：内存预算配置与设计对齐

发现Compose的m后缀配置与文档3.776GB表述既有单位混淆，也未使用设计10.1的具体预算。本轮改为精确整数bytes：backend/mysql各1,200,000,000，ingest450,000,000，redis200,000,000，frontend100,000,000，合计3,150,000,000；保留850,000,000用于管理/峰值观测。memswap_limit同值，不用额外swap掩盖；CPU配置合计3核。Redis内部maxmemory96MiB、Java堆640MiB、MySQL buffer pool512MiB及40连接与设计默认值对齐；容器日志加单服务10m×3文件保留。
新增结构化资源检查和4项静态回归，防止漏服务/单位含糊/超限/swap或CPU扩大预算。check-deploy.ps1静态Compose解析通过，未构建或启动容器。修正README旧单位表述。
仍未完成cgroup inspect、实际峰值、Docker管理开销、备份和24小时观察；这些配置绝不等于已保证项目≤4GB或≤10GB。未改服务器或已有项目。

### ING-02：单交付事件字节上限

outbox入队前按包含eventId的完整JSON UTF-8字节计算，超过256,000字节明确PayloadTooLarge，不截断正文或摘要、不插入事件、不伪ACK；拒绝NaN等非标准JSON值。已有待交付事件不因新载荷失败而被修改。保留原有序列化排序及格式，避免改变历史载荷身份语义。
实际OutboxPayloadBoundsTest和OutboxIdentityTest合计7项通过：中文多字节超限、精确边界及超1字节、非有限值拒绝、旧数据保留和并发幂等。
剩余事项：当前超限错误会交给主流程报告，但仍缺持久失败登记和人工处置界面；50MB outbox+运行状态总软预算、服务端HTTP请求体字节限制也未完成。不能用单事件限制代替整个项目磁盘限额证明。

### 内部采集入口：解析前身份/请求字节门禁

新增IngestRequestBoundaryFilter，在内部采集JSON反序列化前检查独立服务token；无效凭据不读取body。POST已声明长度或实际流读取超过256,000字节均413，长度未知也不绕过；非identity Content-Encoding返回415，避免压缩载荷绕过该边界。合法body以有界缓冲交给同步MVC解析。控制器原有身份校验保留作为第二道防线。
新增5项过滤器测试及既有内部控制器测试合计12项通过，随后统一scripts/check.ps1通过。Python投递将415加入永久坏事件隔离分类，其2项错误类别测试通过。该证据未包含真实恶意慢连接、网关/容器请求体峰值或异步Servlet路径压测；内网不应经公网代理暴露的部署要求不变。没有实际部署或调用服务凭据。

### 来源日期精度贯通（V7尚未真实迁移验证）

Apex列表只有年月日，旧代码将其转换成无offset午夜时间，既伪造时间精度又不能通过内部入站校验。本轮新增originalPublishedDate：Raw/Processed模型、Apex适配器、模型结果元数据、outbox事件、Java DTO/Article/Mapper、公开DTO和详情显示贯通。完整时间与纯日期互斥，不猜测时区；纯日期显示“仅日期”。V7新增可空DATE列，不转换既有数据。
新增2项入站测试验证日期可接受且不生成timestamp、日期冲突/不存在日期400；新增前端日期精度展示测试；Apex fixture测试确认只有date而timestamp为null。scripts/check.ps1完整通过，OpenAPI和迁移资源版本断言同步。
新增V7及存储字段本轮尚未在真实MySQL执行，隔离库测试仅更新预期版本并按条件跳过；不能把资源/编译通过当作迁移SQL验收。旧originalPublishedAt时区规范仍需后续统一，不在本轮偷偷改历史时间。

### WEB-01：恢复已有评论只读分页

新增公开评论分页DTO/接口，仅返回评论ID、显示名、纯文本内容、父评论ID与时间；每页上限50，读取前检查文章已发布，SQL同样约束双发布状态。摘要详情新增已有评论列表、分页、失败提示，评论提交仍明确关闭；Vue插值展示内容，不执行历史HTML。
新增3项后端控制器回归、2项前端纯文本/错误态测试。首次测试修复Vitest beforeEach误返回mock函数导致结束时执行的问题；一次Node代理检查进程异常退出，先健康检查后单独重跑通过，再完整scripts/check.ps1 -Browser通过（前端39项）。OpenAPI路径登记通过。
新评论SQL尚未真实MySQL实测；浏览器本轮为fixture后端，不宣称已完成真实评论读取E2E。运营处置/限流门禁未解除，无评论写入功能重新开放。

### ADMIN-01：状态分页、下架与审核冲突交互

管理页从只展示前50篇待审核改为20条分页及待审核/已发布/拒绝/下架筛选。仅待审核展示通过/拒绝，已发布展示下架；支持审核说明并提交当前revision。审核请求中禁止重复点击，409或未确认错误要求刷新后重新审核，不自动重复写入。来源暂停/恢复必须检查业务code，原文链接只允许无凭据HTTP(S)。列表请求使用代次防止旧响应覆盖新筛选。
新增3项组件回归：已发布下架入口/版本与说明传递、防双击、409后刷新恢复。前端42项测试通过，生产build通过（既有大包警告）。后台编辑、审计读取、来源/运行完善和真实认证/数据库审核链路仍需继续，不视为管理员模块整体完成。

### ADMIN-01：审核操作者身份与审计归属（2026-09-22）

审核通过、拒绝、下架现在在任何数据库读取或写入之前，强制要求 JWT 认证过滤器放入当前请求的正整数 `userId`。没有声明、声明类型错误或非正 ID 一律返回 401；不会触发文章状态更新，也不会写出 `actor_user_id = NULL` 的新审计记录。旧迁移中的可空列保持不变，以兼容历史数据。

新增服务层 6 项与 MVC 边界 2 项回归：正常审核的审计会记录实际操作者；仅具 CONTENT_ADMIN 角色、但没有经 JWT 过滤器验证的 claims 时不能审核；缺失/异常 claims 在任何 mapper 调用前被拒绝；过期 revision 仍不会写审计。先运行了两个定向测试，再运行 `scripts/check.ps1`，完整基线通过（其中 Python 78 项通过）。

这不是“管理员审核模块已完成”或“真实登录链路已验收”：当前 MVC 边界通过受控 claims fixture 模拟过滤器语义；真实 JWT + Redis、真实隔离 MySQL 的审核事务/审计查询、管理员账号初始化与管理页面完整浏览器链路仍需继续。本轮未连接服务器、未部署、未提交或推送 Git。

### ADMIN-01：实际 JWT 过滤器到审核服务的身份链（2026-09-22）

补充了 `JwtAuthenticationFilterTest`，不再只用管理端 MVC 的手工 claims fixture 证明身份语义。测试覆盖：Redis 中仍有效的 JWT 会在请求链执行期间同时建立带角色的 Spring Security 主体和相同的 `ThreadLocal userId`，该 ID 正是审核服务写入审计所使用的身份；请求结束后 claims 会被清理。`userId` 类型异常或 Redis token 不匹配时不会认证，也不会把 claims 交给后续审核代码。

定向 JWT 过滤器测试通过，随后再次运行 `scripts/check.ps1`，所有选定基线通过（Python 78 项通过）。这仍是本地 mock 的 JWT/Redis 过滤器测试，不是与真实 Redis 或服务器账号的集成验收；本轮未连接 3306 上已有 MySQL，未复用任何现有项目环境，未部署、提交或推送。

### ADMIN-01：文章级审核历史可读（2026-09-22）

新增管理员专用 `GET /api/v1/admin/articles/{id}/audit`，按创建时间与审计 ID 倒序分页读取单篇文章的审核历史。返回动作、前后状态、修订、原因、创建时间和操作者用户 ID；不联表返回用户资料，更不会返回密码、token 或请求头。非法文章 ID/分页参数在读取前返回 422；不存在文章返回真实 404。该读取入口沿用 `/api/v1/admin/**` 的内容管理员/超级管理员授权，写审核仍额外要求有效 JWT claims。

管理页每篇文章增加 AUDIT 按钮，可查看、翻页和关闭只读审核记录；空记录、加载失败和加载中都有明确状态。审核完成后如果当前正查看该文章，会重新读取历史，避免 UI 继续展示旧修订。新增后端服务/控制器回归和前端审计面板回归；`scripts/check.ps1 -Browser` 完整通过（含 OpenAPI、Maven、前端 build/Vitest、浏览器 fixture、Python 78 项）。

目前这证明的是契约、页面和 mock/fixture 行为。审核历史 SQL、V7 DATE 迁移与审核写入仍须在**新的独立本地 MySQL**中做真实事务验证；不能使用本机 3306 已有 MySQL，也没有部署、连接服务器、提交或推送。

### MYSQL-VERIFY：独立实例的 V7 与管理员审核事务（2026-09-22）

为避免复用已有项目环境，临时初始化了一套新的 MySQL 8.0.45，只监听 `127.0.0.1:33067`，数据目录独立，InnoDB buffer pool 限为 128 MiB、最大连接 15。未读取、连接或修改本机既有 MySQL 3306。`MySqlFlywayIntegrationTest` 在该实例实际执行 V1–V7、二次迁移幂等、legacy V1 升级，并额外确认 V7 的 `article.original_published_date` 为可空 DATE。

新增 `AdminModerationMySqlTransactionTest`（只接受上述精确 localhost URL）：Spring + Flyway + MyBatis 在单独创建、随后 DROP 的 `gameboi_admin_moderation_test` 上证明审核通过会实际写入含 actor ID 的 audit，并证明当 audit detail 超过数据库 1000 字限制导致插入失败时，文章 workflow/revision 会随同回滚、不会残留 audit。测试后已确认测试库不存在，并停止仅属于 33067 的 MySQL 进程；数据目录暂保留，未递归删除。

同时尝试既有 `IngestReceiptMySqlTransactionTest`。迁移本身完成，但当前 Windows/JDK 中嵌入式 Tomcat 随机端口启动报 loopback `SocketException: Invalid argument: connect`，因此该 HTTP 集成测试本轮不能列为通过；它不影响本轮已独立通过的 Flyway 与管理员审核真实数据库证据。之后 `scripts/check.ps1 -Browser` 完整通过。未部署、未连接服务器、未提交或推送。

### SOURCE-01：Delta Force Garena 区域官方来源（2026-09-22）

新增 `delta-force-garena-official` 适配器。对 `deltaforce.garena.com/en/` 做受控请求：列表与一篇 S11 Patch Notes 详情均返回 200，列表发现 3 个同源 `/en/news/all/` 官方链接。离线 fixture 回归确认可保留官方 URL、标题、正文和仅日期 `2026-09-04`；规则分类为 `delta-force / patch_notes / ACCEPT`。V8 仅将该来源登记为 `FETCH_VERIFIED` 且 `auto_publish_enabled=0`，明确不是 SHADOW 自动通过，更不是自动发布。

该来源属于 Garena 区域英文入口，页面与摘要都必须保留地区/平台限定，不能冒充全球或国服覆盖。V8 已在独立 localhost MySQL 上实际迁移并通过 legacy upgrade 验证；随后 `scripts/check.ps1` 全量通过，Python 测试增至 79 项。CS2 当前网络不稳定、Delta 的影子运行/更新与重复样本、四游戏 6–8 个高质量来源及质量冻结集仍未完成。

### SOURCE-01：CS2 Steam 官方源混源修复（2026-09-22）

重新受控验证 Steam News API：HTTP/1.1 与 HTTP/2 都返回 200；实际适配器抓到 9 条第一方公告。验证时发现 API 会把第三方 feed 混在 appid=730 新闻中，旧代码会把它们错误标成“官方公告”。现已收紧为仅接受 `Community Announcements` / `steam_community_announcements` 标签，且 URL 必须是 Steam community announcement 的固定前缀；第三方 PlayGround 等 feed 有专门回归用例，确保不会进入 CS2 官方来源。

新增 V9 将 `cs2-official` 登记为 `FETCH_VERIFIED`、24 小时频率、每批 10 条、自动发布关闭。四个首期游戏现在至少各有一个官方或明确区域官方的 FETCH_VERIFIED/SHADOW 来源，但这**不等于覆盖完成**：仍需 6–8 个质量来源、逐来源影子运行、更新/重复样本、Delta 的区域边界说明以及 160 条质量冻结集。`scripts/check.ps1` 完整通过，Python 测试为 80 项；随后 V9 已在独立 MySQL 上实际执行：V1–V9、二次幂等和 seeded V1 legacy upgrade 均通过；测试进程已停止。

### SOURCE-01：VALORANT Esports 官方赛事资讯（2026-09-22）

新增 `valorant-esports-official`：受控请求 `valorantesports.com/en-US/news/` 和一篇 Champions Shanghai 官方赛事详情均为 200。适配器可抽取文章标题、正文、官方 URL 和仅日期 `2026-09-16`，不伪造时间或时区；规则补充 VCT、Champions、Game Changers 等赛事词，以保证明确的 VALORANT 赛事内容能进入 `esports` 类型。V10 将该来源登记为 `FETCH_VERIFIED`，12 小时频率、每批 10 条、自动发布关闭。

这使当前可用的官方/区域官方来源方向增加到 5 个，但仍不足设计所需 6–8 个，并且来源未进入影子发布阶段。赛事票务、地域限定、直播/即时比分等不在首期范围的内容仍应进入审核或拒绝；随后 V10 已在独立 MySQL 上实际执行：V1–V10、二次幂等和 seeded V1 legacy upgrade 均通过；测试进程已停止。`scripts/check.ps1` 完整通过，Python 测试为 81 项。

### ING-02：持久化退避与重试上限（2026-09-22）

outbox 增加 `next_attempt_at`，旧 SQLite 文件会在打开时就地补列，不改变既有事件身份。READY 事件只有到期才会被再次投递；网络错误按 60 秒起、最多 1 小时的指数退避，同时持久暂停整批投递，进程重启不会立刻重试。429/503 支持数字秒数和 HTTP-date 两种 `Retry-After`，并保留至少 15 分钟的全局退避；401/403 仍为人工恢复的永久暂停。

每个事件最多 5 次未确认投递，随后进入 QUARANTINED，仍计入 pending 容量而不删除。400/409/413/415/422 仍直接隔离。新增覆盖网络失败到期重试、HTTP-date、跨重启调度和重试上限的 3 项回归，原有投递暂停/错误类别/幂等回归同步通过；`scripts/check.ps1` 完整通过，Python 测试为 84 项。

本轮没有实现人工重放/恢复命令、失败事件运营界面或告警渠道；这些隔离事件不能自动丢弃，也不能因为有退避就把采集自动发布打开。

### QUALITY-01：候选集与人工标注隔离（2026-09-22）

新增 `quality/prepare_candidates.py` 和 `quality/CANDIDATE_LABELING.md`。工具只接受来源、语言、标题、必要摘录、原文 URL，规范化、去重并写出 `split: candidate` / `PENDING_HUMAN_REVIEW`；最多保留 10,000 个摘录字符，拒绝覆盖已有候选文件。它不生成 `expectedDecision`、游戏标签或内容类型，因此不能伪造成 frozen 集。

已从五个已验证官方/区域官方 fixture 生成 5 条真实来源候选（位于 `quality/candidates/official_seed_2026-09-22.jsonl`），仅供人工标注，不是评测样本。评测器现明确拒绝候选记录作为 frozen 输入；旧 smoke/frozen 兼容指标测试仍可运行。新增 2 项候选流程回归，质量相关 7 项回归通过，随后 `scripts/check.ps1` 完整通过（Python 86 项）。

距 QUALITY-01 关闭仍缺人工审核后的 160 条冻结样本、每游戏 20 条明确正例、至少 80 条负例/边界样本、来源影子运行和逐来源质量报告。没有这些真实人工证据，任何来源都不得转为自动发布。

### SOURCE-01：五来源受控 dry-run 预检与 CS2 上限修复（2026-09-22）

以单来源、每轮最多一个候选的方式依次运行 VALORANT 更新、Apex、Delta Force Garena、CS2、VALORANT Esports 的 `--dry-run`。五个来源均完成至少一条目标游戏候选的实时列表/详情或 API 解析；dry-run 没有创建 AI、Redis、SQLite state/outbox、去重、统计或内部投递写入。

预检发现 CS2 Steam API 虽已过滤第三方 feed，但旧循环会在过滤前截断候选，可能因首条第三方 feed 导致本轮零产出，或忽略 `MAX_ARTICLES_PER_RUN` 的语义。现已改为先过滤到第一方 Community Announcement，再按上限停止；新增回归和实时 one-candidate dry-run 均确认只处理一条第一方 CS2 更新。`scripts/check.ps1` 完整通过，Python 测试为 87 项。

这些结果仍然不是影子运行：没有 AI 结果、没有内部 receipt、没有人工审核，任何来源依然不得自动发布。

### ING-02：来源 DNS 私网地址预防（2026-09-22）

所有 BaseSpider 请求与每次重定向现在在发送前都会解析目标主机；只要解析结果为空、失败，或任一 IP 属于私网、回环、链路本地、保留地址等非 global 地址，就在 transport 之前拒绝。原有协议、端口、同源跳转、账户密码 URL 与 2 MB 解码上限继续保留。新增测试证明私网 DNS 结果不会触发 HTTP 请求，并回归相对跳转、跨域跳转和响应上限。

这是一道请求前 SSRF 防线，但还不能宣称完全解决 DNS rebinding：httpx 建连时仍由其自身解析器选择地址，尚未将已验证 IP 与 TLS/SNI 建连强制绑定。这个剩余风险继续保留在爬虫上线门禁中。`scripts/check.ps1` 完整通过，Python 测试为 88 项。

### ING-02：受控隔离事件重放（2026-09-22）

新增 `runtime/requeue_outbox.py`，只允许对已人工核查的单条 QUARANTINED 事件进行恢复。必须同时指定原 eventId、1–200 字恢复原因和 `--confirm`；恢复保持原 eventId，重置该事件 attempts、重新置为 READY，并在 SQLite `outbox_operator_audit` 写入动作与原因。READY、ACKED、缺失事件均拒绝，不能批量恢复。

恢复命令绝不清除 HTTP401/403/429/503 的全局投递暂停，因此更换凭据、等待退避或排查服务端后仍须按独立暂停策略处理。新增两项回归验证状态限制、审计写入和暂停不被绕过；`scripts/check.ps1` 完整通过，Python 测试为 90 项。仍缺运营告警渠道、管理后台处置页面和受控批量恢复审批，隔离事件不会自动删除。

### SOURCE-01：ALGS 官方赛事资讯（2026-09-22）

新增 `algs-official`，来自 EA 官方 Apex Legends Global Series 新闻入口。受控列表和 Split 2 Pro League 详情均返回 200；离线 fixture 能稳定抽取标题与正文，规则将 ALGS / Pro League 识别为 Apex 电竞内容。V11 将来源登记为 `FETCH_VERIFIED`，12 小时频率、每批 10 条、自动发布关闭。

至此首期已有 6 个官方或明确区域官方的来源方向，达到设计的最低数量门槛；但数量不等于质量完成。六个来源仍缺影子运行、更新/重复样本、人工质量集与逐来源报告，ALGS 的赛事票务和地区限定内容也必须先人工审核。`scripts/check.ps1` 完整通过，Python 测试为 91 项；随后 V11 已在独立 MySQL 上实际执行：V1–V11、二次幂等和 seeded V1 legacy upgrade 均通过；测试进程已停止。

### SOURCE-01：ALGS dry-run 与质量候选（2026-09-22）

ALGS 已完成单候选受控 `--dry-run`，实际解析到 Apex 赛事候选；没有 AI、Redis、SQLite state/outbox、内部投递或统计写入。ALGS fixture 的必要摘录另存为一条 `PENDING_HUMAN_REVIEW` 质量候选，和既有 frozen 评测严格隔离。

六个来源现在都已完成受控解析预检，但这仍不能替代 SHADOW。没有 AI 预算/密钥、内部隔离运行地址和人工审核前，不执行真实影子交付，也不改变任何来源的自动发布状态。

### ING-02：outbox 与运行状态 50 MB 组合上限（2026-09-22）

Outbox 现在把 outbox SQLite、RunState SQLite 及其 WAL/SHM/journal 文件作为一个组合运行状态口径计量，默认上限为 50,000,000 bytes，可通过环境变量显式收紧。每次 enqueue 会先为 UTF-8 事件和保守 SQLite 页面开销预留空间；不足时抛出容量错误，主采集流程打印 PAUSE 并停止当前来源，既有未 ACK/QUARANTINED 事件不删除、不截断、不重写。

新增两项测试覆盖 state+outbox 组合占用阻止新事件且不落库、WAL 等伴随文件去重计量；`scripts/check.ps1` 完整通过，Python 测试为 93 项。该门禁是本地运行状态软上限，不能替代服务器卷配额、Docker 镜像/日志/备份的 10 GB 总盘实测。

### ING-02：MockMvc HTTP 到真实 MySQL 交付链（2026-09-22）

新增 `IngestReceiptMySqlMockMvcTransactionTest`，解决当前 Windows/JDK 下真实 Tomcat 随机端口 loopback 启动异常导致原 HTTP 集成用例无法稳定运行的问题。该测试仍经过 Spring MVC 的内部 token 边界、控制器、Flyway、MyBatis 和真实隔离 MySQL：首次内部 POST 返回 201、相同 eventId 重放返回 200 且不重复写文章；未发布公开详情为 404；测试发布后公开列表返回 sourceName、多游戏与摘要且不泄露正文。

这不是 TCP socket 或 Nginx/浏览器实测的替代品，原 Tomcat 随机端口问题仍记录为环境限制；但它补足了无需开放端口的 HTTP MVC→数据库真实事务证据。专用测试库已 DROP、33067 已停止，未接触 3306/服务器。`scripts/check.ps1` 完整通过。

### OPS-01：备份恢复门禁与演练清单（2026-09-22）

新增 `BACKUP_RECOVERY_RUNBOOK.md`，明确备份对象、10 GB 盘占用口径、禁止覆盖其他项目的独立恢复原则、B02/B06 前置、备份前 ingest 停新增原则和恢复演练的可验收项目。文档不包含任何服务器密码、云端位置、生产删除命令或执行授权。

这只是把恢复能力变成可审计门禁，不是备份/恢复已经完成。异地目标、加密/保留策略、Docker 空环境、实际 restore 演练、耗时及 cgroup/磁盘实测仍需要批准后的独立环境验证。

### 本地验收启动尝试（2026-09-22）

按用户授权启动了项目专用的本地 MySQL（仅绑定 `127.0.0.1:33067`），创建独立 `gameboi` 数据库并由 Flyway 实际应用/校验 V1–V11。前端 Vite 已启动并在 `127.0.0.1:5173` 返回 HTTP 200，根页面包含 Vue 挂载节点。

后端在相同隔离数据库配置下完成数据库连接、Flyway 校验和应用上下文初始化，但嵌入式 Tomcat 在本机 Windows/JDK 环境再次因 `Unable to establish loopback connection` / `SocketException: Invalid argument: connect` 失败，8080 未监听；尝试切换 SelectorProvider 也未解决。前端经 Vite 代理请求 `/api/v1/games` 当前返回 502，因此本次仅可进行静态前端页面检查，不能作为真实前后端浏览器链路通过证据。未接触 3306、现有 Redis、服务器或其他项目环境。

### 本机 Tomcat loopback 启动阻断修复（2026-09-22）

定位为本机 Windows/JDK 在默认长临时目录下创建 NIO selector 的 loopback pipe 失败；这不是数据库迁移或端口占用问题。将**后端进程级** `TEMP` 与 `TMP` 指向短路径 `C:\jtmp` 后，嵌入式 Tomcat 能稳定绑定 8080。该设置仅作用于本地开发启动进程，没有写入系统环境变量、生产配置或 Git。

已在独立 MySQL `127.0.0.1:33067` 上重新启动后端；Flyway V1–V11 校验通过，Tomcat 已实际监听 8080。随后实际验证：`GET http://127.0.0.1:8080/api/v1/games` 为 HTTP 200，Vite 页面 `http://127.0.0.1:5173/` 为 HTTP 200，Vite 代理的同一 API 也为 HTTP 200。没有连接 3306、已有 Redis 或服务器。

本机尚未安装项目专用 Redis，因此为避免复用 6379，当前后端指向未运行的独立端口 36379；Redis 依赖的定时任务会产生连接失败日志。这不影响本次公开游戏列表与前端页面验收，但不能视为 Redis/登录/互动/采集完整链路通过。后续应在项目隔离运行时中提供独立 Redis，再执行完整链路验证。

### WEB-01 / ADMIN-01：FPS 电竞资讯视觉重构启动（2026-09-22）

产品负责人明确批准全站由像素/终端风重构为浅色 FPS 电竞资讯媒体。约束为：白/雾灰/墨黑基础色、竞技红重点色，中文和系统字体优先，桌面端编辑精选首页，独立品牌落地页，管理页同步统一，移动端保障核心流程；不新增外部/生成图片或在线字体，不改变接口、路由、授权、审核和四游戏白名单。开始前已将该用户批准及验收要求同步到 PROJECT_DESIGN.md、DEVELOPMENT_PLAN.md 和 feature_list.json；后续需以本轮 Vue 构建与测试为准。

### WEB-01 / ADMIN-01：FPS 电竞资讯视觉重构验收（2026-09-22）

已实现浅色 FPS 电竞资讯媒体设计令牌（白/雾灰/墨黑 + 竞技红）、独立 GAMEBOI 品牌落地页、桌面优先编辑精选首页、资讯列表和详情阅读布局，并统一账户、收藏与审核后台的组件外观；未修改路由、接口、认证授权、审核流程、四游戏白名单，也未新增外部/生成图片或在线字体。当前运行验证：在 gaming-news 执行 npm test，12 个测试文件、43 项测试全部通过；npm run build 通过。Vite 报告 index 压缩前 chunk 约 1.31 MB（gzip 约 350.94 kB）超过 500 kB 警告阈值，未阻塞构建，后续可在独立性能任务中评估代码分包。

### SOURCE-01：小黑盒话题入口匿名初检（2026-09-22）

按用户建议，对小黑盒 `app/topic/link` 做了不携带 Cookie、不登录的受控单请求。入口返回 HTTP 200 的 SPA 页面壳，未出现登录标记，但没有返回可解析的文章列表或四游戏筛选数据；单次公开分享详情旧路径探测返回“该功能正在维护中”。因此尚不能证明存在稳定、公开、免登录的文章列表/详情接口。

来源已仅登记为 `xiaoheihe-community` 的 CANDIDATE：未新增爬虫、数据库 seed、影子运行或自动发布。后续须先确认公开访问规则/展示许可并验证稳定栏目和详情接口；默认只允许存必要元数据，展示必须标注“文章来源：小黑盒社区”与原文链接，不得未经许可复制全文、图片或评论。

### XHH-01：小黑盒社区补充来源实施计划确认（2026-09-22）

用户确认该来源仅采用“必要索引信息 + AI 短摘要”展示，并固定标注“文章来源：小黑盒社区”及原文跳转；所有候选必须进入管理员待审核，官方来源优先去重。技术边界锁定为只使用公开、免登录、无需绕过访问限制的页面或接口；不登录、不保存 Cookie、不逆向私有 App 接口、不伪造签名、不绕过验证码/反爬，验证不到稳定公开入口即暂停。

计划已写入 `docs/DEVELOPMENT_PLAN.md`，并新增 `XHH-01` 任务。AI 摘要实际调用默认关闭；B04 未确认前不调用付费模型。后续启用时采用单候选一次、无自动重试、每日 20 篇、输入 1,200 字符、输出 220 中文字符的 fail-closed 初始上限；失败/超限仅保留待审核，不显示原始正文。当前尚未新增适配器、数据库 seed、影子运行或自动发布。

### XHH-01：计划落盘前基线验证（2026-09-22）

将小黑盒社区受控接入计划、严格 AI 摘要限额和 `XHH-01` 任务写入权威计划/状态后，执行项目根目录 `scripts\check.ps1`。静态资源预算、Docker build context、OpenAPI、Maven 后端测试、前端构建与 Vitest、Python compile/import/unittest 全部通过；Python unittest 为 93 项。该验证不代表小黑盒来源已可用，未进行额外爬取、AI 调用、数据库 seed、影子运行或自动发布。


### XHH-01：候选来源 fail-closed 首个实现闭环（2026-09-22）

在分支 `codex/xiaoheihe-community-source` 上新增 `ingest/source_registry.py`，把 `xiaoheihe-community` 显式登记为候选来源，包含四游戏范围和“公开免登录列表/详情及内容使用条件未验证”的阻断原因。`ingest/main.py --source xiaoheihe-community` 现在会在 HTTP、Redis、AI、outbox 初始化前输出 CANDIDATE 并以退出码 2 停止，避免有人因来源名称存在而误启动采集。

`ingest/settings.py` 增加小黑盒专属摘要开关与严格默认上限：默认关闭、每日 20 篇、输入 1,200 字符、输出 220 中文字符、单候选一次。当前没有调用 AI，也没有放宽通用 DeepSeek 的重试策略；在来源仍未验证且 B04 未确认时，这些配置只是 fail-closed 契约。新增 `ingest/tests/test_xiaoheihe_candidate.py` 覆盖无网络/无 Redis/无 AI/无 outbox 的阻断行为和默认上限。相关 7 项测试与完整 `scripts\check.ps1` 均通过，Python unittest 总数为 95。

### WEB-01 / ING-02：站内全文阅读打通（2026-10-16）

用户明确变更阅读策略：入库侧保存爬取正文，公开详情按 id 读取正文并作为**纯文本**返回，前端以文本渲染。该决定已同步写入 `docs/PROJECT_DESIGN.md` §4.3 与 `docs/api/openapi.yaml` 的 `content` 描述。

**入库侧**：`IngestReceiptServiceImpl.resolveOrCreateArticle()` 的 `article.setContent(...)` 从摘要改为爬取正文（新增 `storedBody(event)`：正文为空时回退摘要，因 `article.content` 为 `NOT NULL`）；`readingMode` 由 `EXTERNAL_SUMMARY` 改为 `FULL_TEXT`，`Article` 实体默认值同步改为 `FULL_TEXT`（`ReadingMode` 注释更新）。已确认 `article.content` 本就是 `MEDIUMTEXT`（V1），**无需数据库迁移**；`article_content.sanitized_html` 的内容哈希副本保留不变。

**读取侧（分层重构）**：新增 `ArticleMapper.selectPublishedContentById`，显式投影标题、正文与展示元数据（只返回已发布）。新增 `PublicArticleReadService` 接口与 `PublicArticleReadServiceImpl`，承载全部原控制器私有逻辑：分页/游戏/内容类型白名单校验、摘要组装、游戏与来源名批量查询、以及正文文本化（块级标签转换行 + 单次实体解码，避免二次解码）。`PublicApiV1Controller` 从 185 行降到只接收参数与包装 `ApiResponse`，不再依赖 `ArticleMapper`/`GameMapper`/`ArticleSourceMapper`/`ObjectMapper`。`InternalIngestController` 的载荷 SHA-256 指纹移入 `IngestReceiptService.payloadHash(IngestEvent)`。

**性能修正**：`listPublishedByFilters` 由 `select distinct a.*` 改为显式列投影并**排除 content**——正文入库后列表若继续拉 `MEDIUMTEXT` 会显著放大响应。

**前端**：详情路由 `/article/:id` 与 `getPublicArticleDetail` 调用原已存在，无需新增。`ArticleDetailView.vue` 改为渲染 `article.content`（`white-space:pre-wrap`，用插值而非 `v-html`，无 XSS 面），无正文时回退摘要；提示文案更新为“内容整理自官方来源，如有差异以原文为准”。

**本轮实际运行的验证**（隔离 MySQL `127.0.0.1:33067`，未接触 3306/6379；实例已停止）：
- 后端 `mvnw -o test`：**123 项通过、0 失败、10 跳过**（跳过项为 MySQL 门禁测试）。
- 门禁测试在隔离 MySQL 上实跑：`IngestReceiptMySqlTransactionTest` 5/5、`IngestReceiptMySqlMockMvcTransactionTest` 1/1、`MySqlFlywayIntegrationTest` 2/2、`AdminModerationMySqlTransactionTest` 2/2 —— **10 项 0 跳过**，证明正文真实落库且详情接口返回纯文本。
- **真实 Spring + MySQL + Chromium 整链路**（`GAMEBOI_BROWSER_E2E=1`）：5/5 通过，浏览器读到正文纯文本且 DOM 内无 `<p>` 标记。
- 前端 `npm test` 12 文件 45 项通过；`npm run build` 退出码 0。
- 离线浏览器流程 `scripts/check-web-browser.py` 在 1440px 与 390px 双双通过。

**本轮顺带修复的既有失效**（与上述改动无关，属视觉重构后遗留）：`scripts/check-web-browser.py` 与 `scripts/check-web-live.py` 仍在用 `.sidebar .cat-item`、`.hamburger`、`.menu-cats`、`NO DATA`、`重试游戏列表` 等已不存在的选择器/文案，导致浏览器 E2E 在改动前就已失败；已按其当前标记改为 `.game-tabs button`、`暂无资讯`、`重试`，并补上 `.content` 断言。

**未验证 / 遗留**：`scripts/check.ps1` 因 `$ErrorActionPreference='Stop'` 会把原生命令的任意 stderr 输出升级为终止错误，故 Backend/Frontend build/Frontend tests/Python smoke 四项显示 FAIL；但各底层命令直接执行均为退出码 0（已逐项复核），未修改该检查脚本的行为。E2E 子进程依赖 PATH 上的 `python` 具备 playwright，本机需前置 conda 解释器。

### SOURCE-01：三角洲行动国服来源提升为 SHADOW 并完成首次影子运行（2026-10-16）

产品负责人授权把该来源由 `FETCH_VERIFIED` 提升为 `SHADOW`。按项目惯例用 Flyway 迁移而非手改数据行：

- `V12__promote_delta_force_source_to_shadow.sql`：`UPDATE content_source SET verification_status='SHADOW', auto_publish_enabled=0 WHERE code='delta-force-garena-official' AND verification_status <> 'PAUSED'`。保留 PAUSED 语义、**不打开自动发布**。
- `V13__align_delta_force_source_identity.sql`：把显示名/base_url/allowed_domains/language 对齐国服实际入口（`三角洲行动官方公告（国服官网）`、`df.qq.com`、`df.qq.com`+`comm.ams.game.qq.com`、`zh-CN`）。**sourceCode 保持不变**（V8 种子、来源策略与迁移测试都以它为准），迁移测试显式断言 `V13` 不改 code。
- 同步更新 4 处硬编码迁移数量/期望值的测试：`FlywayMigrationLayoutTest`（names 数组、长度 11→13、新增 V12/V13 内容断言）、`MySqlFlywayIntegrationTest`（全量 11→13、legacy 升级 10→12、delta-force 期望 FETCH_VERIFIED→SHADOW）、`IngestReceiptMySqlTransactionTest`（11→13）。

**首次真实影子运行**（`--source delta-force-garena-official --no-ai`，指向本地 8080）：列表 6 条 → 解析 6 篇 → 5 篇入库（id 14–18），全部 `reading_mode=FULL_TEXT`、正文 1059–7191 字符、`workflow_status=PENDING_REVIEW`（**未自动上线**，符合 SHADOW 语义）；1 条按规则 REJECT（官方公告通篇未出现游戏名）。

**本轮修掉的三个缺陷**：

1. **`DISALLOWED` 关键词误杀**（用户裁决"移除主播"）：`filters/fps_rules.py` 的 `DISALLOWED` 移除 `"主播"`。原来官方活动公告正文提及主播即被 `DISALLOWED_TYPE` 硬拒；修复后真实那条由 `REJECT` 变 `ACCEPT`。新增回归测试。
2. **REVIEW 且类型未定的条目永久投不进去**（阻塞级，用户裁决"只扩充类型关键词表"）：`IngestEvent.contentType` 是 `@NotBlank` 六类枚举，而 `classify()` 在标题/正文都命中不到类型关键词时返回 `REVIEW` + `contentType=None`，载荷违反契约 → 后端 400 → 投递器把 400 归为永久失败 → 事件 **QUARANTINED**。（DB 侧 `article.content_type` 本就 `NULL` 容忍，卡的只是线上契约。）已扩充 `TYPE_KEYWORDS`：esports 增加 联赛/总决赛/半决赛/预选赛/锦标赛/晋级/冠军/夺冠/赛程，event 增加 庆典/周年/嘉年华/联动。复测：原本被 400 隔离的《春夏双冠！…夏季赛总冠军！》现为 `ACCEPT/esports`，重跑 `enqueued 1 / delivered 1`。新增 2 项回归测试。
3. **来源显示名与实际入口不符**：新入库文章此前挂"Delta Force 官方公告（Garena 区域）"（V8 旧名），前台会显示错误的来源；`V13` 已对齐。

**连带修正**：`tests/test_delta_force_garena_source.py` 中断言该夹具为 `patch_notes` 的用例，因标题级 `庆典` 现被正确识别为 `event`（既有规则"标题级类型强于正文偶然词"），已更新为 `event` 并注明原因。

**本轮实际运行的验证**：Python unittest **112 项通过**；后端 `mvnw -o test`（含隔离 MySQL `127.0.0.1:33067` 的真实迁移与事务测试）**123 项通过、0 跳过、0 失败**，其中 `MySqlFlywayIntegrationTest` 在全新库实跑 V1–V13 并断言 delta-force 为 `SHADOW`；`big_event` 实际应用 V12/V13（`flyway_schema_history` success=1）。3306/6379 之外的隔离实例运行后已停止，未接触其他项目环境。


### ING-03：三角洲行动来源改接国服官网 flow JSON（2026-10-16）

用户要求把 `ingest/spiders/delta_force_garena.py` 的爬取路径改为 `https://df.qq.com/cp/a20240906main/newslist.html`。

**入口排查（受控请求，未使用无头浏览器）**：列表页 `<ul id="newslist">` 与详情页 `<p id="sContent">` 原始 HTML 均为空，内容由内联脚本 `Milo.emit(flow_1073310 / flow_1073326)` 异步拉取。本机沙箱不允许 Chrome/Playwright 启动（进程管道被拒），设计基线也明确“首期不安装常驻 headless browser”，因此改为直接还原并调用同一批第一方接口：从 `milo.min.js` 模块 171/9002 得到请求契约 `POST https://comm.ams.game.qq.com/ide/`，表单需 `iChartId/iSubChartId/sIdeToken/sMiloTag` + 业务参数，活动 `actId=670608`、`sServiceType=dfm`（chart id 是 IDE mapid 而非 flow id：列表 329849、详情 329870）。匿名即可用，不需要 Cookie 或登录。

**代码**：`spiders/delta_force_garena.py` 重写为 JSON 流程适配器（列表 → 按 `iDocID` 拉详情 → `RawArticle`，保留官方 URL、原文日期、图片与正文）；`spiders/base.py` 新增 `post_form_json()`（复用 origin/DNS/2MB 上限守卫）与 `API_ORIGINS` 显式额外来源白名单（默认空集、仅 https）。`SOURCE_CODE` 保持 `delta-force-garena-official` 不变，避免破坏 `V8__seed_fetch_verified_delta_force_garena_source.sql`、后端来源策略与 `FlywayMigrationLayoutTest` / `MySqlFlywayIntegrationTest`。

**本轮实际运行的验证**：
- 受控接口调用：列表 `{"ret":0,...,"total":530}`，详情返回 `sTitle/sContent/sIdxTime/sAuthor`，实测时间 2026-10-16 17:0x。
- 离线夹具 + 单测：`ingest/tests/test_delta_force_garena_source.py` 6/6 通过（列表发现、URL/日期/图片归属、规则分类、接口错误 fail-closed、额外来源 origin 白名单）。
- 端到端 dry-run：`python main.py --dry-run --source delta-force-garena-official` 成功；9 条列表全部解析，2 条进入候选（`ACCEPT delta-force/patch_notes`、`REVIEW`），dry-run 未写 Redis/SQLite/outbox/AI/OSS。
- **`python -m unittest discover -s tests` 全量 109 项通过、0 失败**（在文件沙箱策略放宽后复跑；同一条命令在受限沙箱下曾出现 56 项 ERROR，全部是 `tempfile.TemporaryDirectory()` 清理阶段 `os.chmod`/`scandir` 被拒的 `PermissionError: [WinError 5]`，与代码无关，已用无关文件 `tests/test_outbox.py` 单独复现确认）。

**未验证 / 遗留**：国服官网公告大量为整篇长图，正文只有 `<img>`，规则初筛按 `GAME_NOT_ALLOWED` 拒绝（标题常不含“三角洲行动”）。是否为国服官方来源补充“来源级游戏归属”证据，或把图片型公告转为封面摘要，属产品与规则决策，本轮未改动 `fps_rules.py`；`docs/SOURCE_EVALUATION.md` §6 已登记该事实。

**后续调整（同日）：列表栏目收敛为「新闻」**。用户要求“只爬取新闻资讯的内容”。按 `index.js` 的 `NewsArr`（1=最新 6895、2=公告 6896、3=新闻 6898、4=赛事 7037），把 `NEWS_CHANNEL_ID` 由 6895「最新」改为 6898「新闻」（实测 `total=155`，分页正常，每条 `sChannelInfo` 含 `6898|新闻`），避免违规处罚公告等纯公告混入资讯流。列表夹具重建为新闻栏目 6 条（含 77 字段完整记录），适配器测试改为 6/6，其中新增断言：请求 `chanid` 必须等于 6898、返回条目必须都在新闻栏目、正文图片全部提升为绝对 https 地址。

**本轮实际运行的验证（新闻栏目）**：
- dry-run `MAX_ARTICLES_PER_RUN=6`：6 条全部解析，**4 条进入候选**（`ACCEPT patch_notes` ×1、`ACCEPT esports` ×2、`REVIEW` ×1），2 条被规则过滤。
- 27 条抽样（`start=0/9/18`，逐条取详情后过规则）：**ACCEPT 13 / REVIEW 5 / REJECT 9**；9 条拒绝中 4 条是其它游戏内容被 `GAME_NOT_ALLOWED` 正确拦下，5 条是官方活动/社团招募标题不含游戏名。
- 全量 `python -m unittest discover -s tests`：109 项通过、0 失败。

**遗留（内容质量，非解析缺陷）**：新闻栏目 `esports` 占比偏高，且有多条重复模板（`恭喜XXX成功晋级总决赛！` 连续 6 条）。**产品已确认本轮不处理，归属去重策略**，来源适配器与 `fps_rules.py` 均不再调整。

**栏目策略已定稿**：锁定「新闻」6898 单一栏目，不采集「最新」/「公告」/「赛事」；已知取舍是**不收录补丁说明与版本更新公告**（该类内容主要挂在「公告」6896）。如后续需要补丁说明，应新增独立来源或在去重策略一并评估，而不是把 6895/6896 混回本来源。

### DOC：README 项目入口与系统架构图更新（2026-10-27）

**目标**：按当前设计、迁移和实际代码重整 README，使新读者能快速理解项目定位、两条主链路、本地启动方式、来源状态和未闭环边界。

**参考**：读取 Spring Boot、RSSHub、NewsNow 官方仓库 README，采用「简短定位 → 核心能力 → 架构 → 快速开始 → 专项文档」的组织方式；不复制其业务内容或添加无实证徽章。

**变更文件**：
- `README.md`：重写项目入口，补充可靠交付与审核语义、六个注册来源、PowerShell 启动步骤、接口/配置/测试/目录导航；Node 要求对齐 Vite 8 engines，真实采集补充容量快照前置条件。
- `docs/assets/gameboi-architecture.svg`：新增本地 SVG 架构图，覆盖 Vue、Spring Boot、MySQL、Redis、官方来源、Python 适配与清洗、SQLite outbox、内部 API、receipt 和人工审核状态机。

**实际验证**：
- 自定义 Python 文档检查通过：23/23 个本地 Markdown 链接存在，代码围栏成对，关键架构与运行说明存在。
- SVG XML 解析通过：6 个 ID 无重复，18 个 `url(#...)` 引用均可解析。
- `git diff --check` 通过；保留用户已有的 `PublicArticleReadServiceImpl.java` 修改，本轮未触碰业务代码。

**未验证 / 范围**：本轮只修改文档与 SVG，未运行后端、前端、Python 业务测试，未启动服务、连接数据库、部署、提交或推送。README 来源注册状态以 V1–V13 迁移为准；历史叙述文档不作为当前状态依据。
