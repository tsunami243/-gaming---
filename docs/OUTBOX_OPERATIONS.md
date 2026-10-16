# Outbox 只读排查

在项目根 PowerShell 指定明确的本项目队列文件：

```powershell
python .\ingest\runtime\inspect_outbox.py "C:\明确的项目目录\outbox.sqlite3" --limit 20
```

命令以SQLite只读模式打开已有数据库。文件不存在时失败，不自动创建空库、不修改schema、不确认事件、不恢复采集。返回各状态数量、全部未解决数量、当前暂停原因/等待时间，以及最多100条事件ID/状态/尝试次数/创建时间；不打印正文、链接、token或任意旧错误详情。

- QUARANTINED：坏请求/载荷冲突待核查，不自动重试，也不自动删除。
- HTTP401/403暂停：检查独立服务凭据与服务配置后，由后续受控恢复入口处理；更换token不会自行解除。
- HTTP429/503暂停：持久等待期结束前不应强制提前重放。
- 对已人工核查的 QUARANTINED 单事件，可使用显式确认命令：

```powershell
python .\ingest\runtime\requeue_outbox.py "C:\明确的项目目录\outbox.sqlite3" --event-id "原eventId" --reason "人工核查后的恢复原因" --confirm
```

该命令只允许 QUARANTINED 状态、必须填写 1–200 字原因，并写入本地 `outbox_operator_audit`；它保留原 eventId、将 attempts 重置为 0，不清除 401/403/429/503 的全局 delivery pause，也不删除、确认或批量重放其他事件。READY、ACKED、缺失事件都会失败。不要通过删除 SQLite 控制表、重建 outbox 或换新 eventId 绕过暂停和幂等约束。

只读状态排查不代表已完成运营告警渠道、审核恢复权限或自动清理策略。

