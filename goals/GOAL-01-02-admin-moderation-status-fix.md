# 修复方案与实施记录：管理后台状态筛选与来源暂停判断（GOAL-01 + GOAL-02）

- 覆盖目标：`goal.md` GOAL-01、GOAL-02
- 状态：**done（已实施并验证）** — 2026-10-13
- 类型：bug（非法枚举值）· 严重级：P0
- 改动文件：`gaming-news/src/utils/constants.js`、`gaming-news/src/views/AdminView.vue`
- 验证结果：`npm test` 43/43 通过；`npm run build` 成功

---

## 1. 问题与根因

前端把后端枚举值写成了中文+拉丁混拼的非法字符串，导致界面行为异常：

| 位置 | 修复前（非法） | 修复后 | 原后果 |
|---|---|---|---|
| `AdminView.vue:6` 状态下拉 | `拒绝ED` | `REJECTED` | 选"已拒绝"时请求 `workflowStatus=拒绝ED`，列表恒为空 |
| `AdminView.vue:6` 状态下拉 | `下架ED` | `UNPUBLISHED` | 同上 |
| `AdminView.vue:23` 来源按钮 | `暂停D` | `PAUSED` | 已暂停来源仍显示 PAUSE，永远出不来 RESUME |

后端真值来源：
- `src/main/java/org/example/bigevent/api/v1/admin/AdminModerationService.java:21`
  `LISTABLE = Set.of("PENDING_REVIEW","PUBLISHED","REJECTED","UNPUBLISHED")`
- `src/main/java/org/example/bigevent/api/v1/admin/AdminOperationsMapper.java:14,17`
  暂停写入 `verification_status='PAUSED'`

经全前端 grep 确认，坏值只存在于 `AdminView.vue` 第 6、23 行，且无测试、无其他文件引用 → 改动面最小。

---

## 2. 实施方案（修复 + 常量收敛）

为从根上避免"中文混拼"再次出现，把这两组后端枚举收敛为冻结常量，统一引用。

### 2.1 `gaming-news/src/utils/constants.js`（追加）

```js
export const ARTICLE_WORKFLOW_STATUS = Object.freeze({
  PENDING_REVIEW: 'PENDING_REVIEW',
  PUBLISHED: 'PUBLISHED',
  REJECTED: 'REJECTED',
  UNPUBLISHED: 'UNPUBLISHED',
})

export const SOURCE_VERIFICATION_STATUS = Object.freeze({
  CANDIDATE: 'CANDIDATE',
  SHADOW: 'SHADOW',
  FETCH_VERIFIED: 'FETCH_VERIFIED',
  PAUSED: 'PAUSED',
})
```

### 2.2 `gaming-news/src/views/AdminView.vue`

- `import { ARTICLE_WORKFLOW_STATUS, SOURCE_VERIFICATION_STATUS } from '../utils/constants'`
- 第 6 行：四个 `<option>` 的 `value` 全部改用 `ARTICLE_WORKFLOW_STATUS.*`（含原本就正确的 `PENDING_REVIEW`、`PUBLISHED`，统一为常量）；中文展示文案保持不变
- 第 12 行：`item.workflowStatus===ARTICLE_WORKFLOW_STATUS.PENDING_REVIEW`
- 第 13 行：`item.workflowStatus===ARTICLE_WORKFLOW_STATUS.PUBLISHED`
- 第 23 行：`source.verificationStatus===SOURCE_VERIFICATION_STATUS.PAUSED`
- 初始值：`const status=ref(ARTICLE_WORKFLOW_STATUS.PENDING_REVIEW)`

> 说明：第 12/13 行与初始值原本合法，按"常量收敛"原则一并换成常量，保持同文件内两组枚举引用一致；未触碰其他逻辑。

---

## 3. 验证证据

```powershell
cd gaming-news
npm test        # Test Files 12 passed (12) · Tests 43 passed (43)，含 AdminView 4 用例不回归
npm run build   # ✓ built in 4.02s
```

> 说明：未新增测试用例（按用户选择，本次只做修复 + 常量收敛）。`>500 kB` chunk 警告为既有问题（GOAL-13），与本次无关。

---

## 4. 验收核对

- [x] 选"已拒绝"→ 发出 `workflowStatus=REJECTED`；选"已下架"→ `workflowStatus=UNPUBLISHED`（由常量保证）
- [x] 已暂停来源显示 `RESUME`，未暂停显示 `PAUSE`
- [x] 原有 4 条 AdminView 用例 + 全部 43 条前端用例通过，无回归
- [x] `npm run build` 成功
- [x] 未改动后端、路由、授权与审核策略

---

## 5. 范围外 / 后续（不在本次改动）

- **GOAL-16（新发现，未实施）**：`AdminOperationsMapper.java:17` 恢复来源时无条件 `verification_status='SHADOW'`，而 `IngestSourcePolicyService.java:8` 的 `RUNNABLE={SHADOW, AUTO_ENABLED}` 不含 `FETCH_VERIFIED` → `FETCH_VERIFIED` 来源暂停再恢复会被静默降级为 `SHADOW` 并变为可运行。需先确认语义（刻意设计 vs 状态漂移 bug）再处理。
- **GOAL-02b（未实施）**：`AdminView.vue:68` 的 `window.prompt('暂停原因')` 可改为页内输入，属 UX 改进。
- 如需，可为两组枚举补 vitest 回归用例，防止字面量被再次改坏（本次按用户选择未加）。
