#Requires -Version 5.1
<#
.SYNOPSIS
    Build the gulimall microservice images (gl-*), skipping services that are already
    built from unchanged sources.

.DESCRIPTION
    Every service Dockerfile expects the repository root as build context (it has to build the
    shared "common" module first), so this script wires up -f <module>/Dockerfile plus the
    correct context for you.

    Before building, each service gets a SHA256 fingerprint over everything that can affect its
    image: its Dockerfile, its pom.xml, its src/**, plus common/pom.xml, common/src/**,
    .mvn/settings.xml and .dockerignore. If the image already exists AND the fingerprint is
    unchanged since the last successful build, the build is skipped entirely.

    The fingerprints live in .build-images.state.json (local state, safe to delete; deleting it
    just means the next run rebuilds everything once). Use -Force to ignore it.

    NOTE: this file is intentionally ASCII-only. Windows PowerShell 5.1 decodes BOM-less UTF-8
    scripts as ANSI, which corrupts non-ASCII characters, so keep it ASCII.

.PARAMETER Tag
    Image tag, default "latest". Example: -Tag 0.0.1-SNAPSHOT

.PARAMETER Registry
    Optional registry prefix. When set, an extra fully qualified tag is applied.
    Example: -Registry registry.cn-hangzhou.aliyuncs.com/myns

.PARAMETER Only
    Build only the listed images. Example: -Only gl-product,gl-cart

.PARAMETER Force
    Rebuild everything, ignoring image existence and stored fingerprints.

.EXAMPLE
    .\build-images.ps1

.EXAMPLE
    .\build-images.ps1 -Force

.EXAMPLE
    .\build-images.ps1 -Only gl-product -Tag 0.0.1-SNAPSHOT
#>
[CmdletBinding()]
param(
    [string]$Tag = "latest",
    [string]$Registry = "",
    [string[]]$Only = @(),
    [switch]$Force
)

$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($root)) { $root = (Get-Location).Path }

# Directory name == module name == Dockerfile location.
# "common" is a library and gets no image of its own: every service installs it during its build stage.
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

$stateFile = Join-Path $root ".build-images.state.json"

# ---------------------------------------------------------------------------
# fingerprint: SHA256 over every file that can change the resulting image
# ---------------------------------------------------------------------------
function Get-ServiceFingerprint {
    param(
        [string]   $Root,
        [string]   $Module,
        [string[]] $ExtraPaths
    )

    $inputs = @(
        (Join-Path $Root ".dockerignore")
        (Join-Path $Root ".mvn\settings.xml")
        (Join-Path $Root "$Module\Dockerfile")
        (Join-Path $Root "$Module\pom.xml")
        (Join-Path $Root "$Module\src")
    ) + $ExtraPaths

    $files = @()
    foreach ($p in $inputs) {
        if (Test-Path -LiteralPath $p -PathType Container) {
            $files += Get-ChildItem -LiteralPath $p -Recurse -File -ErrorAction SilentlyContinue
        }
        elseif (Test-Path -LiteralPath $p -PathType Leaf) {
            $files += Get-Item -LiteralPath $p
        }
    }

    $manifest = foreach ($f in ($files | Sort-Object -Property FullName)) {
        $rel = $f.FullName.Substring($Root.Length).TrimStart('\', '/')
        $h = (Get-FileHash -LiteralPath $f.FullName -Algorithm SHA256).Hash
        "$rel|$h"
    }

    # hash the manifest through a temp file so only cmdlets are used
    $tmp = Join-Path $env:TEMP ("gl-fingerprint-" + $Module + ".txt")
    Set-Content -LiteralPath $tmp -Value ($manifest -join "`n") -Encoding ASCII
    try {
        return (Get-FileHash -LiteralPath $tmp -Algorithm SHA256).Hash
    }
    finally {
        Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue
    }
}

function Save-State {
    param([hashtable]$Map, [string]$Path)
    $ordered = [ordered]@{}
    foreach ($k in ($Map.Keys | Sort-Object)) { $ordered[$k] = $Map[$k] }
    ($ordered | ConvertTo-Json) | Set-Content -LiteralPath $Path -Encoding ASCII
}

# ---------------------------------------------------------------------------
# load previous state + list of images that already exist
# ---------------------------------------------------------------------------
$fingerprints = @{}
if (-not $Force -and (Test-Path -LiteralPath $stateFile)) {
    try {
        $obj = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
        foreach ($p in $obj.PSObject.Properties) { $fingerprints[$p.Name] = [string]$p.Value }
    }
    catch {
        Write-Warning "state file could not be read, everything will be rebuilt"
        $fingerprints = @{}
    }
}

$existingImages = @{}
if (-not $Force) {
    foreach ($line in (& docker images --format "{{.Repository}}:{{.Tag}}" 2>$null)) {
        $t = "$line".Trim()
        if ($t) { $existingImages[$t] = $true }
    }
}

Write-Host "Build context  : $root" -ForegroundColor DarkGray
Write-Host "Image tag      : $Tag" -ForegroundColor DarkGray
if ($Registry) { Write-Host "Registry       : $Registry" -ForegroundColor DarkGray }
if ($Force) {
    Write-Host "Mode           : FORCE (skip logic disabled)" -ForegroundColor Yellow
}
else {
    Write-Host ("Existing images: {0} | build records: {1}" -f $existingImages.Count, $fingerprints.Count) -ForegroundColor DarkGray
}
Write-Host ""

# ---------------------------------------------------------------------------
# build loop
# ---------------------------------------------------------------------------
$built = @()
$skipped = @()
$failed = @()
$startedAll = Get-Date

foreach ($module in $modules) {

    $image = "gl-$module"
    if ($Only.Count -gt 0 -and $Only -notcontains $image) { continue }

    $dockerfile = Join-Path $root "$module\Dockerfile"
    if (-not (Test-Path -LiteralPath $dockerfile)) {
        Write-Warning "skip $image : $dockerfile not found"
        continue
    }

    $fullTag = "${image}:$Tag"
    $tags = @($fullTag)
    if ($Registry) { $tags += "$Registry/$fullTag" }

    # common is a real dependency of every service except renren-fast
    $extra = @()
    if ($module -ne 'renren-fast') {
        $extra = @(
            (Join-Path $root "common\pom.xml")
            (Join-Path $root "common\src")
        )
    }

    $fingerprint = Get-ServiceFingerprint -Root $root -Module $module -ExtraPaths $extra

    $reason = $null
    if ($Force)                                        { $reason = "forced" }
    elseif (-not $existingImages.ContainsKey($fullTag)) { $reason = "image not built yet" }
    elseif (-not $fingerprints.ContainsKey($fullTag))   { $reason = "no build record" }
    elseif ($fingerprints[$fullTag] -ne $fingerprint)   { $reason = "sources changed" }

    if (-not $reason) {
        Write-Host "SKIP $fullTag  (already built, sources unchanged)" -ForegroundColor DarkGray
        $skipped += $fullTag
        continue
    }

    $tagArgs = @()
    foreach ($t in $tags) { $tagArgs += @('-t', $t) }

    Write-Host "==> building $fullTag  [$reason]" -ForegroundColor Cyan
    $startedAt = Get-Date

    & docker build -f $dockerfile @tagArgs $root
    $code = $LASTEXITCODE

    if ($code -ne 0) {
        $failed += $fullTag
        Write-Host "!! $fullTag FAILED (exit code: $code)" -ForegroundColor Red
    }
    else {
        $built += $fullTag
        $cost = [int]((Get-Date) - $startedAt).TotalSeconds
        Write-Host "OK $fullTag done in ${cost}s" -ForegroundColor Green

        # remember it immediately so an interrupted run keeps its progress
        $fingerprints[$fullTag] = $fingerprint
        $existingImages[$fullTag] = $true
        Save-State -Map $fingerprints -Path $stateFile
    }
    Write-Host ""
}

# ---------------------------------------------------------------------------
# summary
# ---------------------------------------------------------------------------
$totalCost = [int]((Get-Date) - $startedAll).TotalSeconds

Write-Host "==================== SUMMARY ====================" -ForegroundColor Yellow
Write-Host ("skipped ({0}): {1}" -f $skipped.Count, $(if ($skipped.Count) { $skipped -join ', ' } else { '-' })) -ForegroundColor DarkGray
Write-Host ("built   ({0}): {1}" -f $built.Count, $(if ($built.Count) { $built -join ', ' } else { '-' }))
if ($failed.Count -gt 0) {
    Write-Host ("failed  ({0}): {1}" -f $failed.Count, ($failed -join ', ')) -ForegroundColor Red
}
Write-Host "total time: ${totalCost}s"
Write-Host ""

if ($failed.Count -gt 0) { exit 1 }

& docker images --filter "reference=gl-*"
