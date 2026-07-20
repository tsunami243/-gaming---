# Failures.md — GameBoi 故障记录

> **面向对象**：开发工作者（非 AI Agent）
> **目标**：快速定位问题本质原因，获取可复现的解决方法论

---

## 目录

| # | 来源 | 问题 | 状态 |
|---|------|------|------|
| BUG-001 | S001 | Redis 未运行导致后端每 10s 报错 + 爬虫不可用 | ✅ 已修复 |
| BUG-002 | S001 | Python 路径损坏导致 init.sh 崩溃且爬虫不可用 | 🔧 待修复 |
| BUG-003 | S002 | @Scheduled 消费者未自动消费 Redis 队列 | ✅ 已修复 (2026-07-05) |
| BUG-004 | S002 | AI 分类 category_id 始终为 0 | 🔧 待修复 |
| BUG-005 | S002 | 非游戏内容（体育新闻）通过 is_gaming 过滤 | 🔧 待修复 |
| BUG-006 | S002 | 空 title/空 content 文章入库 | ✅ 已修复 (2026-07-05) |
| BUG-007 | S001 | 无单元测试，src/test 目录不存在 | 🔧 待修复 |
| BUG-008 | S002 | 旧进程残留导致端口冲突 | 🔧 待修复 |

---

## BUG-001 · Redis 未运行

| 字段 | 内容 |
|------|------|
| **发现时间** | 2026-07-04 (Session 001) |
| **状态** | ✅ 已修复 |

### 表现

- 后端启动日志每 10 秒输出：
  ```
  RedisConnectionFailureException: Unable to connect to Redis
  Connection refused: localhost/127.0.0.1:6379
  ```
- `redis-cli ping` → `command not found` 或 `Connection refused`
- 爬虫 `python main.py` → 启动即退出：`ERROR: Redis not reachable — aborting`
- `/admin/ingest/consume` 端点不可用

### 根本原因

Windows 环境下 Redis 未安装。项目依赖 Redis 承担两项核心职责：
1. **JWT Token 双验证**（Redis 存储 token 副本，与 JWT 一起校验）
2. **爬虫消息队列**（Python 侧 RPUSH → `ingest:article_queue`，Java 侧 LPOP 消费）

Redis 不可用时，这两种能力完全失效。后端可以启动（因为 Spring Data Redis 只在实际调用时创建连接），但定时任务 `ArticleIngestConsumer` 每 10s 尝试 LPOP 必抛异常。

### 解决方案

```bash
# Windows — 通过 winget 安装（推荐）
winget install --id Redis.Redis --accept-source-agreements

# 安装后需重启终端，或手动添加到 PATH
# 默认安装路径：C:\Program Files\Redis
# 启动 Redis：
redis-server

# 验证
redis-cli ping  # 期望 PONG
```

**方法论**：任何依赖外部服务（Redis、MySQL、OSS）的中间件项目，应在开发者 onboarding 文档中明确列出服务清单和安装步骤。`init.sh` 应包含 Redis 状态检测。

---

## BUG-002 · Python 路径损坏

| 字段 | 内容 |
|------|------|
| **发现时间** | 2026-07-04 (Session 001) |
| **状态** | 🔧 待修复 |

### 表现

```bash
$ python3 --version
[ERROR] Failed to launch 'D:\python.exe' (0x80070002)

$ ./init.sh
# 脚本在 Python 检查阶段直接退出，set -e 导致后续全不执行
```

### 根本原因

用户的 Windows PATH 环境变量中存在一条历史残留：
```
D:\python.exe
```
该路径指向一个已被卸载的 Python 可执行文件。Windows 在搜索 `python3` 时找到此路径，但文件不存在，导致启动失败。

**为什么 `python` 可以用？** 因为 conda（`D:\conda\python`）提供了 `python` 命令，且其优先级高于损坏路径。

**为什么 init.sh 崩溃？** `init.sh` 第 54 行：
```bash
PY_VER=$(python3 --version)  # 触发 set -e，直接退出
```
`set -euo pipefail` 使任何非零退出码立即终止脚本。

### 解决方案

**短期（立即生效）**：
```bash
# 从 PATH 中移除损坏条目
# Windows: 系统属性 → 环境变量 → Path → 删除 D:\python.exe 条目
```

**工程化修复（init.sh）**：
```bash
# 改为回退机制
if command -v python3 &>/dev/null; then
    PY_VER=$(python3 --version 2>/dev/null) || true
fi
# 如果 python3 失败，回退到 python
if [ -z "$PY_VER" ] && command -v python &>/dev/null; then
    PY_VER=$(python --version 2>/dev/null) || true
fi
```

**方法论**：环境检测脚本不应因非关键检查而终止。对可选依赖（爬虫模块）的检查应降级为 warning，而非 fatal error。使用 `|| true` 保护可能失败的命令。

---

## BUG-003 · @Scheduled 消费者未自动消费

| 字段 | 内容 |
|------|------|
| **发现时间** | 2026-07-04 (Session 002) |
| **状态** | ✅ 已修复 (2026-07-05) |

### 表现

- 爬虫 `python main.py` 成功运行，6 篇文章 RPUSH 到 `ingest:article_queue`
- 等待 10+ 分钟后，`redis-cli LLEN ingest:article_queue` 仍返回 6
- 手动调用 `POST /admin/ingest/consume` 可正常 LPOP 并入库
- 之后手动 enqueue 测试文章，消费者又能正常消费

### 根本原因

**时序竞态 + Lettuce 连接池静默失败**。

时间线还原：
```
T0     Redis 启动（刚安装完，服务不稳定）
T+120  后端启动，ArticleIngestConsumer @PostConstruct 执行
       → Spring Data Redis 此时创建 LettuceConnectionFactory
       → 第一次连接成功（因为 Redis 此时正常）
T+125  消费者首次 @Scheduled 触发（initialDelay=5s）
       → LPOP 执行，Redis 正常响应，返回 null（队列为空）
T+180  爬虫运行，6 篇文章 RPUSH 到队列
T+190  消费者再次 @Scheduled 触发
       → 此时 LPOP 可能因 Lettuce 连接池的某种状态异常返回 null
       → 或连接在 Redis 服务短暂波动后进入静默死状态
```

Spring `@EnableScheduling` 默认创建单线程 `ScheduledThreadPoolExecutor`（coreSize=1），如果某次 `leftPop` 调用阻塞（网络超时），后续所有定时任务全部延迟。Lettuce 默认没有连接空闲检测，TCP 连接可能在 Redis 服务波动后表面建立但实际断开。

### 解决方案

**两处修改**：

1. **配置 Lettuce 连接池 + 重连策略**（`application.yml`）：
```yaml
spring:
  data:
    redis:
      port: 6379
      host: localhost
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
        cluster.refresh.adaptive: true
      connect-timeout: 3000ms
```

2. **显式配置 TaskScheduler**（新增 config bean）：
```java
@Configuration
@EnableScheduling
public class SchedulingConfig {
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("scheduled-");
        scheduler.setErrorHandler(t -> log.error("Scheduled task error", t));
        return scheduler;
    }
}
```

**方法论**：生产环境的 @Scheduled 定时任务不应依赖默认的单线程池。所有需要可靠投递的消费型定时任务，应配置独立的线程池 + 错误处理器 + 连接超时 + 重连策略。消息队列消费应考虑使用 Redis BRPOP（阻塞式）代替轮询式 LPOP。

---

## BUG-004 · DeepSeek AI category_id 始终为 0

| 字段 | 内容 |
|------|------|
| **发现时间** | 2026-07-04 (Session 002) |
| **状态** | 🔧 待修复 |

### 表现

爬虫采集的 10 篇文章，DeepSeek V4 Flash 返回的 `category_id` 全部为 0：
```
[1] GTA6付费广告        → cat=0
[2] 空之轨迹限时免费    → cat=0
[3] 梅西笑谈            → cat=0
...全部 cat=0
```
Java 侧 `IngestController` 中 `catId > 0 ? catId : null` 将所有 0 转为 NULL，导致 34 篇文章中 32 篇 `category_id IS NULL`。

### 根本原因

**AI prompt 中 CATEGORY_MAP 描述不够强制**。当前 prompt 将分类信息作为参考文本传递给 AI，但未在 prompt 中明确要求 AI 必须从 CATEGORY_MAP 中选择 category_id。DeepSeek V4 Flash 是轻量模型，在开放式指令下倾向返回默认值（0）而非理解分类体系。

查看 `agent/prompts.py` 中的 system prompt 可以确认：CATEGORY_MAP 被嵌入为"参考信息"而非"强制要求"。

### 解决方案

**修改 `agent/prompts.py`**：
- 将 category_id 从"参考"提升为"必须从 CATEGORY_MAP 中选择"
- 在 prompt 中添加硬性约束：`category_id 必须是 CATEGORY_MAP 中存在的整数，如果无法确定则选择最接近的，不要返回 0`
- 可选：在 `deepseek_client.py` 添加后处理逻辑，如果 category_id 不在 CATEGORY_MAP 中则降级为默认分类

**方法论**：与 LLM 交互时，约束性字段（如枚举值）必须在 prompt 中明确标记为必填和严格校验，而不是以参考信息形式呈现。轻量模型对"建议"和"要求"的区分能力弱于大模型。

---

## BUG-005 · 非游戏内容通过 is_gaming 过滤

| 字段 | 内容 |
|------|------|
| **发现时间** | 2026-07-04 (Session 002) |
| **状态** | 🔧 待修复 |

### 表现

一篇体育新闻"姆巴佩能否超梅西成世界杯历射手王"被：
1. AI 判定 `is_gaming=True`（confidence=10%）
2. 正常入队 → 入库
3. title 和 content 为空（AI 处理失败）
4. 手动删除该文章（id=881）

### 根本原因

**双重失效**：

1. **AI 判定过于宽松**：10% confidence 的情况下仍然返回 `is_gaming=True`。这说明 prompt 中 `is_gaming` 的判断标准和 confidence 之间没有联动关系。

2. **管线缺乏阈值过滤**：`main.py` 第 89-91 行直接使用 AI 返回的 `is_gaming` 布尔值，未检查 `confidence`：
```python
if not processed.is_gaming:
    print("✗ 已过滤 (非游戏内容)")
    redis.incr_stat("total_filtered")
    continue
```

### 解决方案

**双层修复**：

1. **Python 侧增加硬性阈值**（`deepseek_client.py` 或 `main.py`）：
```python
# confidence < 阈值 → 强制 is_gaming=False
if processed.confidence < 0.3:
    processed.is_gaming = False
```

2. **优化 prompt**（`agent/prompts.py`）：
   - 要求 AI 对 `is_gaming` 的判断与 `confidence` 联动
   - 明确"不确定时偏向保守（False）"

**方法论**：LLM 输出具有随机性，不应将单一 AI 判定作为管道 gate。关键过滤节点应增加确定性规则（如 confidence 阈值、关键词匹配）作为第二道防线。

---

## BUG-006 · 空 title/空 content 文章入库

| 字段 | 内容 |
|------|------|
| **发现时间** | 2026-07-04 (Session 002) |
| **状态** | ✅ 已修复 (2026-07-05) |

### 表现

```sql
SELECT id, title, content FROM article WHERE id=881;
-- id=881, title='', content=''
```

- 文章 881 的 title 和 content 均为空字符串
- 前端展示为空白卡片

### 根本原因

**整条管线缺少数据校验**：

| 环节 | 校验 | 结果 |
|------|------|------|
| DeepSeek AI 返回 | 无 | title/content 可能为空 |
| `main.py` enqueue | 无 | 空数据直接 RPUSH 到 Redis |
| `ArticleIngestConsumer.toArticle()` | 无 | 空数据直接 setTitle("") setContent("") |
| `InspectController` manual consume | 无 | 同上 |
| MySQL | 无 NOT NULL + 无 CHECK 约束 | 空字符串合法 |

**缺失校验的位置**（按修复优先级）：

1. **Python `main.py`（最佳修复点）**：enqueue 前校验，空数据不入队
2. **Java `toArticle()`（兜底）**：校验失败抛异常，触发 push-back
3. **MySQL（最后防线）**：添加 CHECK 约束（可选，需要 DDL 变更）

### 解决方案

**Python 侧（首选）**：
```python
# main.py — 在 producer.enqueue(processed) 之前
if not processed.title or not processed.title.strip():
    print("  ✗ 跳过 (空标题)")
    redis.incr_stat("total_skipped")
    continue
if not processed.content or len(processed.content.strip()) < 50:
    print("  ✗ 跳过 (内容过短或无实质内容)")
    redis.incr_stat("total_skipped")
    continue
```

**Java 侧（兜底）**：
```java
// ArticleIngestConsumer.toArticle()
private Article toArticle(Map<String, Object> map) {
    String title = str(map, "title");
    if (title.isBlank()) {
        throw new IllegalArgumentException("Article title must not be blank");
    }
    // ...
}
```

**方法论**：数据管线的每个环节都应对下游有"不信任假设"。上游产出的数据在被消费前必须经过校验。校验应在协议边界处实施（入库前、入队前、展示前），而非依赖最终存储层的约束。

---

## BUG-007 · 无单元测试

| 字段 | 内容 |
|------|------|
| **发现时间** | 2026-07-04 (Session 001) |
| **状态** | 🔧 待修复 |

### 表现

```bash
$ ./mvnw test
# Tests run: 0, Failures: 0, Errors: 0, Skipped: 0
# No tests to run.
$ ls src/test/
# ls: cannot access 'src/test/': No such file or directory
```

### 根本原因

项目从零搭建时未创建测试目录。`pom.xml` 已引入 `spring-boot-starter-test`（含 JUnit 5 + Mockito），但 `src/test/java` 目录不存在。

### 解决方案

1. 创建标准测试目录结构
2. 优先覆盖 **ArticleIngestConsumer**（当前最脆弱的组件）
3. 覆盖 **ArticleService.addArticle()**（数据入库入口）

**方法论**：测试应在功能代码编写时同步创建，而非事后补充。项目的 `feature_list.json` 应将"编写测试"作为每个功能的硬性完成条件。

---

## BUG-008 · 旧进程残留导致端口冲突

| 字段 | 内容 |
|------|------|
| **发现时间** | 2026-07-04 (Session 002) |
| **状态** | 🔧 待修复 |

### 表现

```
Port 8080 was already in use.
Port 5173 is in use, trying another one...
```
- 后端启动失败（8080 被上次会话的 Java 进程占用）
- 前端被迫使用 5174（5173 被上次会话的 Vite 进程占用）

### 根本原因

项目没有进程生命周期管理。Spring Boot `spring-boot:run` 和 Vite `--host` 以普通进程启动，关闭终端不会自动终止它们。没有 `stop.sh` 脚本。

### 解决方案

**`init.sh` 启动前增加端口清理**：
```bash
# 检查并清理 8080
PID_8080=$(netstat -ano | grep ":8080.*LISTENING" | awk '{print $5}' | head -1)
if [ -n "$PID_8080" ]; then
    taskkill //PID "$PID_8080" //F 2>/dev/null
fi
```

**方法论**：开发环境的服务管理应明确生命周期（启动 → 运行 → 停止）。每个项目的启动脚本应包含前置的端口冲突检测与清理逻辑。
