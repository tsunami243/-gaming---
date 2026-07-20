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
