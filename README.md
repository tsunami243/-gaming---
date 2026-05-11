# 灵境 gaming - 后端服务

基于 Spring Boot + MyBatis + MySQL + Redis 构建的游戏新闻与社区平台后端服务。

## 技术栈

- **框架**: Spring Boot 3.x
- **ORM**: MyBatis + MyBatis-Plus
- **数据库**: MySQL 8.x
- **缓存**: Redis
- **认证**: JWT
- **分页**: PageHelper

## 功能特性

- 用户注册、登录、JWT认证
- 文章发布、管理、分类、标签
- 评论系统（支持嵌套回复）
- 文章收藏与点赞
- 浏览量追踪与热度计算
- 轮播图管理
- 文件上传（阿里云OSS）

## 项目结构

```
src/main/java/org/example/bigevent/
├── controller/     # REST API控制器
├── service/        # 业务逻辑层
├── mapper/         # MyBatis Mapper接口
├── pojo/           # 数据实体类
├── utils/          # 工具类
└── interceptors/   # 拦截器（JWT认证）
```

## 数据库

数据库名: `big_event`

启动前需创建数据库并导入 `sql/schema.sql` 中的表结构。

## 配置

配置文件: `src/main/resources/application.yml`

主要配置项：
- 数据库连接 (MySQL)
- Redis连接
- JWT密钥

## API端口

后端服务默认运行在 `http://localhost:8080`

## 运行

```bash
# 开发环境
./mvnw spring-boot:run

# 打包
./mvnw package
```