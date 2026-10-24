# =====================================================================
# FuLu - build helper
# =====================================================================
#
# Why not gradlew.bat:
#   JAVA_HOME and the JDK path on this machine contain non-ASCII characters
#   (D:\<cjk>\...). gradlew.bat / gradle.bat are cmd batch files and cannot
#   handle such paths - they fail with "JAVA_HOME is set to an invalid directory".
#   Gradle's native libs also unpack to ~/.gradle/native by default, which can
#   fail in a confined environment.
#
# So this script:
#   1. launches Gradle directly via
#      java -cp gradle-launcher.jar org.gradle.launcher.GradleMain  (no batch file)
#   2. points GRADLE_USER_HOME at tools\.gradle-home inside the project
#   3. passes proxy JVM args when a proxy is actually reachable
#   4. falls back to --offline when there is no network, and classifies a
#      failure as "compile error" vs "missing cached dependency" - the two
#      need completely different follow-up actions
#
# NOTE: this file is intentionally pure ASCII.
#   Windows PowerShell 5.1 reads .ps1 files as ANSI unless they carry a UTF-8
#   BOM, so non-ASCII characters in this script would be mangled into parse
#   errors. All Chinese documentation lives in README.md instead.
#
# Usage:
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\build.ps1
#   powershell ... -File tools\build.ps1 -Task build
#   powershell ... -File tools\build.ps1 -Task runClient
#   powershell ... -File tools\build.ps1 -Clean
#
#   # force offline (auto-detected when the proxy probe fails)
#   powershell ... -File tools\build.ps1 -Offline
#
#   # force online after opening the accelerator, to fetch missing deps
#   powershell ... -File tools\build.ps1 -Online
# =====================================================================

[CmdletBinding()]
param(
    [string]$Task = "build",
    [switch]$Clean,
    # Empty means: read tools\java-home.txt (UTF-8, may contain a CJK path)
    [string]$JavaHome = "",
    [string]$Proxy = "127.0.0.1:7890",
    [string]$GradleDist = "",
    [switch]$NoProxy,
    [switch]$Offline,
    [switch]$Online
)

# Deliberately NOT setting $ErrorActionPreference = "Stop":
# Java / Gradle write a lot of normal logging to stderr and PowerShell would
# treat that as a fatal error, aborting the build right at the start.
$ErrorActionPreference = "Continue"

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

# ---- resolve JDK ----
if (-not $JavaHome) {
    $jdkFile = Join-Path $root "tools\java-home.txt"
    if (Test-Path $jdkFile) {
        $JavaHome = (Get-Content $jdkFile -Encoding UTF8 |
                     Where-Object { $_.Trim() -ne "" -and -not $_.Trim().StartsWith("#") } |
                     Select-Object -First 1).Trim()
    }
}
if (-not $JavaHome) { $JavaHome = "D:\Program Files\Java\jdk-17" }
$java = Join-Path $JavaHome "bin\java.exe"
if (-not (Test-Path $java)) { throw "java.exe not found: $java  (pass -JavaHome or fix tools\java-home.txt)" }

# ---- resolve Gradle ----
if (-not $GradleDist) {
    $cand = Get-ChildItem (Join-Path $root "tools\.toolchain") -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -like "gradle-*" } | Select-Object -First 1
    if (-not $cand) { throw "Gradle distribution not found. Run tools\setup_toolchain.ps1 first." }
    $GradleDist = $cand.FullName
}
$launcher = Get-ChildItem (Join-Path $GradleDist "lib") -Filter "gradle-launcher-*.jar" -ErrorAction SilentlyContinue |
            Select-Object -First 1 -ExpandProperty FullName
if (-not $launcher) { throw "gradle-launcher jar not found in $GradleDist" }

# ---- keep the Gradle home inside the project ----
$env:GRADLE_USER_HOME = Join-Path $root "tools\.gradle-home"
New-Item -ItemType Directory -Force -Path $env:GRADLE_USER_HOME | Out-Null

# ---- proxy probe ----
# Do not blindly route through the proxy: when the accelerator is off, forcing
# 127.0.0.1:7890 produces "Connection refused" plus a 200 line stack trace that
# buries the real cause. Probe with a real HTTP request instead of just checking
# whether the port is listening (that proved unreliable in practice).
function Test-ProxyUsable([string]$spec) {
    if (-not $spec) { return $false }
    $parts = $spec.Split(":")
    if ($parts.Count -lt 2) { return $false }
    try {
        $req = [System.Net.HttpWebRequest]::Create("https://services.gradle.org/distributions/")
        $req.Proxy = New-Object System.Net.WebProxy("http://$($parts[0]):$($parts[1])")
        $req.Method = "HEAD"
        $req.Timeout = 15000
        $req.AllowAutoRedirect = $true
        $resp = $req.GetResponse()
        $code = [int]$resp.StatusCode
        $resp.Close()
        return ($code -ge 200 -and $code -lt 500)
    } catch {
        return $false
    }
}

$proxyUsable = $false
if (-not $NoProxy -and $Proxy) {
    Write-Host "  probing proxy $Proxy ..." -ForegroundColor Gray
    $proxyUsable = Test-ProxyUsable $Proxy
    if (-not $proxyUsable) {
        Write-Host "  [note] proxy $Proxy is not usable (accelerator off?)" -ForegroundColor Yellow
        Write-Host "         going direct for this run." -ForegroundColor Yellow
    } else {
        Write-Host "  proxy is usable; builds will go through it." -ForegroundColor Gray
    }
}

# ---- JVM args ----
$jvm = @(
    "-Xmx3g",
    "-Dfile.encoding=UTF-8",
    "-Dorg.gradle.internal.http.connectionTimeout=120000",
    "-Dorg.gradle.internal.http.socketTimeout=120000"
)
if ($proxyUsable) {
    $parts = $Proxy.Split(":")
    $jvm += "-Dhttp.proxyHost=$($parts[0])"
    $jvm += "-Dhttp.proxyPort=$($parts[1])"
    $jvm += "-Dhttps.proxyHost=$($parts[0])"
    $jvm += "-Dhttps.proxyPort=$($parts[1])"
}

$tasks = @()
if ($Clean) { $tasks += "clean" }
$tasks += $Task

# ---- stale resource guard ----
# Gradle's processResources is incremental: when a file is deleted from the
# source tree it is NOT removed from build/resources/main, so it keeps getting
# packed into the jar. Compare the two trees and delete stale copies first.
$srcRes = Join-Path $root "src\main\resources"
$outRes = Join-Path $root "build\resources\main"
if ((Test-Path $srcRes) -and (Test-Path $outRes)) {
    $stale = New-Object System.Collections.ArrayList
    Get-ChildItem $outRes -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object {
        $rel = $_.FullName.Substring($outRes.Length).TrimStart('\')
        if (-not (Test-Path (Join-Path $srcRes $rel))) { [void]$stale.Add($rel) }
    }
    if ($stale.Count -gt 0) {
        Write-Host " [stale] $($stale.Count) resource(s) deleted from source, removing from build output:" -ForegroundColor Yellow
        foreach ($rel in $stale) {
            Write-Host "         $rel" -ForegroundColor DarkYellow
            Remove-Item -LiteralPath (Join-Path $outRes $rel) -Force -ErrorAction SilentlyContinue
        }
    }
}

# ---- offline decision ----
# With no reachable network, every dependency resolution would hit
# "Connection refused" and the resulting wall of network stack traces buries the
# real compile errors. So: no proxy => offline, which makes failures meaningful.
$useOffline = $false
if ($Offline) {
    $useOffline = $true
} elseif ($Online) {
    $useOffline = $false
} elseif (-not $proxyUsable) {
    $useOffline = $true
}
if ($useOffline) { $tasks += "--offline" }

Write-Host ("=" * 68)
Write-Host " FuLu - build"
Write-Host ("=" * 68)
Write-Host " java        : $java"
Write-Host " gradle      : $GradleDist"
Write-Host " gradle home : $env:GRADLE_USER_HOME"
Write-Host " proxy       : $(if ($proxyUsable) { "$Proxy (probed OK)" } else { 'none usable' })"
Write-Host " deps        : $(if ($useOffline) { 'offline (tools\.gradle-home cache only)' } else { 'online' })"
Write-Host " tasks       : $($tasks -join ' ')"
Write-Host ("=" * 68)
Write-Host ""

Push-Location $root
try {
    $output = & $java @jvm -cp $launcher org.gradle.launcher.GradleMain @tasks --no-daemon --no-watch-fs --console=plain --stacktrace 2>&1
    $code = $LASTEXITCODE
} finally {
    Pop-Location
}

# Collect output first (see the offline note above) so we can classify the failure.
#
# NOTE: this block must stay pure ASCII. This machine's JDK prints diagnostics in
# Chinese, so the obvious fix - adding Chinese regex patterns here - breaks the
# script: Windows PowerShell 5.1 reads .ps1 files as ANSI unless they carry a
# UTF-8 BOM, so the Chinese bytes get mangled into unterminated strings and the
# whole script fails to parse. (That is exactly what happened once.)
# The robust classifier is structural, not lexical: javac always prints
# "<file>.java:<line>: <message>" regardless of locale, so match that shape.
$javacErrors = @()
$seenLocations = @{}
foreach ($line in $output) {
    $text = $line.ToString()
    Write-Host $text

    $isError = $false
    if ($text -match '\.java:\d+:') {
        $isError = $true
    }
    # Locale-independent English fallbacks, in case the location line is wrapped.
    elseif ($text -match 'error:|cannot find symbol|incompatible types|no suitable method') {
        $isError = $true
    }

    if ($isError) {
        # Deduplicate: javac prints each error twice (once inline, once in the
        # summary), so without this the operator sees every error listed two or
        # three times and it reads as if there are more problems than there are.
        $key = $text.Trim()
        if (-not $seenLocations.ContainsKey($key)) {
            $seenLocations[$key] = $true
            $javacErrors += $text
        }
    }
}

$cacheMiss = $false
if ($code -ne 0 -and $useOffline) {
    $cacheMiss = ($output | Where-Object {
        $_ -match 'No cached version|not found in offline mode|offline mode'
    }).Count -gt 0
}

Write-Host ""
Write-Host ("=" * 68)
if ($code -eq 0) {
    Write-Host " BUILD OK (exit 0)" -ForegroundColor Green
    $libs = Join-Path $root "build\libs"
    if (Test-Path $libs) {
        Write-Host ""
        Write-Host " artifacts:"
        Get-ChildItem $libs -Filter *.jar | ForEach-Object {
            Write-Host ("   {0}   {1:N0} bytes" -f $_.Name, $_.Length)
        }
    }
} else {
    Write-Host " BUILD FAILED (exit $code)" -ForegroundColor Red
    Write-Host ""
    if ($cacheMiss) {
        Write-Host " cause: missing dependencies in the local cache (cannot download while offline)." -ForegroundColor Yellow
        Write-Host " fix  : open the accelerator, then rerun with -Online:" -ForegroundColor Yellow
        Write-Host "          powershell -File tools\build.ps1 -Online" -ForegroundColor Gray
    } elseif ($javacErrors.Count -gt 0) {
        Write-Host " cause: compile errors (key lines below; full output above)." -ForegroundColor Yellow
        Write-Host ""
        $javacErrors | Select-Object -First 20 | ForEach-Object { Write-Host "   $_" -ForegroundColor Red }
        if ($javacErrors.Count -gt 20) {
            Write-Host "   ... $($javacErrors.Count - 20) more lines, see full output above" -ForegroundColor Gray
        }
    } else {
        Write-Host " cause: unknown. Please share the full output above."
    }
}
Write-Host ("=" * 68)
exit $code
