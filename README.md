# GameBoi — 游戏资讯平台

基于 Spring Boot + Vue 3 + Python 爬虫构建的游戏资讯聚合与社区平台。

## 架构概览

```
Vue 3 前端 ←→ Spring Boot API ←→ MySQL
                    ↕
                 Redis (Token / 消息队列)
                    ↕
           Python 爬虫 → DeepSeek AI → OSS
```

## 技术栈

| 层 | 技术 |
|---|------|
| 前端 | Vue 3 + Vite + Pinia + Vue Router |
| 后端 | Spring Boot 4.0 + MyBatis + Spring Security 7.0 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7（Token / 消息队列） |
| 认证 | JWT + Redis 双验证 |
| 存储 | 阿里云 OSS |
| 爬虫 | Python 3 + readability-lxml + DeepSeek V4 API |

## 功能模块

- **文章系统**：发布/编辑/删除，分类筛选，热度排行榜
- **用户系统**：注册/登录（JWT）、角色权限（USER / CONTENT_ADMIN / SUPER_ADMIN）
- **互动系统**：点赞/取消、收藏/取消、评论/回复
- **轮播图**：首页轮播展示
- **浏览统计**：独立浏览记录、热度算法
- **AI 采集管线**：Python 爬虫 → DeepSeek AI 清洗分类 → Redis 队列 → 后端入库

## 项目结构

```
gaming/
├── src/main/java/.../          # Java 后端
│   ├── config/SecurityConfig   # Spring Security 权限配置
│   ├── security/               # JWT 认证过滤器
│   ├── controller/             # REST 接口
│   ├── service/                # 业务逻辑
│   ├── mapper/                 # MyBatis 数据访问
│   └── pojo/                   # 数据模型
├── gaming-news/                # Vue 3 前端
│   └── src/
│       ├── components/         # 可复用组件
│       ├── views/              # 页面
│       ├── stores/             # Pinia 状态管理
│       └── api/                # 接口封装
├── ingest/                     # Python 爬虫
│   ├── main.py                 # 管线入口
│   ├── spiders/                # 网站适配器
│   ├── agent/                  # DeepSeek AI 处理
│   ├── storage/                # Redis + OSS 客户端
│   └── mq/                     # 消息队列生产者
└── sql/                        # 数据库脚本
```

## 本地运行

### 环境要求
- JDK 17+ · Node.js 18+ · Python 3.11+ · MySQL 8.0 · Redis 7

### 1. 启动 MySQL 和 Redis

### 2. 启动后端

```bash
cd gaming
./mvnw spring-boot:run
```

### 3. 启动前端

```bash
cd gaming/gaming-news
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`

### 4. 启动爬虫（可选）

```bash
cd gaming/ingest
pip install -r requirements.txt

# 配置 .env（参考下方说明）
cp .env.example .env

# 干跑测试
python main.py --dry-run

# 正式运行
python main.py
```

### 5. 消费爬虫队列

```bash
curl -X POST http://localhost:8080/admin/ingest/consume \
  -H "Authorization: Bearer <登录获取的token>"
```

## 配置说明

### 后端 `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/big_event
    username: root
    password: 你的密码
  data:
    redis:
      host: localhost
      port: 6379
```

### 爬虫 `ingest/.env`

```env
DEEPSEEK_API_KEY=sk-xxx
OSS_ENDPOINT=oss-cn-beijing.aliyuncs.com
OSS_ACCESS_KEY_ID=xxx
OSS_ACCESS_KEY_SECRET=xxx
OSS_BUCKET_NAME=your-bucket
```

## 权限设计

| 角色 | 权限 |
|------|------|
| ROLE_USER | 浏览文章、点赞/收藏/评论、发布文章 |
| ROLE_CONTENT_ADMIN | + 管理分类、轮播图 |
| ROLE_SUPER_ADMIN | + 全部管理权限 |

## API 概览

| 路径 | 方法 | 权限 |
|------|------|------|
| `/user/login` `register` | POST | 公开 |
| `/article` | GET | 公开 |
| `/article` | POST/PUT/DELETE | 登录 |
| `/category` | GET | 公开 |
| `/category` | POST/PUT/DELETE | ADMIN |
| `/comment` | GET | 公开 |
| `/comment` | POST/DELETE | 登录 |
| `/article-like` | POST/DELETE | 登录 |
| `/article-favorite` | POST/DELETE | 登录 |
| `/admin/ingest/consume` | POST | 登录 |

## 数据库

数据库名: `big_event`，启动后端前需创建数据库并导入 `sql/schema.sql`
