# 浏览器回归验证范围

项目根 PowerShell：

```powershell
.\scripts\check.ps1 -Browser
```

前置：本机已安装 Python Playwright 及其 Chromium。脚本不安装或下载软件，缺失依赖时失败，不把跳过伪装成通过。

`check-web-browser.py` 启动随机 localhost 端口的临时静态服务，加载本次 Vue 生产构建，在真实无头 Chromium 下分别验证 1440×900 和 390×844。覆盖首屏一次文章请求、四游戏入口、游戏/类型筛选、下一页、摘要详情/原文链接、未知原文时间对应页面、404、503重试、空态、禁用消息路由及详情横向溢出检查。所有非本地网络请求阻断；不会访问原文或付费服务。finally 关闭浏览器和临时 HTTP 服务。

**明确限制**：API 为测试回显数据，不是 Spring/MySQL；该验证证明生产 Vue 构建的浏览器交互，不证明真实数据库到浏览器数据一致性、Nginx生产代理、真实登录或服务器资源约束。后者仍需独立联调验收。不能据此关闭 WEB-01 全部门禁。

## 真实 Spring/MySQL 浏览器链路（已执行）

设置受限 GAMEBOI_FLYWAY_TEST_URL，准备本项目独立临时MySQL实例和本次Vue生产构建后，设置 GAMEBOI_BROWSER_E2E=1 并运行 IngestReceiptMySqlTransactionTest。该测试在提交入库、测试发布期间调用 scripts/check-web-live.py：临时本地静态/转发服务承载生产Vue，将公开API原样转发至本轮Spring随机端口。1440px和390px真实Chromium均验证游戏/类型筛选、数据库文章导航、来源/多游戏标签、摘要/原文链接，禁止所有外网请求。

本次环境需测试JVM参数 `-Djdk.net.unixdomain.tmpdir=C:\mysql-gameboi-http-test` 规避默认临时socket路径故障。该路径是本项目已建立的测试目录，不是业务数据。测试结束关闭MySQL，不自动删除目录。

证据在target/live-e2e-current.log、target/live-web-current.log、target/live-web-1440.png与target/live-web-390.png。截图在路由动画opacity=1后获取并人工查看；正文与元信息正常换行，无横向溢出。

此验证使用测试事件与SQL显式发布，不证明采集模型质量、管理员认证/发布链路、Redis互动或生产Nginx。测试标题/来源为固定输入，不代表真实游戏内容，未来2099发布时间仅为测试排序固定数据。
