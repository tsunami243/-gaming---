# Spring Security 整合与用户权限分级设计

## 概述

将现有 Spring Boot 项目从自定义 JWT 拦截器迁移到 Spring Security 框架，实现用户认证授权和角色分级管理。

## 技术变更

| 变更项 | 原方案 | 新方案 |
|--------|--------|--------|
| 认证框架 | 自定义 LoginInterceptor | Spring Security |
| 密码加密 | Md5Util | BCrypt |
| 令牌验证 | Filter + ThreadLocal | Spring Security Filter Chain |
| CSRF | 无 | 默认关闭 |

## 用户角色分级

| 角色 | 标识 | 说明 |
|------|------|------|
| 普通用户 | ROLE_USER | 注册用户，默认角色，可发布文章、评论、收藏 |
| 内容管理员 | ROLE_CONTENT_ADMIN | 管理文章分类、轮播图配置 |
| 超级管理员 | ROLE_SUPER_ADMIN | 用户管理、系统设置 |

## 数据库变更

### 1. 修改 user 表
```sql
ALTER TABLE `user` ADD COLUMN `role` varchar(50) DEFAULT 'ROLE_USER' COMMENT '角色';
```

### 2. 新建 role 表
```sql
CREATE TABLE `role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_alias` varchar(50) NOT NULL COMMENT '角色标识',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `role` (`role_name`, `role_alias`) VALUES
  ('普通用户', 'ROLE_USER'),
  ('内容管理员', 'ROLE_CONTENT_ADMIN'),
  ('超级管理员', 'ROLE_SUPER_ADMIN');
```

### 3. 新建 user_role 表（支持多角色）
```sql
CREATE TABLE `user_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '用户ID',
  `role_id` int NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 接口权限分配

### 公开接口（无需认证）
| 接口 | 路径 | 方法 |
|------|------|------|
| 登录 | `/user/login` | POST |
| 注册 | `/user/register` | POST |
| 文章列表 | `/article` | GET |
| 文章详情 | `/article/detail` | GET |
| 文章统计 | `/article/stats` | GET |
| 文章热度 | `/article/heat` | GET |
| 分类列表 | `/category` | GET |
| 分类详情 | `/category/detail` | GET |
| 评论列表 | `/comment/{articleId}` | GET |
| 评论树 | `/comment/{articleId}/tree` | GET |
| 浏览记录 | `/article-view/**` | POST |

### ROLE_USER+（已登录用户）
| 接口 | 路径 | 方法 | 说明 |
|------|------|------|------|
| 用户信息 | `/user/userInfo` | GET | |
| 更新用户 | `/user/update` | PUT | |
| 更新头像 | `/user/updateAvatar` | PATCH | |
| 更新密码 | `/user/updatePwd` | PATCH | |
| 发布文章 | `/article` | POST | |
| 更新文章 | `/article` | PUT | 仅自己的 |
| 删除文章 | `/article` | DELETE | 仅自己的 |
| 添加评论 | `/comment` | POST | |
| 回复评论 | `/comment/{articleId}/reply` | POST | |
| 删除评论 | `/comment/{id}` | DELETE | 仅自己的 |
| 收藏文章 | `/article-favorite/**` | POST/DELETE | |
| 点赞文章 | `/article-like/**` | POST/DELETE | |
| 点赞评论 | `/comment-like/**` | POST/DELETE | |
| 头像上传 | `/upload` | POST | |

### ROLE_CONTENT_ADMIN+（内容管理员+）
| 接口 | 路径 | 方法 | 说明 |
|------|------|------|------|
| 轮播图 | `/article/carousel` | GET | |
| 新增分类 | `/category` | POST | |
| 更新分类 | `/category` | PUT | |
| 删除分类 | `/category` | DELETE | |

### ROLE_SUPER_ADMIN（超级管理员）
| 接口 | 路径 | 方法 |
|------|------|------|
| 用户列表 | `/user/list` | GET |
| 删除用户 | `/user/{id}` | DELETE |

## Spring Security 配置要点

1. **密码加密器**：BCryptPasswordEncoder
2. **JWT过滤器**：在 UsernamePasswordAuthenticationFilter 之前执行，解析 Token 并设置 SecurityContext
3. **CSRF**：默认关闭（使用 Token 进行身份验证）
4. **认证失败处理**：返回 401 JSON 响应
5. **授权失败处理**：返回 403 JSON 响应

## 实现步骤

1. 添加 Spring Security 依赖
2. 创建 SecurityConfig 配置类
3. 创建 JwtAuthenticationFilter
4. 创建 UserDetailsServiceImpl
5. 修改 User 实体，实现 UserDetails 接口
6. 迁移现有接口到新的权限体系
7. 更新数据库表结构
8. 测试所有接口权限

## 兼容性说明

- 原有的 JWT Token 格式保留，继续用于身份验证
- ThreadLocalUtil 暂时保留，用于兼容现有代码
- 后续逐步迁移到 SecurityContextHolder