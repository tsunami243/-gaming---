# Isolated Compose preparation only

This directory is preparation work only. It has not been built, started, installed, or applied to the target server. Do not run Docker on the server until the user separately approves B06 and the deployment window.

- `compose.yaml` uses named GameBoi-only volumes and an internal network; it does not attach to existing project networks.
- Only `frontend` publishes a host port. MySQL, Redis and backend remain internal.
- `ingest` is an explicit Compose profile and is not a daemon; schedule it only after source quality and capacity gates are approved.
- The memory caps now total exactly 3,150,000,000 bytes, matching PROJECT_DESIGN section 10.1, leaving 850,000,000 bytes for measured runtime/management/transient overhead. This is a configuration budget, not proof of the project-wide 4 GB target. Earlier 3.776 GB wording incorrectly treated m-suffixed values as decimal.
- Set an actual project-owned disk quota or equivalent before claiming the 10 GB storage cap; the Python capacity gate alone is not a filesystem quota.

Deployment config validation is now available from the project root without starting containers:

```powershell
.\scripts\check-deploy.ps1
```

It validates the isolated Compose topology and refuses unpinned `latest` tags, but it does not build images, start Docker, or constitute deployment approval.
