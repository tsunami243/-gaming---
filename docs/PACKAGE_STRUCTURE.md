# 包结构约定（分层规范）

> 2026-10-16 完成全项目包结构重构后确立。根包 `org.example.bigevent`。
> 规则：**一个包只放一种角色**。新增类必须放入下面对应包。

## 目录树

```
org.example.bigevent
├── BigEventApplication.java     启动类
├── anno/                       自定义注解（State）
├── config/                     配置类（SecurityConfig / PasswordConfig / SchedulingConfig）
├── controller/                 REST 控制器（含 v1 公开/管理、内部采集、push demo）
├── exception/                  全局异常处理（GlobalExceptionHandler / ApiV1ExceptionHandler）
├── filter/                     请求过滤器（ApiTraceIdFilter / IngestRequestBoundaryFilter）
├── mapper/                     MyBatis Mapper 接口（全部 @Mapper 注解，勿依赖 @MapperScan）
├── pojo/                       实体与枚举（Article / Game / WorkflowStatus / PushTask …）
│   └── dto/                    DTO：*Response / *Row / *Request / 事件与凭据 record
├── security/                   Spring Security 组件（仅 JWT 认证过滤器）
├── service/                    Service 接口（**只放接口**）
│   └── impl/                   Service 实现（@Service 类，含 UserDetailsServiceImpl）
├── task/                       定时任务（@Component + @Scheduled）
├── utils/                      工具类（含 JsonTags、TokenBucketRateLimiter、MockWechatPushClient）
└── validation/                 校验器实现
```

## 放置规则

| 新增内容 | 放哪 |
|---|---|
| REST 端点 | `controller/` |
| 业务接口 | `service/`（接口） |
| 业务实现 | `service/impl/`（`@Service`，命名 `xxxServiceImpl`） |
| 数据访问 | `mapper/`（`@Mapper` 接口；XML 放 `resources/org/example/bigevent/mapper/`） |
| 实体 / 枚举 | `pojo/` |
| 请求 / 响应 / Row / 事件 record | `pojo/dto/` |
| 过滤器 | `filter/` |
| 异常处理器 | `exception/` |
| 定时任务 | `task/` |
| 通用工具 | `utils/` |

## 关键约定

1. **Mapper 用 `@Mapper` 注解**，不使用 `@MapperScan`。因此 Mapper 换包不会破坏扫描。
2. **MyBatis XML 的 namespace / resultType 必须写 FQN**，移动 POJO 时同步更新（见 `PushTaskMapper.xml`）。
3. **`ApiV1ExceptionHandler` 用 `assignableTypes` 显式限定 v1 控制器**，不用 `basePackages`。
   原因：v1 与 legacy/内部控制器现同处 `controller` 包，包级匹配会把内部采集接口一并吞掉，导致其错误码从 400 变成 422（2026-10-16 实测回归）。新增 v1 控制器时需同步加入该列表。
4. **Service 一律「接口 + 实现」**，实现放 `service/impl/`；接口中的公开常量可被静态引用（如 `HeatService.DELTA_LIKE`）。
5. **公开接口不得直接返回 `pojo` 实体**，使用 `pojo/dto` 下的 DTO。

## 验证

```powershell
.\mvnw.cmd test          # 后端全部单测（MySQL 相关用例按门控跳过）
.\scripts\check.ps1      # 全量：OpenAPI + 后端 + 前端 build/test + Python
```
