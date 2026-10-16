# 首期来源验证登记

更新时间：2026-09-20。关联：[设计第5节](PROJECT_DESIGN.md#5-采集管线与调度)。

## 1. 状态定义

- CANDIDATE：只找到入口方向或参考代码，不能启用自动采集/发布。
- FETCH_VERIFIED：实际受控请求与解析样本已验证；仍未完成内容质量门禁。
- SHADOW：影子运行，可采样供审核但不自动发布。
- AUTO_ENABLED：通过来源与质量评测，允许规则内自动发布。
- PAUSED：失效、限流、质量或容量原因暂停。

截至 2026-10-16，VALORANT、Apex、ALGS、CS2 与 Delta Force 国服官网入口均已有受控列表/详情请求和离线 fixture 解析证据；它们最多是 `FETCH_VERIFIED` 或已注册的 `SHADOW`，**不存在 AUTO_ENABLED 来源**。

## 2. 初始候选

| sourceCode（暂定） | 游戏 | 来源方向/证据 | 当前状态 | 下一验证 |
|---|---|---|---|---|
| cs2-official | cs2 | Valve Steam News API（appid=730）；2026-09-22 本机 httpx HTTP/1.1 与 HTTP/2 均 200，适配器实测 9 条第一方 Community Announcements。API 会混入第三方 feed，现已强制只接收 `Community Announcements`/Steam community URL 前缀 | FETCH_VERIFIED | 影子运行、内容质量/重复样本、自动发布门禁；任何非第一方 feed 均不得进入该官方来源 |
| apex-official | apex-legends | EA 官方 `ea.com/games/apex-legends/news`；2026-09-20 Python httpx 列表/详情 200，Patch Notes 可抽取标题与正文，列表卡片包含原始日期 | FETCH_VERIFIED | 影子运行、详情日期与列表日期关联、中文摘要质量、自动发布仍关闭 |
| algs-official | apex-legends | EA 官方 ALGS `algs.ea.com/en/news`；2026-09-22 受控列表/详情 200，Split 2 Pro League 文章可抽取标题和正文 | FETCH_VERIFIED | 影子运行、赛事票务/地区内容审核规则、摘要质量与自动发布门禁仍待完成 |
| valorant-official | valorant | Riot 官方 `playvalorant.com/en-us/news/game-updates/`；2026-09-20 实测 Python httpx 列表 200、6 条发现，详情 200，标题/正文/ISO 原始发布时间可提取 | FETCH_VERIFIED | 影子运行、中文摘要质量、来源设置、重试与重复评估；自动发布仍关闭 |
| valorant-esports-official | valorant | Riot 官方 `valorantesports.com/en-US/news/`；2026-09-22 受控列表/详情 200，赛事文章可抽取标题、正文与仅日期 `2026-09-16` | FETCH_VERIFIED | 影子运行、赛事地区/票务内容人工规则、摘要质量与自动发布门禁仍待完成 |
| delta-force-garena-official | delta-force | **入口已于 2026-10-16 换为三角洲行动国服官网** `df.qq.com/cp/a20240906main/newslist.html`（sourceCode 保留不变以免破坏 content_source 种子与发布门禁）；页面是 JS 壳，取数改走第一方 flow JSON `comm.ams.game.qq.com/ide/`，列表/详情实测均 `iRet=0`，可抽取标题、正文 HTML、原文日期、图片与 docid 原文链接。同日 `V12` 将该来源由 `FETCH_VERIFIED` 提升为 `SHADOW`（`auto_publish_enabled` 保持 0），`V13` 把显示名/base_url/allowed_domains/language 对齐国服实际入口 | SHADOW | 已于 2026-10-16 完成一次真实影子运行（6 条列表 → 5 篇入库，全部 `FULL_TEXT` + `PENDING_REVIEW`，无自动上线）；仍待完成：国服语言与地区限定标注、图片型公告的正文质量、摘要质量、人工标注质量门禁与自动发布 |
| fps-media-a | 多游戏 | 待选择明确单游戏专栏，不能直接沿用综合首页 | CANDIDATE | 相关率、可用性、更新频率、原文时间、重复率 |
| fps-media-b | 多游戏 | 待选择攻略/赛事补充来源 | CANDIDATE | 四游戏覆盖与已有来源重叠、内容使用条件 |
| xiaoheihe-community | delta-force、cs2、apex-legends、valorant（待逐游戏验证） | 用户建议的小黑盒话题入口 xiaoheihe.cn/app/topic/link；2026-09-22 匿名请求仅得到 SPA 页面壳（200），未验证公开文章列表/详情接口 | CANDIDATE | 明确公开、免登录且允许的栏目/详情接口；四游戏相关率；使用条件；展示固定标注“小黑盒社区”与原文链接；不得自动全文/图片搬运 |

现有 GamerSky `/news/` 和 GameRes `/list/18` 只作为旧源核查对象，不因爬虫已经存在就直接批准上线。游戏栏目级入口需重新验证。四游戏各至少一个合格官方或明确专属来源；不能对尚无来源的游戏声称覆盖完成。

国服与国际服、赛事资讯与即时比分要在来源配置中标明。默认接收与目标游戏直接相关的公告，不将无关地区活动误写成全服活动；摘要保留地区/平台限定。

## 3. 每个来源必须登记的证据

1. 来源名称、sourceCode、官方/媒体属性、精确入口与允许域名、游戏和语言范围。
2. 实测时间、状态码、重定向、访问规则、限流/缓存方式；不保存 Cookie、密钥或登录凭据。
3. 至少覆盖列表发现、正常详情、缺失字段/空列表、重复/更新四类解析样本。
4. title、canonical URL、原文发布时间、语言、正文抽取和署名/来源的准确性。
5. 元数据初筛与模型/规则评测结果、拒绝/审核例子、是否达到自动发布条件。
6. 周期、批量上限、超时、重试、来源级禁用开关、最后成功时间。
7. 内容展示条件和原文链接可用性；没有确认条件时不自动全文/图片搬运。
8. 样本保存位置、适配器与规则版本、验证人/日期、失效回退方式。

本表不登记服务器登录信息；来源凭据（如果未来需要）只通过部署密钥配置注入。
## 4. 受控 dry-run 预检（2026-09-22）

以 `MAX_ARTICLES_PER_RUN=1`、`CRAWL_DELAY_SECONDS=0` 分别运行六个来源的 `main.py --dry-run --source ...`。VALORANT 更新、Apex、Delta Force Garena、CS2、VALORANT Esports 和 ALGS 均至少解析到一条白名单相关候选；dry-run 没有创建 Redis、SQLite state、outbox、AI、OSS、去重或统计写入。CS2 API 修复后在实时预检中只提取一条第一方 Community Announcement，确认候选上限是在过滤第三方 feed 后生效。

这只是受控解析预检，不是 SHADOW：没有模型判定、没有写入内部 API、没有人工审核，也没有自动发布。



## 5. 小黑盒话题入口匿名可用性初检（2026-09-22）

用户建议将 `https://xiaoheihe.cn/app/topic/link` 作为三角洲、CS2、Apex 与 VALORANT 的补充资讯候选。以不携带 Cookie、不登录的受控单请求访问，该入口返回 HTTP 200 和“小黑盒 - 玩家高能聚集地”页面壳，未发现登录提示；但响应本身未包含可供解析的文章列表或四游戏筛选数据。

对一个公开分享详情旧路径的单次匿名探测返回“该功能正在维护中”，未取得文章标题、作者、发布时间、正文或 canonical 原文地址。因此当前只能登记为 `CANDIDATE`，不能建立爬虫适配器、不能写入来源表、不能进入影子运行，更不能自动发布。

若继续，必须先确认网站公开访问规则及内容展示许可，并以最小请求量确认一个稳定、免登录的列表入口和详情入口。内容展示默认只保存必要元数据、显著标注“文章来源：小黑盒社区”并提供原文链接；除非得到明确许可，不复制全文、图片或评论。

## 6. 三角洲行动国服官网 flow JSON 受控验证（2026-10-16）

用户要求把 `ingest/spiders/delta_force_garena.py` 的采集入口改为国服官网 `https://df.qq.com/cp/a20240906main/newslist.html`。受控排查结果：

1. 列表页与详情页均为 JS 壳：`<ul id="newslist">` 与 `<p id="sContent">` 在原始 HTML 中为空，内容由页面内联脚本 `Milo.emit(flow_1073310)`（列表）/`Milo.emit(flow_1073326)`（详情）异步拉取，普通 GET 得不到任何标题或正文。
2. 从 Milo 前端模块（`milo.min.js` 模块 171/9002）还原出请求契约：`POST https://comm.ams.game.qq.com/ide/`，表单字段 `iChartId/iSubChartId/sIdeToken`（chart id 为 IDE mapid，不是 flow id）+ `sMiloTag` + 业务参数（列表 `chanid/typeids/limit/start`，详情 `id`）。活动为 `actId=670608`、`sServiceType=dfm`，列表 chart 329849 / token `o3Iw53`，详情 chart 329870 / token `vDQriw`。
3. 匿名请求即可成功（不需要 Cookie 或登录）：列表响应 `{"ret":0,"iRet":0,"jData":{"data":{"total":530,"items":[...]}}}`，详情响应 `jData.data[0]` 含 `sTitle/sContent/sIdxTime/sAuthor/iDocID/sRedirectURL`。实测时间 2026-10-16 17:0x，未触发验证码或限流提示。
4. 离线夹具：`ingest/tests/fixtures/delta_force_qq_newslist_flow.json`（3 条）、`delta_force_qq_newsdetail_flow.json`（图片型公告）、`delta_force_qq_newsdetail_text_flow.json`（文本型公告）。适配器测试 6 项通过；`python -m unittest discover -s tests` 全量 109 项通过、0 失败。
5. 受控 dry-run（`MAX_ARTICLES_PER_RUN=9`、`CRAWL_DELAY_SECONDS=0`、`main.py --dry-run --source delta-force-garena-official`）：列表 9 条全部解析成功，2 条进入候选（1 条 `ACCEPT delta-force/patch_notes`、1 条 `REVIEW`），其余 7 条被规则过滤；dry-run 未写 Redis/SQLite/outbox/AI/OSS。

6. **列表栏目收敛为「新闻」（2026-10-16 当日调整）**：页面 tab 顺序来自 `index.js` 的 `NewsArr`（1=最新 6895、2=公告 6896、3=新闻 6898、4=赛事 7037）。先前用 6895「最新」会把违规处罚公告等纯公告混进资讯流。现固定 `NEWS_CHANNEL_ID=6898`，实测 `total=155`，分页正常，每条记录的 `sChannelInfo` 都含 `6898|新闻`。

   「新闻」栏目 27 条抽样（`start=0/9/18`，逐条受控请求详情）规则判定：**ACCEPT 13 / REVIEW 5 / REJECT 9**。9 条拒绝中 4 条是其它游戏内容（《王者万象棋》、主播巅峰赛）被 `GAME_NOT_ALLOWED` 正确拦下，5 条是官方活动/社团招募因标题不含游戏名被拦。ACCEPT 覆盖 `patch_notes`、`esports`、`event` 三类，内容分布明显优于「最新」栏目（后者首屏 9 条仅 1 条 ACCEPT）。

7. **栏目策略已定稿（2026-10-16，产品确认）**：锁定「新闻」`6898` 单一栏目，不采集「最新」/「公告」/「赛事」，因此**不收录补丁说明与版本更新公告**（该类内容主要挂在「公告」6896）；赛事结果类重复模板（`恭喜XXX成功晋级总决赛！` 连续多条）**本轮不处理，归属去重策略**，不在来源适配器或规则层解决。

**已知质量事实（未放宽任何规则）**：国服官网公告大量为**整篇长图**，正文 HTML 只有 `<img>`，规则初筛按 `GAME_NOT_ALLOWED` 拒绝（标题通常只写“违规行为处罚公告”“X月X日更新公告”，不含“三角洲行动”）。该问题主要出现在「最新」/「公告」栏目，收敛到「新闻」栏目后已明显缓解（27 条抽样 13 条 ACCEPT）。仍待产品确认：是否需要为国服官方来源补充“来源级游戏归属”证据，以覆盖官方活动/社团招募标题不含游戏名的情况；本轮未改动 `fps_rules.py`。

**「新闻」栏目的内容质量风险**：抽样中 `esports` 占比偏高，且存在多条重复模板（`恭喜XXX成功晋级总决赛！` 连续 6 条）。产品已确认**本轮不处理，属去重策略范畴**，不在来源适配器或 `fps_rules.py` 内解决；来源层不再调整。

**额外允许来源**：取数接口在 `comm.ams.game.qq.com`，与页面 `df.qq.com` 不同源。`BaseSpider` 新增 `API_ORIGINS` 显式白名单（默认空集、仅允许 https、同样做 DNS 公有地址校验），该来源显式登记 `https://comm.ams.game.qq.com/`，未引入 headless browser。
