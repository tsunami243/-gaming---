# GameBoi — 游戏资讯平台

基于 **Spring Boot + Vue 3 + Python 采集管线** 的四游戏精准资讯平台。

## 开发入口（先读这些）

| 文档 | 说明 |
|---|---|
| [docs/PROJECT_DESIGN.md](docs/PROJECT_DESIGN.md) | 产品边界、架构、资源上限与发布门禁的**权威基线** |
| [docs/DEVELOPMENT_PLAN.md](docs/DEVELOPMENT_PLAN.md) | 任务顺序、范围、依赖与验收定义 |
| [feature_list.json](feature_list.json) | 任务状态唯一记录 |
| [claude-progress.md](claude-progress.md) | 按时间追加的执行证据 |
| [docs/SOURCE_EVALUATION.md](docs/SOURCE_EVALUATION.md) | 来源验证登记 |
| [docs/DATABASE_MIGRATION.md](docs/DATABASE_MIGRATION.md) | 数据库迁移与旧库预演 |
| [AGENTS.md](AGENTS.md) | 协作约定（权限边界、资源预算、收尾要求） |

> 历史章节（`CLAUDE.md`、`DEVELOPMENT.md`、`Failures.md`）仅作背景，与上述基线冲突时以基线为准。

---

## 架构概览

```
Vue 3 (gaming-news/)  ──/api/v1──▶  Spring Boot (src/main/java)  ──▶  MySQL 8
                                          │                              ▲
                                          ├── Redis（会话校验 / 热度 ZSet / 推送演示）
                                          │
                                          ▼
                              /internal/v1/ingest  ◀── Python 采集管线 (ingest/)
                                                          │
                                          四个游戏官网 + ALGS 赛事
```

两条独立的内容链路：

1. **人工链路**：管理员/作者通过 legacy 接口或 `/api/v1/admin/*` 创建、审核、发布文章。
2. **采集链路**：Python 管线抓取 → 规则分类 → 本地清洗 → 幂等投递到内部入库接口 → 落库为**待审核草稿**。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 4.0.5 · Spring Security · MyBatis 4.0.1（Java 17 编译） |
| 数据库 | MySQL 8.0（Flyway 迁移，当前 V1–V13） |
| 缓存 | Redis（JWT 会话校验、热度 ZSet；推送演示模块需 Redis ≥ 5.0） |
| 认证 | JWT（HS256）+ Redis 双验证；口令 BCrypt |
| 前端 | Vue 3.5 · Vite · Pinia · Vue Router · Naive UI |
| 采集 | Python 3.11+ · httpx · BeautifulSoup4 · lxml · Pydantic · Click |
| 可选 AI | DeepSeek（`--no-ai` 时完全不调用） |

## 目录结构

```
gaming/
├── src/main/java/org/example/bigevent/
│   ├── config/          # Security / 口令 / 调度配置
│   ├── security/        # JWT 认证过滤器
│   ├── controller/      # REST 接口（v1 与 legacy 两组）
│   ├── service/         # 接口层；实现统一放 service/impl/
│   ├── mapper/          # MyBatis 数据访问（部分 SQL 在同名 XML）
│   ├── pojo/            # 实体与 DTO
│   ├── filter/          # 追踪 ID、内部采集请求边界
│   └── task/            # 轮播、热度回写定时任务
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/    # Flyway 迁移（V1–V13）
├── gaming-news/         # Vue 3 前端
├── ingest/              # Python 采集管线
│   ├── main.py          # CLI 入口
│   ├── spiders/         # 各来源适配器
│   ├── filters/         # 确定性游戏/类型判定规则
│   ├── agent/           # DeepSeek 处理器与本地确定性清洗器
│   ├── runtime/         # outbox / 状态锁 / 容量闸门 / 投递
│   └── quality/         # 影子运行与质量评估夹具
└── deploy/              # Dockerfile、nginx、.env.example
```

## 采集管线

### 已登记来源

| 来源编码 | 游戏 | 状态 |
|---|---|---|
| `apex-official` | Apex Legends | SHADOW（影子运行） |
| `valorant-official` | VALORANT | SHADOW |
| `delta-force-garena-official` | 三角洲行动（国服官网） | SHADOW |
| `cs2-official` | CS2 | FETCH_VERIFIED（仅可抓取） |
| `algs-official` | Apex Legends 赛事 | FETCH_VERIFIED |

### 两道发布门禁

- **来源级**：`content_source.verification_status` 决定该来源能否真实运行（仅 `SHADOW` / `AUTO_ENABLED` 放行，`FETCH_VERIFIED` 只能 dry-run）。
- **内容级**：入库文章**无条件**进入 `PENDING_REVIEW`，必须经管理员 approve 才发布。

> `auto_publish_enabled` 是预留字段，当前采集链路**不读取**它——即"自动发布"尚未实现。

### 分类与清洗

- 分类走 `ingest/filters/fps_rules.py` 的确定性规则（游戏别名 + 类型关键词 + 禁用词），输出 `ACCEPT / REVIEW / REJECT`。
- 正文清洗有两种后端：
  - `LocalArticleProcessor`（`--no-ai`）：离线确定性清洗，无任何外部调用。
  - `DeepSeekAgent`：调用 DeepSeek 生成摘要与分类。

### 运行采集

```bash
cd ingest
pip install -r requirements.txt
cp .env.example .env          # 填写下方环境变量

# 只抓取不入库
python main.py --source apex-official --dry-run

# 抓取 → 清洗 → 投递入库（不调用 AI）
python main.py --source apex-official --no-ai
```

## 快速开始（本地）

### 环境要求

JDK 17+ · Node.js 18+ · Python 3.11+ · MySQL 8.0 · Redis

### 1. 数据库

创建库并让 Flyway 自动迁移（V1–V13）：

```bash
mysql -u root -p -e "CREATE DATABASE big_event CHARACTER SET utf8mb4;"
```

### 2. 后端

`application.yml` 中的**一切凭据都是必填占位符，不注入直接启动失败**（仓库里不留任何真实口令）：

```powershell
$env:JWT_SECRET_KEY       = "<至少 32 字符的随机串>"
$env:INGEST_SERVICE_TOKEN = "<内部采集接口令牌>"
$env:DB_USERNAME          = "<数据库用户>"      # 本地通常是 root
$env:DB_PASSWORD          = "<数据库口令>"

.\mvnw.cmd spring-boot:run
```

变量名与 `deploy/compose.yaml`、`init.sh` 保持一致（`DB_USERNAME` / `DB_PASSWORD`），因此同一套值可以同时喂给本地运行、容器部署与初始化检查。换 host/库名时再覆盖连接串与 Redis：

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://<host>:3306/big_event?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:SPRING_DATA_REDIS_HOST = "<redis host>"
```

> Windows 下若宿主注入了 `SERVER__PORT`，需先 `Remove-Item Env:SERVER__PORT` 再启动，否则端口会被改写。
>
> 需要看 SQL 明细日志时加 `--spring.profiles.active=dev`；默认 `logging.level` 是 `info`，
> 不会把 SQL 与参数值（含邮箱、口令哈希）写进日志。

### 3. 前端

```bash
cd gaming-news && npm install && npm run dev   # http://127.0.0.1:5173
```

### 4. 采集（可选）

见上方「运行采集」。

## 配置项

### 后端环境变量

| 变量 | 说明 |
|---|---|
| `JWT_SECRET_KEY` | JWT 签名密钥（HS256，长度不足直接启动失败） |
| `INGEST_SERVICE_TOKEN` | 内部采集接口令牌；未配置时一律拒绝请求（fail-closed） |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号口令（**必填**，无内置默认值） |
| `SPRING_DATASOURCE_URL` | 连接串覆盖；默认指向 `localhost:3306/big_event` |
| `SPRING_DATA_REDIS_HOST` / `_PORT` | Redis 连接 |
| `GAMEBOI_FLYWAY_TEST_URL` | 仅测试用：指向隔离 MySQL 以启用门禁测试 |

### 采集管线环境变量

| 变量 | 说明 |
|---|---|
| `INGEST_INTERNAL_BASE_URL` | 后端地址，如 `http://127.0.0.1:8080` |
| `INGEST_SERVICE_TOKEN` | 与后端一致 |
| `REDIS_HOST` / `REDIS_PORT` | URL 去重与统计 |
| `MAX_ARTICLES_PER_RUN` | 单次运行抓取上限 |
| `CRAWL_DELAY_SECONDS` | 逐篇请求间隔 |
| `DEEPSEEK_API_KEY` | 仅在使用 AI 处理器时必需 |
| `OSS_ENDPOINT` / `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` / `OSS_BUCKET_NAME` | 对象存储（当前采集链路不使用） |

## 主要接口

### 公开读取（`/api/v1`）

| 路径 | 方法 | 说明 |
|---|---|---|
| `/api/v1/games` | GET | 游戏（即前端「分类」）列表 |
| `/api/v1/articles` | GET | 已发布文章分页，支持 `gameCode`、`contentType` 筛选 |
| `/api/v1/articles/{id}` | GET | 文章详情；`readingMode=FULL_TEXT` 时返回纯文本正文 |
| `/api/v1/articles/{id}/comments` | GET | 公开评论（需开启评论开关） |

### 内部采集（`/internal/v1/ingest`，令牌保护）

| 路径 | 方法 | 说明 |
|---|---|---|
| `/sources/{code}/policy` | GET | 查询来源是否允许运行（发布门禁） |
| `/events` | POST | 幂等接收采集事件并落库 |

### 管理端（需 `ROLE_CONTENT_ADMIN` / `ROLE_SUPER_ADMIN`）

`GET /api/v1/admin/articles`、`POST /api/v1/admin/articles/{id}/approve|reject|unpublish`、`GET /api/v1/admin/sources` 等。

> 完整契约见 [docs/api/openapi.yaml](docs/api/openapi.yaml)。

## 测试与检查

```bash
# 后端（MySQL 门禁测试需先设置 GAMEBOI_FLYWAY_TEST_URL）
./mvnw.cmd -o test

# 前端
cd gaming-news && npm test && npm run build

# 采集管线
cd ingest && python -m unittest discover -s tests -t .
```

`scripts/check.ps1` 汇总以上检查。注意：该脚本设置了 `$ErrorActionPreference='Stop'`，会把原生命令的**任意 stderr 输出**（Mockito 自附加警告、Vite chunk 体积警告、bs4 DeprecationWarning 等）升级为失败，排查 FAIL 时请先单独运行底层命令核对退出码。

## 安全约定

**以下内容不得提交到仓库**（已在 `.gitignore` 中排除）：

- `ingest/.env`、`deploy/.env`、`.env.local` —— 包含 DeepSeek Key、OSS AccessKey 与数据库口令
- `.workbuddy/` —— 本地开发记录，含排查过程中的账号与令牌
- `ingest/runtime/*.sqlite3*` —— 采集中间状态与 outbox 载荷
- `target/`、`gaming-news/node_modules/`、`__pycache__/`

其他约定：

- **凭据只走环境变量**，仓库内（含 `application.yml`、`compose.yaml`）不出现任何真实口令；`.env.example` 一律用 `replace-before-use` 占位值。
- **一旦凭据曾进入 Git、截图或外发文件，即视为已泄露并立即轮换**——数据库改密、DeepSeek Key 重置、OSS AccessKey 在 RAM 控制台禁用旧 Key 后新建。
- 令牌比较使用常量时间实现；内部采集入口 fail-closed（未配令牌即拒绝）。
- **账号安全**：登录/注册均过 Redis 令牌桶限流；登录失败统一返回“用户名或密码错误”，且对不存在的账号同样跑一次 BCrypt 比对，消除响应内容与耗时上的账号枚举差异；注册强制 10 字符以上、含字母与数字、UTF-8 下不超过 72 字节（BCrypt 上限）。
- **上传**：仅 `CONTENT_ADMIN` / `SUPER_ADMIN` 可用，且校验文件魔数（后缀可伪造，不作为依据）。
- **内部路径**：安全链只放行 `/internal/v1/ingest/**`（由过滤器验令牌），其余 `/internal/**` 一律 403。
- **日志**：`logging.level` 默认 `info`，避免 SQL 与参数值（邮箱、口令哈希）落日志；DEBUG 仅 dev profile 开启。
- **公开正文**为服务端剥离标签后的纯文本，前端一律用插值渲染，禁止 `v-html`。

## 已知限制

- **本机 Redis 需 ≥ 5.0**：推送演示模块（`PushConsumer` / `DeadLetterReprocessor`）依赖 Redis Streams；Redis 3.x 会持续报 `ERR unknown command 'XREADGROUP'`。不影响公开读取、登录与热度榜。
- **REVIEW 类型未定的稿件会投递失败**：`IngestEvent.contentType` 为 `@NotBlank`，若标题与正文都命中不到类型关键词，投递会返回 400 并被隔离到 outbox 待人工重投。数据库侧 `article.content_type` 本身允许 NULL。
- **`article.content` 与 `article_content.sanitized_html` 并存**：公开详情读 `article.content`（入库时写入的爬取正文），`article_content` 保留清洗副本用于留痕比对。
