# CLAUDE.md

## 项目概述

GameBoi — 游戏资讯聚合与社区平台。详情见 [README.md](README.md)。

- **后端端口**: 8080
- **前端端口**: 5173（Vite 代理 `/api` → `localhost:8080`）

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 4.0.5 · Java 17 · MyBatis · Spring Security 7.0 |
| 认证 | JWT (jjwt 0.11.5) + Redis Token 双验证 |
| 数据库 | MySQL 8.0（库名 `big_event`） |
| 缓存/队列 | Redis 7 |
| 存储 | 阿里云 OSS |
| 前端 | Vue 3.5 · Vite 8.0 · Pinia 3.0 · Vue Router 4.6 · Naive UI 2.44 |
| 爬虫 | Python 3.11+ · httpx · BeautifulSoup · DeepSeek V4 API |

## 模块结构

| 目录 | 说明 |
|---|---|
| `src/main/java/org/example/bigevent/` | Spring Boot 后端主模块 |
| `gaming-news/` | Vue 3 前端 |
| `ingest/` | Python 爬虫采集管线 |
| `sql/` | 数据库 DDL 脚本 |

## 环境准备

- JDK 17+ · Node.js 18+ · Python 3.11+ · MySQL 8.0 · Redis 7

### 首次初始化

1. 创建数据库: `CREATE DATABASE IF NOT EXISTS big_event DEFAULT CHARSET utf8mb4;`
2. 导入表结构: `mysql -u root -p big_event < sql/schema.sql`
3. 后端配置: 检查 `src/main/resources/application.yml`（数据库密码、Redis 地址）
4. 爬虫配置（可选）: `cd ingest && cp .env.example .env`，填入 DeepSeek API Key 和 OSS 凭证

## 常用命令

### 构建与启动

```bash
# 后端（编译 + 启动）
./mvnw clean package -DskipTests
./mvnw spring-boot:run

# 前端
cd gaming-news && npm install && npm run dev

# 爬虫（可选）
cd ingest && pip install -r requirements.txt && python main.py --dry-run
```

### 验证

```bash
# 后端冒烟测试
curl http://localhost:8080/article?pageNum=1&pageSize=5

# 运行后端单元测试
./mvnw test
```

## 开工流程

写代码前先做这些事：

1. 用 `pwd` 确认当前目录。
2. 读取 `claude-progress.md`，了解最新已验证状态和下一步。
3. 读取 `feature_list.json`，选择优先级最高的未完成功能。
4. 用 `git log --oneline -5` 看最近提交。
5. 运行 `./init.sh`。
6. 在开始新功能前，先跑必需的 smoke test 或端到端验证。
7. 如果基础验证一开始就失败，先修基础状态，不要在坏的起点上继续叠新功能。

## 工作规则

- 一次只做一个功能。
- 不要因为"代码已经写了"就把功能标记为完成。
- 除非为了消除当前 blocker 的窄范围修复，否则不要扩大到其他功能。
- 实现过程中不要悄悄改弱验证规则。
- 优先依赖仓库里的持久化文件，而不是聊天记录。

## 必需文件

- **feature_list.json**：功能状态的唯一事实来源
- **claude-progress.md**：会话进度和当前已验证状态
- **init.sh**：统一的启动与验证入口
- **session-handoff.md**：较长会话可选的交接摘要
- **DEVELOPMENT.md**：工具层操作手册 — shell 命令、文件地图、API 参考、测试指南、故障排查。执行 shell 命令、查 API、写测试、排查环境问题时必须先读此文件。

## 完成定义

一个功能只有在以下条件都满足时才算完成：

- 目标行为已经实现
- 要求的验证真的跑过
- 证据记录在 feature_list.json 或 claude-progress.md
- 仓库仍然能按标准启动路径重新开始工作

## 收尾

结束会话前：

1. 更新 claude-progress.md
2. 更新 feature_list.json
3. 记录仍未解决的风险或 blocker
4. 在工作处于安全状态后，用清晰的提交信息提交
5. 保证下一轮会话可以直接运行 ./init.sh
