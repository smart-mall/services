#Requires -Version 5.1
<#
.SYNOPSIS
    Build every gulimall microservice image. Image names are all prefixed with "gl-".

.DESCRIPTION
    Every service Dockerfile expects the repository root as build context (it has to build the
    shared "common" module first), so this script wires up -f <module>/Dockerfile plus the
    correct context for you.

    NOTE: this file is intentionally ASCII-only. Windows PowerShell 5.1 decodes BOM-less UTF-8
    scripts as ANSI, which corrupts non-ASCII characters, so keep it ASCII.

.PARAMETER Tag
    Image tag, default "latest". Example: -Tag 0.0.1-SNAPSHOT

.PARAMETER Registry
    Optional registry prefix. When set, an extra fully qualified tag is applied.
    Example: -Registry registry.cn-hangzhou.aliyuncs.com/myns

.PARAMETER Only
    Build only the listed images. Example: -Only gl-product,gl-cart

.EXAMPLE
    .\build-images.ps1

.EXAMPLE
    .\build-images.ps1 -Tag 0.0.1-SNAPSHOT -Registry registry.cn-hangzhou.aliyuncs.com/myns

.EXAMPLE
    .\build-images.ps1 -Only gl-product
#>
[CmdletBinding()]
param(
    [string]$Tag = "latest",
    [string]$Registry = "",
    [string[]]$Only = @()
)

$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($root)) { $root = (Get-Location).Path }

# Directory name == module name == Dockerfile location.
# "common" is a library and gets no image of its own: each service installs it during its build stage.
$modules = @(
    'gateway',
    'product',
    'coupon',
    'member',
    'ware',
    'order',
    'search',
    'third-party',
    'auth',
    'cart',
    'seckill',
    'renren-fast'
)

Write-Host "Build context : $root" -ForegroundColor DarkGray
Write-Host "Image tag     : $Tag" -ForegroundColor DarkGray
if ($Registry) { Write-Host "Registry      : $Registry" -ForegroundColor DarkGray }
Write-Host ""

$built = @()
$failed = @()

foreach ($module in $modules) {

    $image = "gl-$module"
    if ($Only.Count -gt 0 -and $Only -notcontains $image) { continue }

    $dockerfile = Join-Path $root "$module\Dockerfile"
    if (-not (Test-Path $dockerfile)) {
        Write-Warning "skip $image : $dockerfile not found"
        continue
    }

    $tags = @("${image}:$Tag")
    if ($Registry) { $tags += "$Registry/${image}:$Tag" }

    $tagArgs = @()
    foreach ($t in $tags) { $tagArgs += @('-t', $t) }

    Write-Host "==> building $image" -ForegroundColor Cyan
    $startedAt = Get-Date

    & docker build -f $dockerfile @tagArgs $root
    $code = $LASTEXITCODE

    if ($code -ne 0) {
        $failed += $image
        Write-Host "!! $image FAILED (exit code: $code)" -ForegroundColor Red
    }
    else {
        $built += $image
        $cost = [int]((Get-Date) - $startedAt).TotalSeconds
        Write-Host "OK $image done in ${cost}s" -ForegroundColor Green
    }
    Write-Host ""
}

Write-Host "==================== SUMMARY ====================" -ForegroundColor Yellow
Write-Host ("succeeded ({0}): {1}" -f $built.Count, ($built -join ', '))
if ($failed.Count -gt 0) {
    Write-Host ("failed ({0}): {1}" -f $failed.Count, ($failed -join ', ')) -ForegroundColor Red
    exit 1
}
Write-Host ""
& docker images --filter "reference=gl-*"
