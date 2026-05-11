# 灵境 gaming - 游戏资讯与社区平台

基于 Spring Boot + Vue 3 构建的游戏新闻与社区平台，支持文章发布、评论互动、用户收藏、热度计算等功能。

## 项目结构

```
gaming/
├── src/                    # 后端 Spring Boot 代码
├── gaming-news/            # 前端 Vue 3 代码
├── sql/                    # 数据库SQL脚本
├── pom.xml                 # 后端 Maven 配置
└── README.md               # 项目说明文档
```

## 技术栈

### 后端
- **框架**: Spring Boot 3.x
- **ORM**: MyBatis
- **数据库**: MySQL 8.x
- **缓存**: Redis
- **认证**: JWT

### 前端
- **框架**: Vue 3
- **构建工具**: Vite
- **HTTP客户端**: axios
- **UI**: Element Plus

## 功能特性

- 用户注册、登录、JWT认证
- 文章发布、管理、分类、标签
- 评论系统（支持嵌套回复）
- 文章收藏与点赞
- 浏览量追踪与热度计算
- 轮播图管理

## 快速启动

### 后端

```bash
cd gaming
./mvnw spring-boot:run
```

### 前端

```bash
cd gaming/gaming-news
npm install
npm run dev
```

## 数据库

数据库名: `big_event`

启动后端前需创建数据库并导入 `sql/schema.sql` 中的表结构。

## API端口

- 后端: `http://localhost:8080`
- 前端: `http://localhost:5173`