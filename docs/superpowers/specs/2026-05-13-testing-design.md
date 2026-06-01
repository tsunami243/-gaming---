# Gaming News System - Testing Design

## Date: 2026-05-13

## Overview

前后端分离的游戏新闻系统，包含文章管理、评论系统、点赞收藏、用户认证和分权管理功能。

## Project Structure

- **Backend**: `gaming/` - Spring Boot 4.0.5 + MyBatis + MySQL + Redis
- **Frontend**: `gaming/gaming-news/` - Vue 3 + Vite + Naive UI + Pinia

## Test Architecture

### Backend Test Structure

```
gaming/src/test/java/org/example/bigevent/
├── BigEventApplicationTests.java          # 基础测试
├── controller/
│   ├── ArticleControllerTest.java         # 文章API测试
│   ├── CommentControllerTest.java         # 评论API测试
│   └── UserControllerTest.java            # 用户/认证API测试
├── service/
│   └── ArticleServiceTest.java            # 文章Service测试
└── security/
    └── PermissionTest.java               # 分权权限测试（核心）
```

### Technology Stack

- **Framework**: Spring Boot Test + JUnit 5
- **Database**: H2 in-memory database (test isolation)
- **Mock**: `@WithMockUser` for role simulation
- **HTTP Testing**: MockMvc for REST API
- **Security**: Spring Security with JWT

## Permission Control (Core)

### Roles

| Role | Description |
|------|-------------|
| ROLE_GUEST | 游客（未登录） |
| ROLE_USER | 普通用户 |
| ROLE_ADMIN | 管理员 |

### Permission Matrix

| Resource | Action | Guest | User | Admin |
|----------|--------|-------|------|-------|
| Article | List/View | ✅ | ✅ | ✅ |
| Article | Create | ❌ | ✅ | ✅ |
| Article | Update Own | ❌ | ✅ | ✅ |
| Article | Delete Own | ❌ | ✅ | ✅ |
| Article | Delete Others | ❌ | ❌ | ✅ |
| Comment | Create | ❌ | ✅ | ✅ |
| Comment | Delete Own | ❌ | ✅ | ✅ |
| Comment | Delete Others | ❌ | ❌ | ✅ |
| Like/Favorite | Toggle | ❌ | ✅ | ✅ |

## Test Cases

### 1. Permission Tests (`PermissionTest.java`)

```java
// Article permission
- guest_cannot_create_article()
- user_can_create_article()
- user_cannot_delete_others_article()
- admin_can_delete_any_article()

// Comment permission
- guest_cannot_create_comment()
- user_can_delete_own_comment()
- admin_can_delete_any_comment()

// Like/Favorite permission
- guest_cannot_toggle_like()
- user_can_toggle_favorite()
```

### 2. Controller Tests

**ArticleControllerTest.java**
```java
- test_list_articles()
- test_get_article_by_id()
- test_create_article_success()
- test_create_article_unauthorized()
- test_update_article()
- test_delete_article()
```

**CommentControllerTest.java**
```java
- test_add_comment()
- test_delete_comment()
- test_list_comments()
```

**UserControllerTest.java**
```java
- test_register()
- test_login()
- test_get_current_user()
```

### 3. Service Tests

**ArticleServiceTest.java**
```java
- test_add_article()
- test_update_article()
- test_delete_article()
- test_list_articles_with_pagination()
- test_increment_view_count()
```

## Frontend Testing

```
gaming/gaming-news/src/
├── components/
│   └── Article.spec.js       # Vitest component test
├── views/
│   └── Home.spec.js
└── api/
    └── article.spec.js      # API mock test
```

## Coverage Target

| Layer | Target |
|-------|--------|
| Controller | 80%+ |
| Service | 80%+ |
| Security | 100% |

## Implementation Order

1. Setup H2 test database configuration
2. Create base test fixtures and utilities
3. Implement PermissionTest (core - role-based access)
4. Implement Controller tests
5. Implement Service tests
6. Frontend Vitest setup and tests