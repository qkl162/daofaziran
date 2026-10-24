# deploy.ps1 - 把 build/libs/fulu-*.jar 部署到「阵法师」实例的 mods 目录
#
# 为什么用脚本：旧版先备份到实例根目录（不能放 mods/ 里，会被当成另一个模组加载）；
# 复制完核对大小和 SHA-256；游戏在跑时直接拒绝。
#
# 用法: powershell -NoProfile -ExecutionPolicy Bypass -File tools\deploy.ps1

param(
    [string]$Instance = "C:\Users\lengyang\Desktop\.minecraft\versions\阵法师"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$src = Get-ChildItem (Join-Path $root "build\libs") -Filter "fulu-*.jar" |
    Where-Object { $_.Name -notmatch "sources|javadoc" } |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $src) { throw "build\libs 里没有 fulu-*.jar，先构建" }

$mods = Join-Path $Instance "mods"
if (-not (Test-Path $mods)) { throw "实例的 mods 目录不存在: $mods" }

$java = Get-Process -Name java -ErrorAction SilentlyContinue
if ($java) { throw "游戏还在运行（java pid $($java[0].Id)），先关掉再部署" }

$dest = Join-Path $mods $src.Name
"  源      : $($src.FullName)"
"           $($src.Length) 字节  $($src.LastWriteTime)"
"  目标    : $dest"

if (Test-Path $dest) {
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $backup = Join-Path $Instance ("fulu-backup-" + $stamp + ".jar")
    Copy-Item -LiteralPath $dest -Destination $backup -Force
    "  备份旧版: $backup"
} else {
    "  （目标不存在，是第一次部署）"
}

Copy-Item -LiteralPath $src.FullName -Destination $dest -Force

$srcHash = (Get-FileHash -LiteralPath $src.FullName -Algorithm SHA256).Hash
$dstHash = (Get-FileHash -LiteralPath $dest -Algorithm SHA256).Hash
$dstSize = (Get-Item $dest).Length
"  部署后  : $dstSize 字节"
"  SHA-256 : $($srcHash.Substring(0,16))..."
if ($srcHash -ne $dstHash) { throw "哈希不一致，复制没成功" }
if ($dstSize -ne $src.Length) { throw "大小不一致，复制没成功" }

""
"  OK - 已部署。重启游戏后 /fulu shield 20 的回话应该是正常中文（那是新版的标志）。"
