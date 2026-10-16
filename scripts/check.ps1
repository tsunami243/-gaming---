[CmdletBinding()]
param(
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [switch]$SkipPython,
    [switch]$Browser
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$failures = [System.Collections.Generic.List[string]]::new()

function Invoke-CheckStep {
    param(
        [string]$Name,
        [scriptblock]$Command
    )

    Write-Host "`n== $Name ==" -ForegroundColor Cyan
    try {
        & $Command
        if ($LASTEXITCODE -ne 0) {
            throw "Exit code $LASTEXITCODE"
        }
        Write-Host "PASS: $Name" -ForegroundColor Green
    } catch {
        $failures.Add("${Name}: $($_.Exception.Message)")
        Write-Host "FAIL: $Name — $($_.Exception.Message)" -ForegroundColor Red
    }
}

Push-Location $projectRoot
try {
    Invoke-CheckStep 'Configured resource budget' { python .\scripts\check-resource-budget.py }
    Invoke-CheckStep 'Static build-context safety' { python .\scripts\check-build-context.py }
    Invoke-CheckStep 'OpenAPI contract' { python .\scripts\validate_openapi.py }

    if (-not $SkipBackend) {
        Invoke-CheckStep 'Backend tests' { & .\mvnw.cmd test }
    }

    if (-not $SkipFrontend) {
        Invoke-CheckStep 'Frontend production build' {
            Push-Location .\gaming-news
            try { npm run build } finally { Pop-Location }
        }
        Invoke-CheckStep 'Frontend HTTP proxy integration' {
            node .\gaming-news\scripts\check-proxy.mjs
        }
        Invoke-CheckStep 'Frontend tests' {
            Push-Location .\gaming-news
            try { npm test } finally { Pop-Location }
        }
    }

    if ($Browser) {
        if ($SkipFrontend) { throw '-Browser requires the frontend build; do not combine with -SkipFrontend.' }
        Invoke-CheckStep 'Chromium desktop/mobile UI flows (fixture API)' {
            python .\scripts\check-web-browser.py
        }
    }

    if (-not $SkipPython) {
        Invoke-CheckStep 'Python compile and import smoke' {
            Push-Location .\ingest
            try {
                python -m compileall -q .
                if ($LASTEXITCODE -ne 0) { throw "Python compilation failed: $LASTEXITCODE" }
                python -m unittest discover -s tests -t . -v
                if ($LASTEXITCODE -ne 0) { throw "Python tests failed: $LASTEXITCODE" }
                python -c "import main; import agent.deepseek_client; import spiders.gamersky; import spiders.gameres; print('Python import smoke: PASS')"
            } finally { Pop-Location }
        }
    }
} finally {
    Pop-Location
}

if ($failures.Count -gt 0) {
    Write-Host "`nBaseline checks failed:" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    exit 1
}

Write-Host "`nAll selected baseline checks passed." -ForegroundColor Green
