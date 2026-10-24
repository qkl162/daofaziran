# 一键初始化本地 git 仓库并提交（不推送；推送用 GitHub Desktop 或下面的命令）
#
# 用法（先装好 Git for Windows：https://git-scm.com/download/win）：
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\init_git.ps1
#
# 之后在 GitHub 上建一个空仓库（不要勾 README），再执行：
#   git remote add origin https://github.com/<你的用户名>/fulu.git
#   git branch -M main
#   git push -u origin main

$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Host "没有找到 git。请先安装 Git for Windows：https://git-scm.com/download/win" -ForegroundColor Red
    exit 1
}

Write-Host "== git 版本 ==" -ForegroundColor Cyan
git --version

if (-not (Test-Path ".git")) {
    Write-Host "== 初始化仓库 ==" -ForegroundColor Cyan
    git init
}

# 提交身份（只影响这个仓库）
$name = git config user.name
if (-not $name) {
    git config user.name "lengyang"
}
$email = git config user.email
if (-not $email) {
    git config user.email "lengyang@users.noreply.github.com"
}

Write-Host "== 加入文件 ==" -ForegroundColor Cyan
git add -A

Write-Host "== 待提交文件数 ==" -ForegroundColor Cyan
(git diff --cached --name-only | Measure-Object -Line).Lines

Write-Host "== 提交 ==" -ForegroundColor Cyan
git commit -m "FuLu 1.0.0: talisman & formation mod for Forge 1.20.1"

Write-Host ""
Write-Host "本地仓库已就绪。接下来：" -ForegroundColor Green
Write-Host "  1) 在 GitHub 建一个空仓库（不要初始化 README）"
Write-Host "  2) git remote add origin https://github.com/<你的用户名>/fulu.git"
Write-Host "  3) git branch -M main"
Write-Host "  4) git push -u origin main"
