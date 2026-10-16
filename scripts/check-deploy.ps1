[CmdletBinding()]
param([string]$ComposeFile = "$PSScriptRoot\..\deploy\compose.yaml")
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$compose = (Resolve-Path $ComposeFile).Path
if (-not (Test-Path (Join-Path $root 'deploy\.env.example'))) { throw 'Missing deploy/.env.example' }
if (Select-String -Path $compose -Pattern 'image:\s*[^\s]+:latest' -Quiet) { throw 'Deployment images must not use latest tags.' }
if (-not (Select-String -Path $compose -Pattern 'internal:\s*true' -Quiet)) { throw 'Compose network must remain internal.' }
if (-not (Select-String -Path $compose -Pattern 'mem_limit:' -Quiet)) { throw 'Compose services need explicit memory limits.' }
& python (Join-Path $PSScriptRoot 'check-resource-budget.py') $compose
if ($LASTEXITCODE -ne 0) { throw 'Configured resource budget failed validation.' }
& docker compose -f $compose config --no-interpolate | Out-Null
if ($LASTEXITCODE -ne 0) { throw "docker compose config failed: $LASTEXITCODE" }
Write-Host 'PASS: isolated Compose configuration validated without building or starting containers.' -ForegroundColor Green
