# DEVELOPMENT.md — GameBoi 工具层操作手册

面向接手项目的开发者和 AI 助手。覆盖 shell 命令、文件地图、API 参考、测试指南、故障排查。

---

## 1. Shell 命令清单

### 1.1 后端 (Spring Boot + Maven)

```bash
# 编译
./mvnw compile

# 编译 + 打包（跳过测试）
./mvnw clean package -DskipTests

# 启动开发服务器（热重载需要 devtools 依赖）
./mvnw spring-boot:run

# 运行测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=ArticleControllerTest

# 查看依赖树
./mvnw dependency:tree

# 强制更新 SNAPSHOT 依赖
./mvnw clean compile -U
```

### 1.2 前端 (Vue 3 + Vite)

```bash
cd gaming-news

# 安装依赖
npm install

# 启动开发服务器（:5173，热重载）
npm run dev

# 生产构建
npm run build

# 预览生产构建
npm run preview

# 运行前端测试（vitest）
npx vitest

# 运行单次测试
npx vitest run

# 带覆盖率
npx vitest --coverage
```

### 1.3 数据库 (MySQL)

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS big_event DEFAULT CHARSET utf8mb4;"

# 导入表结构
mysql -u root -p big_event < sql/schema.sql

# 查看所有表
mysql -u root -p big_event -e "SHOW TABLES;"

# 查看表结构
mysql -u root -p big_event -e "DESC article;"

# 快速检查连接
mysql -u root -p123456 -e "SELECT 1" big_event
```

### 1.4 Redis

```bash
# 启动 Redis（Windows 需在 WSL/Docker 中运行，或使用 Windows 版 redis-server）
redis-server

# 检查 Redis 是否运行
redis-cli ping

# 查看队列状态
redis-cli LLEN ingest:article_queue

# 清空队列（开发环境）
redis-cli DEL ingest:article_queue

# 查看所有 key
redis-cli KEYS "*"

# 监控实时命令
redis-cli MONITOR
```

### 1.5 爬虫 (Python)

```bash
cd ingest

# 安装依赖
pip install -r requirements.txt

# 试运行（不实际入库）
python main.py --dry-run

# 正式运行
python main.py

# 指定新闻源
python main.py --source gamersky

# 查看统计
python main.py --stats
```

### 1.6 Git 工作流

```bash
# 查看最近提交
git log --oneline -5

# 查看文件变更
git status
git diff

# 创建功能分支
git checkout -b feature/your-feature-name

# 提交（遵循项目风格：英文消息，简洁）
git add <specific-files>
git commit -m "feat: description"  # 或 fix/refactor/chore/docs
```

### 1.7 验证冒烟测试

```bash
# 后端心跳
curl http://localhost:8080/article?pageNum=1&pageSize=5

# 前端可访问
curl -o /dev/null -w "%{http_code}" http://localhost:5173/

# 前端→后端代理
curl -o /dev/null -w "%{http_code}" http://localhost:5173/api/article?pageNum=1&pageSize=3

# 分类列表
curl http://localhost:8080/category

# 轮播图
curl http://localhost:8080/article/carousel?limit=5

# 用户登录
curl -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'
```

### 1.8 一键环境检测

```bash
# 仅检查环境（跳过编译测试）
./init.sh --check-only

# 完整初始化（编译 + 测试）
./init.sh

# 跳过测试
./init.sh --skip-tests
```

---

## 2. 文件地图

### 2.1 项目根目录

```
gaming/
├── CLAUDE.md              # AI 助手行为规则 & 项目概览
├── DEVELOPMENT.md          # 本文件 — 工具层操作手册
├── README.md               # 项目 README（面向人类开发者）
├── claude-progress.md      # 会话进度 & 已验证状态
├── feature_list.json       # 功能列表（唯一事实来源）
├── init.sh                 # 环境检测 & 验证入口
├── pom.xml                 # Maven 项目定义（依赖、插件、Java 17）
├── mvnw / mvnw.cmd         # Maven Wrapper（无需全局安装 Maven）
├── .gitignore              # Git 忽略规则
├── .gitattributes          # Git 行尾设置
├── sql/
│   └── schema.sql          # 数据库 DDL（建表 + 角色种子数据）
├── src/
│   ├── main/java/org/example/bigevent/   # [见 2.2]
│   └── main/resources/
│       └── application.yml               # 数据库/Redis/JWT 配置
├── gaming-news/            # [见 2.3] Vue 3 前端
├── ingest/                 # [见 2.4] Python 爬虫
└── target/                 # Maven 构建输出（gitignore 已排除）
```

### 2.2 后端 — `src/main/java/org/example/bigevent/` (50 个文件)

```
BigEventApplication.java           # @SpringBootApplication 入口，启用 @EnableScheduling

config/
├── SecurityConfig.java            # Spring Security 配置：JWT 过滤器、CORS、角色权限、公开/受保护路径
└── PasswordConfig.java            # 密码编码器 Bean (BCryptPasswordEncoder)

security/
├── JwtAuthenticationFilter.java   # OncePerRequestFilter：从 Authorization 头提取 Bearer Token，
│                                  #   验证 JWT + Redis 双校验，设置 SecurityContextHolder
└── UserDetailsServiceImpl.java    # UserDetailsService：从 DB 加载用户，用于 Spring Security 认证

controller/
├── ArticleController.java         # /article           — 文章 CRUD、轮播、热度、统计
├── ArticleFavoriteController.java # /article-favorite  — 收藏/取消/检查/统计
├── ArticleLikeController.java     # /article-like      — 点赞/取消/检查/统计
├── CategoryController.java        # /category          — 分类 CRUD
├── CommentController.java         # /comment           — 评论 CRUD、树形结构、回复
├── CommentLikeController.java     # /comment-like      — 评论点赞/取消/检查/统计
├── FileUploadController.java      # /upload            — 图片上传至阿里云 OSS
├── IngestController.java          # /admin/ingest/consume — 手动消费 Redis 队列中的爬虫文章
├── UserController.java            # /user              — 注册、登录、用户信息、修改密码、头像
└── ViewHistoryController.java     # /article-view      — 文章阅读记录、去重统计

service/
├── ArticleService.java            # 文章业务接口
├── ArticleFavoriteService.java    # 收藏业务接口
├── ArticleLikeService.java        # 点赞业务接口
├── CategoryService.java           # 分类业务接口
├── CommentService.java            # 评论业务接口
├── CommentLikeService.java        # 评论点赞业务接口
├── UserService.java               # 用户业务接口
└── ViewHistoryService.java        # 阅读历史业务接口

service/impl/
├── ArticleServicelmpl.java        # 文章服务实现（热度公式：views*1 + uniqueViews*3 + favorites*5 + likes*2 + comments*3）
├── ArticleFavoriteServiceImpl.java
├── ArticleLikeServiceImpl.java
├── ArticleIngestConsumer.java     # @Scheduled(fixedDelay=10s) 定时从 Redis 队列消费爬虫文章
├── CategoryServiceImpl.java
├── CommentServiceImpl.java        # 含树形评论组装逻辑
├── CommentLikeServiceImpl.java
├── UserServiceImpl.java           # 注册时 BCrypt 加密，修改密码时清除 Redis Token
└── ViewHistoryServiceImpl.java    # 24 小时去重（登录用户按 userId，游客按 IP）

mapper/
├── ArticleMapper.java             # 文章 MyBatis Mapper（含动态过滤 XML）
├── ArticleFavoriteMapper.java
├── ArticleLikeMapper.java
├── ArticleViewHistoryMapper.java
├── CategoryMapper.java
├── CommentMapper.java
├── CommentLikeMapper.java
└── UserMapper.java

pojo/
├── Article.java                   # 文章实体（含 @State 校验注解）
├── ArticleFavorite.java           # 收藏实体
├── ArticleLike.java               # 点赞实体
├── ArticleViewHistory.java        # 浏览历史实体
├── Category.java                  # 分类实体（含 Add/Update 校验分组）
├── Comment.java                   # 评论实体（含 replies 非 DB 字段用于树形组装）
├── CommentLike.java               # 评论点赞实体
├── PageBean.java                  # 分页通用包装（total + items）
├── Result.java                    # 统一响应体（code + message + data）
└── User.java                      # 用户实体（实现 UserDetails）

anno/
└── State.java                     # @State 自定义校验注解（值必须为 "草稿" 或 "已发布"）

validation/
└── StateValidation.java           # @State 注解的 ConstraintValidator 实现

exception/
└── GlobalExceptionHandler.java    # @RestControllerAdvice 全局异常捕获 → Result.error()

utils/
├── AliOssUtil.java                # 阿里云 OSS V4 签名上传工具（bucket: big-event-by-tsunami）
├── JwtUtil.java                   # JJWT HMAC-SHA256 生成/解析 Token
├── Md5Util.java                   # MD5 工具（存在但未使用，项目实际用 BCrypt）
└── ThreadLocalUtil.java           # ThreadLocal 工具（存储当前请求的 JWT claims）
```

### 2.3 前端 — `gaming-news/` (41 个源文件)

```
├── index.html                     # HTML 入口（引入 Google Fonts: Press Start 2P, ZCOOL KuaiLe）
├── package.json                   # 项目清单 & npm scripts
├── vite.config.js                 # Vite 配置（Vue 插件、/api 代理到 :8080、vitest 配置）
├── README.md                      # 前端文档
├── .gitignore
├── .vscode/extensions.json        # 推荐 Volar 扩展
├── public/
│   ├── favicon.svg
│   └── icons.svg
└── src/
    ├── main.js                    # 应用入口：创建 Vue、安装 Router/Pinia/NaiveUI
    ├── App.vue                    # 根组件（n-config-provider + n-message-provider + router-view）
    ├── style.css                  # 基础 body 样式（monospace 字体、细滚动条）
    ├── assets/
    │   ├── hero.png / logo.png
    │   ├── vite.svg / vue.svg
    │   ├── styles/
    │   │   ├── global.css         # CSS 变量（游戏配色）、全局 reset、Naive UI 组件覆盖
    │   │   └── apple.css          # Apple Design System 令牌（glass/卡片/按钮/输入框样式）
    │   └── logos/
    │       ├── main.jpg           # 默认游戏 logo
    │       ├── 三角洲行动.png      # Delta Force logo
    │       ├── 无畏契约.jpg        # Valorant logo
    │       ├── apex.jpg           # Apex Legends logo
    │       └── cs2.png            # CS2 logo
    ├── router/
    │   └── index.js               # 8 条路由 + beforeEach 导航守卫
    │                              #   公开: / /home /article/:id
    │                              #   需登录: /user /publish /message
    │                              #   游客: /login /register
    ├── stores/
    │   ├── article.js             # Pinia Store: 文章列表、分类筛选、分页
    │   ├── category.js            # Pinia Store: 动态分类（缓存加载）、颜色映射
    │   └── user.js                # Pinia Store: 认证状态、登录/注册/登出
    ├── api/
    │   ├── article.js             # 22 个文章/评论/点赞/收藏 API 函数
    │   └── user.js                # 6 个用户认证/信息 API 函数
    ├── utils/
    │   ├── request.js             # Axios 实例（baseURL=/api, 10s 超时, Bearer 注入, 401 重定向）
    │   ├── constants.js           # 常量：BASE_URL, DEFAULT_PAGE_SIZE, 默认图片
    │   ├── category.js            # 硬编码游戏分类（首页侧边栏用，与动态分类可能重叠）
    │   └── format.js              # stripHtml() 去除 HTML 标签
    ├── components/
    │   ├── HelloWorld.vue         # 脚手架残留（未被任何路由使用）
    │   ├── TopNav.vue             # 固定顶栏：GAMEBOI logo、+POST 按钮、用户按钮、滚动隐藏
    │   ├── BottomNav.vue          # 固定底栏：HOME/HOT/POST/MSG/ME 5 个 GameBoy 风格按钮
    │   ├── CategoryTabs.vue       # 水平分类标签栏（Apple glass 风格）
    │   ├── SwiperBanner.vue       # Naive UI 轮播图（5 篇推荐文章）
    │   └── ArticleList.vue        # 文章卡片列表（封面图、标题、摘要、分类标签、时间）
    ├── views/
    │   ├── LandingView.vue        # /      — 开机动画页（GameBoy LCD 像素美学、Matrix 字符雨）
    │   ├── HomeView.vue           # /home  — 主页（侧边栏分类 + 轮播 + 文章流）
    │   ├── ArticleDetailView.vue  # /article/:id — 文章详情（内容、交互按钮、评论区）
    │   ├── LoginView.vue          # /login — 登录页
    │   ├── RegisterView.vue       # /register — 注册页
    │   ├── UserView.vue           # /user  — 用户中心（需登录）
    │   ├── PublishView.vue        # /publish — 发布文章（需登录）
    │   └── MessageView.vue        # /message — 消息页（需登录，当前数据为硬编码 mock）
    └── test/
        └── setup.js               # vitest setup（afterEach cleanup）— 无实际测试文件存在

gaming/                             # 空壳目录（无实际内容，可能是构建产物残留）
```

### 2.4 爬虫 — `ingest/` (14 个文件)

```
ingest/
├── main.py                        # CLI 入口（Click）：--source, --dry-run, --stats
├── settings.py                    # 环境变量配置加载（DeepSeek/Redis/OSS/爬虫参数）
├── requirements.txt               # Python 依赖：httpx, beautifulsoup4, lxml, openai, redis, oss2 等
├── .env                           # 实际密钥文件（DeepSeek API Key, OSS 凭证）
├── spiders/
│   ├── __init__.py                # 导出 BaseSpider, GamerSkySpider
│   ├── base.py                    # 抽象基类：HTTP 请求、UA 轮换、crawl() 管线
│   └── gamersky.py                # 游民星空爬虫实现：URL 提取、内容解析
├── models/
│   ├── __init__.py
│   └── article.py                 # Pydantic 模型：RawArticle、ProcessedArticle、CATEGORY_MAP
├── agent/
│   ├── __init__.py
│   ├── deepseek_client.py         # DeepSeek AI 客户端：HTML → JSON（清洗/分类/标签/摘要/过滤）
│   └── prompts.py                 # System Prompt 模板（10 字段结构化输出）
├── storage/
│   ├── __init__.py                # 导出 RedisClient, OssClient
│   ├── redis_client.py            # Redis: URL 去重、统计计数器、健康检查
│   └── oss_client.py              # 阿里云 OSS: 图片下载 → 上传 → HTML 链接替换
└── mq/
    ├── __init__.py                # 导出 QueueProducer
    └── producer.py                # Redis List 消息队列生产者（RPUSH JSON 到 ingest:article_queue）
```

---

## 3. API 参考

所有接口统一返回 `Result<T>` 格式：`{"code":0,"message":"操作成功","data":...}`
需要认证的接口在 Header 中传 `Authorization: Bearer <jwt_token>`。

### 3.1 文章

#### `GET /article` — 分页文章列表

```bash
curl "http://localhost:8080/article?pageNum=1&pageSize=5&categoryId=5&state=已发布"
```

```json
{
  "code": 0, "message": "操作成功",
  "data": {
    "total": 29,
    "items": [{
      "id": 4, "title": "三角洲行动x肯德基联动指南",
      "content": "5月8日起购买任意联动套餐可得武器外观...",
      "coverImg": "", "state": "已发布", "categoryId": 5,
      "createTime": "2026-05-09T12:08:26", "updateTime": "2026-05-09T12:08:26",
      "views": 0, "uniqueViews": 0, "favorites": 0, "tags": "", "isCarousel": 0
    }]
  }
}
```

参数：`pageNum`(必填) `pageSize`(必填) `categoryId`(选填) `state`(选填: 草稿/已发布)

#### `GET /article/detail` — 文章详情

```bash
curl "http://localhost:8080/article/detail?id=4"
```

```json
{
  "code": 0, "message": "操作成功",
  "data": { "id": 4, "title": "...", "content": "<p>...</p>", ... }
}
```

#### `POST /article` — 新增文章（需认证）

```bash
curl -X POST http://localhost:8080/article \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"title":"测试文章","content":"<p>正文</p>","state":"草稿","categoryId":5,"coverImg":"","tags":"[]"}'
```

#### `PUT /article` — 更新文章（需认证，仅创建者）

```bash
curl -X PUT http://localhost:8080/article \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"id":100,"title":"修改后标题","content":"<p>修改后正文</p>","state":"已发布","categoryId":5}'
```

#### `DELETE /article` — 删除文章（需认证，仅创建者）

```bash
curl -X DELETE "http://localhost:8080/article?id=100" \
  -H "Authorization: Bearer <token>"
```

#### `GET /article/carousel` — 轮播文章

```bash
curl "http://localhost:8080/article/carousel?limit=5"
```

#### `GET /article/stats` — 文章统计

```bash
curl "http://localhost:8080/article/stats?articleId=4"
```

```json
{ "code": 0, "data": { "views": 10, "uniqueViews": 5, "likeCount": 3, "favoriteCount": 2, "commentCount": 7 } }
```

#### `GET /article/heat` — 文章热度分

```bash
curl "http://localhost:8080/article/heat?articleId=4"
```

热度公式：`views × 1 + uniqueViews × 3 + favorites × 5 + likes × 2 + comments × 3`

---

### 3.2 用户

#### `POST /user/register` — 注册

```bash
curl -X POST http://localhost:8080/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}'
```

#### `POST /user/login` — 登录

```bash
curl -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456","rememberme":true}'
```

```json
{ "code": 0, "message": "操作成功", "data": "eyJhbGciOiJIUzI1NiJ9..." }
```

`rememberme=true` → Token 有效期 3 天，否则默认 12 小时。

#### `GET /user/userInfo` — 当前用户信息（需认证）

```bash
curl http://localhost:8080/user/userInfo \
  -H "Authorization: Bearer <token>"
```

#### `PUT /user/update` — 更新用户信息（需认证）

```bash
curl -X PUT http://localhost:8080/user/update \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"nickname":"新昵称","email":"user@example.com"}'
```

#### `PATCH /user/updateAvatar` — 更新头像（需认证）

```bash
curl -X PATCH "http://localhost:8080/user/updateAvatar?avatarUrl=https://example.com/avatar.jpg" \
  -H "Authorization: Bearer <token>"
```

#### `PATCH /user/updatePwd` — 修改密码（需认证）

```bash
curl -X PATCH http://localhost:8080/user/updatePwd \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"old_pwd":"123456","new_pwd":"newpass","re_pwd":"newpass"}'
```

---

### 3.3 分类

#### `GET /category` — 所有分类

```bash
curl http://localhost:8080/category
```

#### `POST /category` — 新增分类（需 CONTENT_ADMIN 或 SUPER_ADMIN 角色）

```bash
curl -X POST http://localhost:8080/category \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"categoryName":"新分类","categoryAlias":"new-cat"}'
```

#### `PUT /category` — 更新分类（需管理员角色）

```bash
curl -X PUT http://localhost:8080/category \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"id":1,"categoryName":"改名后","categoryAlias":"renamed"}'
```

#### `DELETE /category` — 删除分类（需管理员角色）

```bash
curl -X DELETE "http://localhost:8080/category?id=10" \
  -H "Authorization: Bearer <token>"
```

---

### 3.4 评论

#### `GET /comment/{articleId}` — 文章评论（扁平列表）

```bash
curl http://localhost:8080/comment/4
```

#### `GET /comment/{articleId}/tree` — 文章评论（树形结构）

```bash
curl http://localhost:8080/comment/4/tree
```

```json
{
  "code": 0,
  "data": [{
    "id": 1, "content": "好文章", "parentId": null, "userId": 1, "userName": "admin",
    "createTime": "...", "likeCount": 5,
    "replies": [{
      "id": 3, "content": "同意", "parentId": 1, "replyId": null, "replyUserName": null,
      "userId": 2, "userName": "user2", ...
    }]
  }]
}
```

#### `POST /comment` — 发表评论（需认证）

```bash
curl -X POST http://localhost:8080/comment \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"articleId":4,"content":"写得好"}'
```

#### `POST /comment/{articleId}/reply` — 回复评论（需认证）

```bash
curl -X POST "http://localhost:8080/comment/4/reply?parentId=1&replyId=3&content=说得对" \
  -H "Authorization: Bearer <token>"
```

#### `DELETE /comment/{id}` — 删除评论（需认证，仅评论者）

```bash
curl -X DELETE http://localhost:8080/comment/5 \
  -H "Authorization: Bearer <token>"
```

---

### 3.5 点赞 & 收藏

所有点赞/收藏接口结构一致，以文章点赞为例：

```bash
# 点赞
curl -X POST "http://localhost:8080/article-like?articleId=4" \
  -H "Authorization: Bearer <token>"

# 取消点赞
curl -X DELETE "http://localhost:8080/article-like?articleId=4" \
  -H "Authorization: Bearer <token>"

# 检查是否已点赞
curl "http://localhost:8080/article-like/check?articleId=4" \
  -H "Authorization: Bearer <token>"

# 点赞数
curl "http://localhost:8080/article-like/count?articleId=4"

# 我的点赞列表
curl http://localhost:8080/article-like/list \
  -H "Authorization: Bearer <token>"
```

评论点赞对应路径前缀 `/comment-like`，收藏对应 `/article-favorite`，均需认证（count 除外）。

---

### 3.6 其他

#### `POST /upload` — 图片上传（需认证）

```bash
curl -X POST http://localhost:8080/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/image.jpg"
```
限制：≤ 5MB，仅 jpg/jpeg/png/gif/webp/bmp。

#### `POST /article-view` — 记录浏览

```bash
curl -X POST "http://localhost:8080/article-view?articleId=4" \
  -H "Authorization: Bearer <token>"  # 选填，游客可不带
```

#### `GET /article-view/count` — 总浏览量

```bash
curl "http://localhost:8080/article-view/count?articleId=4"
```

#### `GET /article-view/unique-count` — 独立用户浏览量

```bash
curl "http://localhost:8080/article-view/unique-count?articleId=4"
```

#### `POST /admin/ingest/consume` — 手动消费爬虫队列

```bash
curl -X POST http://localhost:8080/admin/ingest/consume
```

---

### 3.7 Spring Security 权限矩阵

| 路径 | 方法 | 认证要求 |
|---|---|---|
| `/test/**` | 全部 | 公开 |
| `/user/login`, `/user/register` | POST | 公开 |
| `/article-view/**` | 全部 | 公开 |
| `/article/**` | GET | 公开 |
| `/category/**` | GET | 公开 |
| `/comment/**` | GET | 公开 |
| `/category` | POST/PUT/DELETE | CONTENT_ADMIN 或 SUPER_ADMIN |
| `/article/**` | POST/PUT/DELETE | 需登录 |
| `/comment/**` | POST/DELETE | 需登录 |
| `/upload` | POST | 需登录 |
| 其他所有 | 全部 | 需登录 |

---

## 4. 测试指南

### 4.1 当前状态

| 层 | 测试框架 | 测试文件 | 状态 |
|---|---|---|---|
| 后端 | JUnit 5 + Mockito（pom.xml 已引入 spring-boot-starter-test） | 无 | `src/test/` 目录不存在 |
| 前端 | vitest 4 + @vue/test-utils 2 + jsdom（vite.config.js 已配置） | 0 个 `.test.js` | 仅 `src/test/setup.js` stub |

### 4.2 后端测试模式

测试文件放在 `src/test/java/org/example/bigevent/`，镜像 main 的包结构。

```java
// Controller 层测试示例 — src/test/java/.../controller/ArticleControllerTest.java
@WebMvcTest(ArticleController.class)
@Import(SecurityConfig.class)
class ArticleControllerTest {

    @MockBean
    private ArticleService articleService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnArticleList() throws Exception {
        PageBean<Article> page = new PageBean<>(1L, List.of());
        when(articleService.list(1, 5, null, null)).thenReturn(page);

        mockMvc.perform(get("/article?pageNum=1&pageSize=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }
}
```

```java
// Service 层测试示例 — src/test/java/.../service/impl/UserServiceImplTest.java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldRegisterUser() {
        when(userMapper.findByUsername("newuser")).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("$2a$...");
        userService.register("newuser", "123456");
        verify(userMapper).add("newuser", "$2a$...");
    }
}
```

### 4.3 前端测试模式

```js
// 组件测试示例 — src/components/__tests__/ArticleList.spec.js
import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import ArticleList from '../ArticleList.vue'

// Mock API
vi.mock('@/api/article', () => ({
  getArticleList: vi.fn(() => Promise.resolve({
    code: 0,
    data: { total: 2, items: [{ id: 1, title: 'Test', ... }] }
  }))
}))

describe('ArticleList', () => {
  it('renders article cards', async () => {
    const wrapper = mount(ArticleList, {
      props: { categoryId: null },
      global: { stubs: { RouterLink: true } }
    })
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('Test')
  })
})
```

### 4.4 运行测试

```bash
# 后端全部测试
./mvnw test

# 后端单个测试类
./mvnw test -Dtest=UserServiceImplTest

# 前端全部测试
cd gaming-news && npx vitest run

# 前端 watch 模式（开发时用）
cd gaming-news && npx vitest

# 前端覆盖率
cd gaming-news && npx vitest --coverage
```

---

## 5. 故障排查

### Redis 未启动

**症状**：后端日志每 10 秒出现 `RedisConnectionFailureException: Connection refused: localhost/127.0.0.1:6379`

**影响**：JWT Token 双验证不可用（但登录和 Token 生成本身不依赖 Redis）；`ArticleIngestConsumer` 爬虫队列消费失败

**修复**：
```bash
# Docker（推荐）
docker run -d --name redis -p 6379:6379 redis:7

# WSL
sudo apt install redis && sudo service redis-server start

# Windows 原生
# 下载 Redis for Windows 或使用 Memurai
```

**验证**：
```bash
redis-cli ping  # 期望返回 PONG
```

### Python 损坏

**症状**：`python3 --version` 报 `0x80070002`（找不到 `D:\python.exe`）

**影响**：爬虫模块完全不工作；`init.sh` 因 `set -e` 直接退出

**修复**：从 python.org 下载安装 Python 3.11+，确认安装路径在 PATH 中

**验证**：
```bash
python --version  # 期望 Python 3.11.x 或更高
```

### init.sh 提前退出

**症状**：`./init.sh --check-only` 在 Python 检查时 exit code 2

**原因**：脚本使用了 `set -euo pipefail`，Python 检查的 `python3 --version` 执行失败触发立即退出

**绕过**：修复 Python 安装，或手动运行各个检查项（跳过 init.sh）

### MySQL 连接失败

**症状**：`CommunicationsException: Communications link failure`

**检查**：
```bash
mysql -u root -p123456 -e "SELECT 1"
```

**修复**：确认 MySQL 服务已启动（Windows: `services.msc` 找到 MySQL80 并启动）

### 端口被占用

**症状**：`Port 8080 is already in use` 或 `Port 5173 is already in use`

**排查**：
```bash
# Windows — 查找占用端口的进程
netstat -ano | findstr :8080
# 记下 PID，去任务管理器结束进程
```

### 前端 401 循环重定向

**症状**：浏览器不断从 `/login` 重定向到自身

**原因**：JWT Token 过期或无效，`request.js` 的 401 拦截器触发重定向循环

**修复**：清除 `localStorage` 中的 token 后重新登录

---

## 6. 缺失项速查

以下是在第一次全项目验证中发现的项目缺口：

| 缺口 | 优先级 | 说明 |
|---|---|---|
| Redis 服务未运行 | 高 | 影响 JWT 双验证 + 爬虫队列 |
| Python 环境损坏 | 中 | 爬虫模块不可用 |
| 后端无单元测试 | 中 | `src/test/` 目录不存在 |
| 前端无测试文件 | 中 | vitest 已配置但无实际测试 |
| 无 CI/CD 流水线 | 低 | 无 `.github/workflows/`、Jenkinsfile 等 |
| 无 Docker 支持 | 低 | 无 Dockerfile、docker-compose.yml |
| 无 lint 配置 | 低 | 前后端均未配置 ESLint/Checkstyle |
| `ingest/.env.example` 缺失 | 低 | 新开发者需自行创建 `.env` |
| `HelloWorld.vue` 脚手架残留 | 低 | 未被任何路由引用 |
| `Md5Util.java` 未使用 | 低 | 项目实际使用 BCrypt |
