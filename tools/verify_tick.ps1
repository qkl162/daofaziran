# =====================================================================
# FuLu - block entity tick verification
# =====================================================================
#
# One command that answers "does the formation eye really tick every tick?":
#
#   1. writes a throw-away datapack into run\<world>\datapacks\verify\
#      whose load function runs, 3 s later:
#          setblock 0 -60 0 fulu:formation_eye
#   2. starts the dedicated server (tools\build.ps1 -Task runServer)
#   3. waits for "Done", lets the block sit there for -Seconds seconds
#   4. stops the server, reads run\logs\latest.log and checks:
#        - the formation eye is silent in the log: a freshly placed block scans
#          and announces through the ACTION BAR, but writes nothing to the log
#          (the console print was removed in step 7; DEBUG_SCAN_LOG is off)
#        - the datapack's own 5 s heartbeat proves the server really was alive
#          and the chunk really was ticking, so "silence" is meaningful
#        - 'particle fulu:spirit_mote' loads as a command (the id is registered)
#   5. prints PASS/FAIL and removes the datapack again
#
# What this script CANNOT check: the actual right-click. That needs a player,
# and a headless server has none - so the toggle itself is verified by
# structure (see tools\verify_jar.py) plus your own click in the game.
#
# Usage:
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\verify_tick.ps1
#   powershell ... -File tools\verify_tick.ps1 -Seconds 30
#   powershell ... -File tools\verify_tick.ps1 -KeepDatapack     # keep it for manual poking
#
# ---------------------------------------------------------------------
# Two things this script has to get right (both were real bugs in v1):
#
#   a) "has the server booted?" must not be answered by a STALE log.
#      Minecraft rotates run\logs\latest.log on every start, so the file from
#      the previous run is still sitting there with a "Done (" in it. We delete
#      latest.log before launching and additionally require the file to have
#      been written after we started.
#
#   b) stopping the server via the powershell process tree is NOT enough:
#      ForgeGradle forks the game JVM, so taskkill /T on our own child can miss
#      it and leave a server ticking forever (and holding the world lock).
#      So we also stop every java.exe that was launched after we started.
#      That is a heuristic - it assumes no other java starts during the run.
#
# NOTE: this file is intentionally pure ASCII.
#   Windows PowerShell 5.1 reads .ps1 files as ANSI unless they carry a UTF-8
#   BOM, so Chinese text in here would be mangled into parse errors. All the
#   Chinese documentation lives in README.md instead - same rule as build.ps1.
#
# NOTE: the log line itself is Chinese ("????? ... ? N ?"), but the
#   *logger name* com.lengyang.fulu.FuLu is ASCII, so that is what we grep for.
# =====================================================================

[CmdletBinding()]
param(
    [int]$Seconds = 26,
    [string]$World = "verifyworld",
    [string]$LevelType = "minecraft:flat",
    # Mob scan test: summons zombies in / out of the 11x11x11 box and checks the
    # counts per phase. Needs DEBUG_SCAN_LOG = true (a temporary build) so the
    # announce also lands in the log; the block itself needs no switch any more.
    [switch]$MobTest,
    # Fight test: one zombie (nothing should happen) vs two zombies (they must
    # actually hurt each other). Reads mob health through a scoreboard, so it
    # needs real AI - do NOT use NoAI on the test mobs.
    [switch]$FightTest,
    # Persistence test: set the "has talisman" flag through /data merge, force a
    # chunk unload + reload, and check the flag came back. Also reads the region
    # file off the disk afterwards. Works on the SHIPPED build (no temp flags).
    [switch]$PersistTest,
    # Blockstate test: does the talisman property exist and can it be set?
    # (Rendering the other MODEL is client-side and cannot be checked here.)
    [switch]$StateTest,
    # Talisman test: the tick is in two segments - scanning/announcing happens
    # with or without the talisman, the fighting only with it. Needs a temporary
    # build with DEBUG_SCAN_LOG = true (there is no on/off switch to force any
    # more: the talisman IS the switch, and this test fits it mid-run).
    [switch]$TalismanTest,
    # Display test: the announce + particles walk a 5 s beat while the scan runs
    # every tick. One zombie arrives per SECOND - if the announce were still
    # driven by "the count changed", it would fire ~20 times in a 20 s window.
    [switch]$DisplayTest,
    # Restart test: does the state survive the game being closed and reopened?
    # Two separate server processes on the SAME world:
    #   -Phase 1  wipe, place the eye, fit the talisman, force the chunk to disk
    #   -Phase 2  a NEW server process, no wipe, read it back and use it
    # Runs on the SHIPPED build (no temp flags): everything is checked with
    # /data get, /execute if block and mob health.
    [switch]$RestartTest,
    # Effect test: the ?? marker. In range -> the effect gets applied;
    # stepping out -> removed at once; running far away -> decays by itself.
    [switch]$EffectTest,
    # Combat test: a marked husk fighting a zombie must pass its own attack
    # effect (hunger) on to the zombie, and the marker must be infinite inside
    # the formation but become a 20 s countdown once out of it.
    [switch]$CombatTest,
    # Particle test: particles must be emitted for the marked monsters and for
    # nobody else - the debug line reports how many monsters it emitted for, and
    # a monster parked outside the formation must never show up in that count.
    [switch]$ParticleTest,
    # Outline test: right-clicking with a stick draws the range boundary.
    # There is no player to right-click here, so the geometry is exercised
    # through the DEBUG_OUTLINE_ALWAYS switch and the [outline] log line.
    [switch]$OutlineTest,
    # Codex test: the formation codex's three stages (nothing / seen / used).
    # The unlock triggers need a player, but the STATE lives in a scoreboard,
    # so the test sets the bits by command and checks what the codex renders.
    [switch]$CodexTest,
    # Block test: ?? and ?? - does the block place, does breaking it drop
    # itself, and do both item ids actually resolve.
    [switch]$BlockTest,
    # Codex persistence: phase 1 unlocks something and the server is asked to
    # close (so it saves); phase 2 is a fresh process reading it back.
    [switch]$CodexPersistTest,
    # Ask the server to close instead of killing it (see Stop-TestServer).
    [switch]$GracefulStop,
    [ValidateSet("1", "2")] [string]$Phase = "1",
    # Phase 2 must NOT throw the world away.
    [switch]$KeepWorld,
    [switch]$KeepDatapack,
    # Two talismans: with the chaos talisman the monsters fight, with the flying
    # sword talisman they must NOT (that one has no effect yet). Same pen, same
    # mobs, same number of ticks - only the talisman differs.
    [switch]$TwoTalismanTest,
    # Flying sword: with the sword talisman the eye shoots the nearest monster
    # once a second for exactly 5 damage. ONE zombie only - infighting needs two,
    # so any health loss at all can only have come from the sword.
    [switch]$SwordTest,
    # The flying sword ENTITY: summon one and a zombie, and check it flies over on
    # its own, hits for exactly 5, and removes itself afterwards.
    [switch]$EntityTest,
    [switch]$EnemyScanTest,
    [switch]$AntiGravityTest,
    # The blood pool: kill an animal, a fulu:blood_pool must be left behind;
    # kill a zombie (not an animal) and nothing extra may appear.
    [switch]$BloodPoolTest
)

$ErrorActionPreference = "Continue"

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$run = Join-Path $root "run"
$log = Join-Path $run "logs\latest.log"
$worldDir = Join-Path $run $World
$dp = Join-Path $worldDir "datapacks\verify"
$outDir = Join-Path $root "tools\out"
$serverOut = Join-Path $outDir "verify_tick_server.log"
$serverErr = Join-Path $outDir "verify_tick_server.err"

$TAG = "com.lengyang.fulu.FuLu"
$exitCode = 1

function Write-Head($text) {
    Write-Host ""
    Write-Host ("=" * 68)
    Write-Host " $text"
    Write-Host ("=" * 68)
}

# Read an NBT key straight out of a region file (*.mca), without Minecraft.
#
# This shells out to tools\check_region.py. Why not do it in PowerShell: the
# obvious .NET route (DeflateStream over the zlib payload) returned 0 bytes for
# every chunk on this machine - decompressed nothing, raised nothing - so the
# check silently reported "the key is not on disk" while the key WAS on disk
# (tools\check_region.py finds it). A verification helper that can fail silently
# is worse than no helper, so this one is written in the language that works,
# and its failure is reported instead of swallowed.
#
# The interpreter path is read from tools\python-home.txt (one line, UTF-8) -
# same convention as tools\java-home.txt for the JDK.
function Get-PythonExe {
    $file = Join-Path $root "tools\python-home.txt"
    if (Test-Path $file) {
        $line = (Get-Content $file -Encoding UTF8 |
                 Where-Object { $_.Trim() -ne "" -and -not $_.Trim().StartsWith("#") } |
                 Select-Object -First 1)
        if ($line -and (Test-Path $line.Trim())) { return $line.Trim() }
    }
    $cmd = Get-Command python -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

function Get-NbtKeyInRegion($path, $key, $expected) {
    $script:RegionReadErrors = 0
    $script:RegionReaderBroken = $false
    $script:RegionReaderOutput = ""
    $hits = New-Object System.Collections.ArrayList
    $python = Get-PythonExe
    if (-not $python) {
        $script:RegionReaderBroken = $true
        $script:RegionReaderOutput = "no python: tools\python-home.txt missing/unreadable and no 'python' on PATH"
        return $hits
    }
    $helper = Join-Path $root "tools\check_region.py"
    $args = @($helper, $path, $key)
    if ($null -ne $expected -and "$expected" -ne "") { $args += "$expected" }
    $out = & $python @args 2>&1
    $script:RegionReaderOutput = ($out | ForEach-Object { $_.ToString() }) -join " | "
    foreach ($line in $out) {
        $text = $line.ToString()
        if ($text -match 'found in chunk (\d+),(\d+) -> value byte right after the name: (-?\d+)') {
            [void]$hits.Add([pscustomobject]@{
                x = [int]$Matches[1]; z = [int]$Matches[2]; value = [int]$Matches[3]
            })
        }
        if ($text -match 'RESULT: key NOT found') { $script:RegionReadErrors = -1 }
        if ($text -match 'decompress failed') { $script:RegionReadErrors++ }
        if ($text -match 'No such file|Traceback') { $script:RegionReaderBroken = $true }
    }
    if (@($out).Count -eq 0) { $script:RegionReaderBroken = $true }
    return $hits
}

function Stop-TestServer($proc, $launchTime) {
    # -GracefulStop asks the process to close instead of killing it. That matters
    # for ONE question: does the unlock progress survive a restart? The dedicated
    # server saves the world from a shutdown hook, and a graceful close runs that
    # hook - a taskkill /F does not. (So "the scoreboard is gone" after a /F kill
    # is the harness being unrealistic, not the mod losing data.)
    $script:GracefulFailed = $false
    if ($proc -and -not $proc.HasExited) {
        if ($script:GracefulStop) {
            # Ask the server to write the world to disk, then close. RCON is the
            # only channel this harness has into a running server (see tools\rcon.py);
            # "save-all flush" is exactly what the shutdown hook would do anyway.
            $python = Get-PythonExe
            if ($python) {
                $out = & $python (Join-Path $root "tools\rcon.py") "save-all flush" 2>&1
                $text = ($out | ForEach-Object { $_.ToString() }) -join " | "
                Write-Host " [stop]  $text"
                if ($text -match "Saved the game") {
                    Write-Host " [stop]  the world was written to disk on request"
                } else {
                    Write-Host " [stop]  could not confirm a save; the run cannot say anything about persistence"
                    $script:GracefulFailed = $true
                }
            } else {
                Write-Host " [stop]  no python available for the RCON save"
                $script:GracefulFailed = $true
            }
        }
        if (-not $proc.HasExited) {
            & taskkill /PID $proc.Id /T /F 2>&1 | Out-Null
        }
    }
    Start-Sleep -Seconds 2
    # ForgeGradle forks the game JVM; it is not always in the tree above.
    $leftover = @(Get-Process -Name java -ErrorAction SilentlyContinue |
                  Where-Object { $_.StartTime -gt $launchTime })
    foreach ($p in $leftover) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue }
    if ($leftover.Count -gt 0) {
        Write-Host (" [stop]  also stopped {0} forked java process(es): {1}" -f `
            $leftover.Count, (($leftover | ForEach-Object { $_.Id }) -join ", "))
    }
    Start-Sleep -Seconds 3
}

Write-Head "FuLu - formation_eye tick verification"

# Stop-TestServer needs to know whether to ask the server to close or to kill it.
# (Giving the server a chance to close is what lets its shutdown hook save the world,
# which is the whole point of the persistence test.)
$script:GracefulStop = [bool]$GracefulStop
$script:GracefulFailed = $false

try {
    # ------------------------------------------------------------ 1. files --
    New-Item -ItemType Directory -Force -Path $run, $outDir | Out-Null

    # RCON: the only way this harness can ask a running server to do something.
    # It is used for exactly one command - "save-all flush" - because the world is
    # otherwise only written from the shutdown hook, and the harness cannot close
    # the server politely. The port is not exposed anywhere but localhost.
    $rconPassword = "fulu-verify"
    $propsPath = Join-Path $run "server.properties"
    $wantRcon = @{
        "enable-rcon"    = "true"
        "rcon.port"      = "25575"
        "rcon.password"  = $rconPassword
        "broadcast-rcon-to-ops" = "false"
    }
    if (Test-Path $propsPath) {
        $lines = @(Get-Content $propsPath -Encoding UTF8)
        $changed = $false
        foreach ($key in $wantRcon.Keys) {
            $pattern = "^$([regex]::Escape($key))="
            if (@($lines | Where-Object { $_ -match $pattern }).Count -gt 0) {
                $new = $lines | ForEach-Object { if ($_ -match $pattern) { "$key=$($wantRcon[$key])" } else { $_ } }
                if (($new -join "`n") -ne ($lines -join "`n")) { $changed = $true }
                $lines = @($new)
            } else {
                $lines += "$key=$($wantRcon[$key])"
                $changed = $true
            }
        }
        if ($changed) {
            Set-Content -Encoding ASCII -Path $propsPath -Value $lines
            Write-Host " [setup] enabled RCON in run\server.properties (local only, for save-all)"
        }
    }

    if (-not (Test-Path (Join-Path $run "eula.txt"))) {
        Set-Content -Encoding ASCII -Path (Join-Path $run "eula.txt") -Value "eula=true"
        Write-Host " [setup] wrote run\eula.txt (eula=true)"
    }
    if (-not (Test-Path (Join-Path $run "server.properties"))) {
        $props = @(
            "level-name=$World",
            "level-type=$LevelType",
            "generate-structures=false",
            "spawn-monsters=false",
            "spawn-protection=0",
            "online-mode=false",
            "max-players=1",
            "view-distance=4",
            "simulation-distance=4",
            "sync-chunk-writes=false",
            "max-tick-time=-1"
        )
        Set-Content -Encoding ASCII -Path (Join-Path $run "server.properties") -Value $props
        Write-Host " [setup] wrote run\server.properties (flat world, fast boot)"
    }

    # A FRESH WORLD EVERY RUN.
    #
    # The world folder survives between runs, and so does everything in it: the
    # formation eye placed by the previous run is reloaded (and, in a build where
    # the switch defaults to on, it starts scanning too), leftover mobs are still
    # standing there, and the numbers from two eyes interleave in the log. That
    # produced a bogus FAIL once, so the world is wiped before the datapack is
    # written into it.
    if ((Test-Path $worldDir) -and -not $KeepWorld) {
        Write-Host " [setup] wiping $worldDir (fresh world for every run)"
        Remove-Item -LiteralPath $worldDir -Recurse -Force -ErrorAction SilentlyContinue
    } elseif ($KeepWorld) {
        Write-Host " [setup] KEEPING $worldDir (this run continues from the previous one)"
    }

    # the throw-away datapack
    New-Item -ItemType Directory -Force -Path `
        (Join-Path $dp "data\minecraft\tags\functions"), (Join-Path $dp "data\verify\functions") | Out-Null
    Set-Content -Encoding ASCII -Path (Join-Path $dp "pack.mcmeta") `
        -Value '{ "pack": { "pack_format": 15, "description": "fulu tick verification" } }'
    Set-Content -Encoding ASCII -Path (Join-Path $dp "data\minecraft\tags\functions\load.json") `
        -Value '{ "values": [ "verify:start" ] }'
    Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
        -Value '# runs on datapack load; delay so the spawn chunk exists by then', `
               'schedule function verify:place 3s', `
               'schedule function verify:mote 5s', `
               'schedule function verify:beat 5s'
    # IMPORTANT: a block placed by command does NOT keep its chunk loaded. If
    # (0,0) happens to be outside the spawn chunks, the chunk unloads after a
    # few seconds and the block entity simply stops ticking - which looks
    # exactly like "the cooldown broke". forceload keeps it entity-ticking for
    # the whole run. (v2 of this script was fooled by this for a while.)
    Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
        -Value '# keep the chunk entity-ticking, otherwise the block falls asleep', `
               'forceload add 0 0', `
               '# flat world: grass top is y=-61, so -60 is open air', `
               'setblock 0 -60 0 fulu:formation_eye'

    if ($BloodPoolTest) {
        # ------------------------------------------------------ blood pool --
        # A pool now needs a PLAYER kill, and a dedicated server has no player - so the
        # positive case cannot be produced here at all. What CAN be tested is the other
        # direction: /kill (no killer credit) must leave nothing, and a non-animal must
        # leave nothing. The positive case is yours to confirm in game.
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'gamerule doMobSpawning false', `
                   'gamerule doDaylightCycle false', `
                   'gamerule doMobLoot false', `
                   'scoreboard objectives add fuBlood dummy', `
                   'summon minecraft:cow 0 -60 0 {PersistenceRequired:1b,NoAI:1b}', `
                   'execute if entity @e[type=fulu:blood_pool] run say [verify] BLOOD-TOO-EARLY', `
                   'say [verify] phase-PLACED', `
                   'schedule function verify:b2 20t'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\b2.mcfunction") `
            -Value 'kill @e[type=minecraft:cow]', `
                   'schedule function verify:b3 10t'
        # Count with a scoreboard. Do NOT use @e[...,limit=2] with "if entity": limit caps
        # how many the selector returns, it does NOT ask for "at least two", so that form
        # succeeds with a single entity and the test quietly lies.
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\b3.mcfunction") `
            -Value 'scoreboard players set #pools fuBlood 0', `
                   'execute as @e[type=fulu:blood_pool] run scoreboard players add #pools fuBlood 1', `
                   'execute if score #pools fuBlood matches 0 run say [verify] BLOOD-NONE-AFTER-KILLED-COW', `
                   'execute if score #pools fuBlood matches 1.. run say [verify] BLOOD-POOL-WITHOUT-PLAYER', `
                   'summon minecraft:zombie 4 -60 0 {PersistenceRequired:1b,NoAI:1b}', `
                   'schedule function verify:b4 20t'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\b4.mcfunction") `
            -Value 'kill @e[type=minecraft:zombie]', `
                   'scoreboard players set #pools fuBlood 0', `
                   'execute as @e[type=fulu:blood_pool] run scoreboard players add #pools fuBlood 1', `
                   'execute if score #pools fuBlood matches 0 run say [verify] BLOOD-NONE-AFTER-ZOMBIE', `
                   'execute if score #pools fuBlood matches 1.. run say [verify] BLOOD-FROM-NON-ANIMAL', `
                   'say [verify] phase-DONE'
        Write-Host " [setup] datapack (blood pool test) -> $dp"
    } elseif ($AntiGravityTest) {
        # ------------------------------------------- anti-gravity test --
        # Put the anti-gravity talisman on the eye, stand a zombie next to it and
        # watch its height: it must be lifted, it must stop at the 5 block ceiling
        # and it must carry the fulu:antigravity effect.
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'gamerule doMobSpawning false', `
                   'gamerule doDaylightCycle false', `
                   'gamerule doMobLoot false', `
                   'gamerule doImmediateRespawn true', `
                   'time set midnight', `
                   'scoreboard objectives add fuHp dummy', `
                   'scoreboard objectives add fuX dummy', `
                   'setblock 0 -60 0 fulu:formation_eye[talisman=antigravity]', `
                   'data merge block 0 -60 0 {Talisman:"antigravity"}', `
                   'summon minecraft:zombie 3 -60 0 {PersistenceRequired:1b}', `
                   'say [verify] phase-FLOAT', `
                   'schedule function verify:f2 5t'
        # ??? y=-60????? -55???? -60 ?????
        #   Y <= -59 ????      -> ????
        #   Y >= -57 ????
        #   Y >= -55 ???5 ??
        #   Y >= -53 ????????
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\f2.mcfunction") `
            -Value 'execute as @e[type=minecraft:zombie] store result score @s fuHp run data get entity @s Pos[1]', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches ..-59 run say [verify] FLOAT-ONGROUND', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches -58.. run say [verify] FLOAT-LIFTED', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches -55.. run say [verify] FLOAT-CAPPED', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches -53.. run say [verify] FLOAT-OVER', `
                   'execute as @e[type=minecraft:zombie] if data entity @s {ActiveEffects:[{"forge:id":"fulu:antigravity","Duration":-1}]} run say [verify] FLOAT-INFINITE', `
                   'execute as @e[type=minecraft:zombie] if data entity @s {ActiveEffects:[{"forge:id":"fulu:antigravity"}]} unless data entity @s {ActiveEffects:[{"forge:id":"fulu:antigravity","Duration":-1}]} run say [verify] FLOAT-TIMED', `
                   'execute as @e[type=minecraft:zombie] unless data entity @s {ActiveEffects:[{"forge:id":"fulu:antigravity"}]} run say [verify] FLOAT-NOMARK', `
                   'execute as @e[type=minecraft:zombie] if data entity @s {ActiveEffects:[{"forge:id":"fulu:antigravity"}]} run say [verify] FLOAT-EFFECT', `
                   'execute as @e[type=minecraft:zombie] store result score @s fuX run data get entity @s Pos[0]', `
                   'execute as @e[type=minecraft:zombie] if score @s fuX matches 2..4 run say [verify] FROZEN', `
                   'execute as @e[type=minecraft:zombie] unless score @s fuX matches 2..4 run say [verify] MOVED-FAR', `
                   'scoreboard players add #n fuHp 1', `
                   'execute if score #n fuHp matches ..119 run schedule function verify:f2 5t', `
                   'execute if score #n fuHp matches 120.. run say [verify] phase-CHECK'
        Write-Host " [setup] datapack (anti-gravity) -> $dp"
        Write-Host " [setup] the zombie must rise, stop at 5 blocks, and carry the effect"
    } elseif ($EnemyScanTest) {
        # ------------------------------------------- enemy scan test --
        # The eye used to scan for `Monster`. Slimes, magma cubes, ghasts,
        # phantoms and hoglins do NOT extend Monster - they only implement the
        # Enemy interface - so the eye simply could not see them.
        # This test lines up four mobs and checks who gets recognised:
        #   slime    (Enemy, not Monster)  -> must be recognised
        #   hoglin   (Enemy, not Monster)  -> must be recognised
        #   wolf     (tamed, so Enemy)     -> must be left alone
        #   villager (not an Enemy)        -> must be left alone
        # Recognition is observed through the chaos formation's mark.
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'gamerule doMobSpawning false', `
                   'gamerule doDaylightCycle false', `
                   'gamerule doMobLoot false', `
                   'time set midnight', `
                   'scoreboard objectives add fuHp dummy', `
                   'setblock 0 -60 0 fulu:formation_eye[talisman=chaos]', `
                   'data merge block 0 -60 0 {Talisman:"chaos"}', `
                   'summon minecraft:slime 3 -59 0 {Size:4,PersistenceRequired:1b}', `
                   'summon minecraft:hoglin -3 -60 0 {PersistenceRequired:1b}', `
                   'summon minecraft:wolf 0 -60 3 {Owner:[I;1,2,3,4],PersistenceRequired:1b}', `
                   'summon minecraft:villager 0 -60 -3 {PersistenceRequired:1b}', `
                   'say [verify] phase-MOBS', `
                   'schedule function verify:q2 1t'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\q2.mcfunction") `
            -Value 'execute as @e[type=minecraft:slime] if data entity @s {ActiveEffects:[{"forge:id":"fulu:infighting"}]} run say [verify] SLIME-MARKED', `
                   'execute as @e[type=minecraft:slime] unless data entity @s {ActiveEffects:[{"forge:id":"fulu:infighting"}]} run say [verify] SLIME-MISSED', `
                   'execute as @e[type=minecraft:hoglin] if data entity @s {ActiveEffects:[{"forge:id":"fulu:infighting"}]} run say [verify] HOGLIN-MARKED', `
                   'execute as @e[type=minecraft:hoglin] unless data entity @s {ActiveEffects:[{"forge:id":"fulu:infighting"}]} run say [verify] HOGLIN-MISSED', `
                   'execute as @e[type=minecraft:wolf] if data entity @s {ActiveEffects:[{"forge:id":"fulu:infighting"}]} run say [verify] WOLF-MARKED', `
                   'execute as @e[type=minecraft:wolf] unless data entity @s {ActiveEffects:[{"forge:id":"fulu:infighting"}]} run say [verify] WOLF-UNMARKED', `
                   'execute as @e[type=minecraft:villager] if data entity @s {ActiveEffects:[{"forge:id":"fulu:infighting"}]} run say [verify] VILLAGER-MARKED', `
                   'execute as @e[type=minecraft:villager] unless data entity @s {ActiveEffects:[{"forge:id":"fulu:infighting"}]} run say [verify] VILLAGER-UNMARKED', `
                   'scoreboard players add #n fuHp 1', `
                   'execute if score #n fuHp matches ..59 run schedule function verify:q2 1t', `
                   'execute if score #n fuHp matches 60.. run say [verify] phase-CHECK'
        Write-Host " [setup] datapack (enemy scan) -> $dp"
        Write-Host " [setup] slime+hoglin must be seen, tamed wolf+villager must not"
    } elseif ($EntityTest) {
        # ----------------------------------------------------- entity test --
        # SUM-ONLY: nothing here uses the formation. A flying sword entity is
        # summoned 6 blocks from a zombie and must find it, fly over, deal exactly
        # 5 and then be gone. That is the entity's own behaviour, tested with no
        # block, player or GUI in the way.
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'gamerule doMobSpawning false', `
                   'gamerule doDaylightCycle false', `
                   'gamerule doMobLoot false', `
                   'time set midnight', `
                   'scoreboard objectives add fuHp dummy', `
                   'summon minecraft:zombie 6 -60 0 {PersistenceRequired:1b}', `
                   'summon fulu:flying_sword 0 -60 0', `
                   'execute if entity @e[type=fulu:flying_sword] run say [verify] SWORD-ALIVE', `
                   'execute unless entity @e[type=fulu:flying_sword] run say [verify] SWORD-NOT-SUMMONED', `
                   'say [verify] phase-SUMMONED', `
                   'schedule function verify:e2 20t'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\e2.mcfunction") `
            -Value 'execute as @e[type=minecraft:zombie] store result score @s fuHp run data get entity @s Health', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 20 run say [verify] HP-20', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 15 run say [verify] HP-15', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 10 run say [verify] HP-10', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 5 run say [verify] HP-5', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches ..4 run say [verify] HP-LOW', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 16..19 run say [verify] HP-WRONG', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 11..14 run say [verify] HP-WRONG', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 6..9 run say [verify] HP-WRONG', `
                   'execute if entity @e[type=fulu:flying_sword] run say [verify] SWORD-STILL-THERE', `
                   'execute unless entity @e[type=fulu:flying_sword] run say [verify] SWORD-GONE', `
                   '# second phase: put a wall between the sword and its target', `
                   'kill @e[type=minecraft:zombie]', `
                   'kill @e[type=fulu:flying_sword]', `
                   'fill 2 -60 0 2 -59 0 minecraft:stone', `
                   'summon minecraft:zombie 5 -60 0 {PersistenceRequired:1b}', `
                   'summon fulu:flying_sword 0 -60 0', `
                   'say [verify] phase-WALL', `
                   'schedule function verify:e3 20t'
        # 第三部分：撞到方块就该消失 —— 剑在墙上没了，墙后的僵尸一点血都没掉
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\e3.mcfunction") `
            -Value 'execute unless entity @e[type=fulu:flying_sword] run say [verify] SWORD-BLOCKED-GONE', `
                   'execute if entity @e[type=fulu:flying_sword] run say [verify] SWORD-WENT-THROUGH', `
                   'execute as @e[type=minecraft:zombie] store result score @s fuHp run data get entity @s Health', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 20 run say [verify] WALL-ZOMBIE-UNHURT', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches ..19 run say [verify] WALL-ZOMBIE-HURT', `
                   'say [verify] phase-CHECK'
        Write-Host " [setup] datapack (flying sword entity) -> $dp"
        Write-Host " [setup] expecting SWORD-ALIVE -> damage in steps of 5 -> SWORD-GONE"
    } elseif ($SwordTest) {
        # ------------------------------------------------------ sword test --
        # The formation now fires a VOLLEY: 10 swords a second, shared out over
        # the monsters in range. So this test checks the volley itself:
        #   a) with no monsters at all, nothing must be launched
        #   b) with three monsters (2, 3 and 4 blocks away) the split is 3/3/4
        #      - visible as: the two near ones take 15 (3 swords) and survive at
        #        5 hp, the far one takes 20 (4 swords) and dies
        #   c) ten swords exist in the world at once
        #   d) no infighting mark (that belongs to the chaos formation)
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'gamerule doMobSpawning false', `
                   'gamerule doDaylightCycle false', `
                   'gamerule doMobLoot false', `
                   'time set midnight', `
                   'scoreboard objectives add fuHp dummy', `
                   'scoreboard players set #n fuHp 0', `
                   'setblock 0 -60 0 fulu:formation_eye[talisman=sword]', `
                   'data merge block 0 -60 0 {Talisman:"sword"}', `
                   'say [verify] phase-EYE-READY', `
                   '# give it 3 s with NO monsters at all', `
                   'schedule function verify:s0 60t'
        # (a) empty: nothing may have been launched
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\s0.mcfunction") `
            -Value 'execute if entity @e[type=fulu:flying_sword] run say [verify] VOLLEY-EMPTY-WRONG', `
                   'execute unless entity @e[type=fulu:flying_sword] run say [verify] NO-VOLLEY-EMPTY', `
                   '# now three monsters, at increasing distance from the eye', `
                   'summon minecraft:zombie 2 -60 0 {PersistenceRequired:1b}', `
                   'summon minecraft:zombie 3 -60 0 {PersistenceRequired:1b}', `
                   'summon minecraft:zombie 4 -60 0 {PersistenceRequired:1b}', `
                   'say [verify] phase-THREE-ZOMBIES', `
                   'schedule function verify:s2 20t'
        # (b)(c)(d) sample every tick - the survivors die on the SECOND volley,
        # so the first volley's signature has to be caught as it happens
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\s2.mcfunction") `
            -Value '# after 2 s, switch to a 100 hp zombie: one volley of ten swords at 5',
                   '# each must take exactly 50 off it. (With the vanilla invulnerability',
                   '# window left alone only ONE sword lands and it would drop by 5.)',
                   'execute if score #n fuHp matches 40 run kill @e[type=minecraft:zombie]',
                   'execute if score #n fuHp matches 40 run kill @e[type=fulu:flying_sword]',
                   'execute if score #n fuHp matches 40 run summon minecraft:zombie -4 -60 0 {PersistenceRequired:1b,Attributes:[{Name:"generic.max_health",Base:100.0}],Health:100.0f}',
                   'scoreboard players set #cnt fuHp 0', `
                   'execute as @e[type=fulu:flying_sword] run scoreboard players add #cnt fuHp 1', `
                   'execute if score #cnt fuHp matches 10.. run say [verify] SWORDS-10', `
                   'execute if score #cnt fuHp matches 1..9 run say [verify] SWORDS-FEW', `
                   'scoreboard players set #left fuHp 0', `
                   'execute as @e[type=minecraft:zombie] run scoreboard players add #left fuHp 1', `
                   'execute if score #left fuHp matches 2 run say [verify] LEFT-2', `
                   'execute if score #left fuHp matches 3 run say [verify] LEFT-3', `
                   'execute as @e[type=minecraft:zombie] store result score @s fuHp run data get entity @s Health', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 50 run say [verify] BIG-AFTER-ONE-VOLLEY', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 95..99 run say [verify] BIG-ONE-HIT-ONLY', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 5 run say [verify] HP-5', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 15 run say [verify] HP-15', `
                   'execute as @e[type=minecraft:zombie] unless data entity @s {ActiveEffects:[{"forge:id":"fulu:infighting"}]} run say [verify] NO-MARK', `
                   'execute if entity @e[type=fulu:flying_sword] run say [verify] ENTITY-SEEN', `
                   'scoreboard players add #n fuHp 1', `
                   'execute if score #n fuHp matches ..59 run schedule function verify:s2 1t', `
                   'execute if score #n fuHp matches 60.. run say [verify] phase-CHECK'
        Write-Host " [setup] datapack (sword test) -> $dp"
        Write-Host " [setup] expecting health to fall in steps of 5, no HP-WRONG, NO-MARK seen"
    } elseif ($TwoTalismanTest) {
        # ------------------------------------------------- two talismans --
        # ??? -> ??; ??? -> ?????????????
        # ???????????????????????????
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'gamerule doMobSpawning false', `
                   'gamerule doDaylightCycle false', `
                   'time set midnight', `
                   'scoreboard objectives add fuHp dummy', `
                   '# a 7x7 pen, two blocks high: mobs with AI stay in range, and they',
                   '# can still path to each other and fight', `
                   'fill -3 -60 -3 3 -59 -3 minecraft:stone', `
                   'fill -3 -60 3 3 -59 3 minecraft:stone', `
                   'fill -3 -60 -3 -3 -59 3 minecraft:stone', `
                   'fill 3 -60 -3 3 -59 3 minecraft:stone', `
                   '# chaos talisman: set both the block state and the block entity', `
                   'setblock 0 -60 0 fulu:formation_eye[talisman=chaos]', `
                   'data merge block 0 -60 0 {Talisman:"chaos"}', `
                   'say [verify] phase-CHAOS', `
                   'summon minecraft:zombie 1 -60 0 {PersistenceRequired:1b}', `
                   'summon minecraft:zombie -1 -60 0 {PersistenceRequired:1b}', `
                   'schedule function verify:t2 14s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\t2.mcfunction") `
            -Value 'execute as @e[type=minecraft:zombie] store result score @s fuHp run data get entity @s Health', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches ..19 run say [verify] CHAOS-HURT', `
                   'execute unless entity @e[type=minecraft:zombie] run say [verify] CHAOS-KILLED', `
                   'execute if entity @e[type=minecraft:zombie] run say [verify] CHAOS-ALIVE', `
                   '# ???????????', `
                   'execute as @e[type=minecraft:zombie] if data entity @s {ActiveEffects:[{"forge:id":"fulu:infighting"}]} run say [verify] CHAOS-MARKED', `
                   'kill @e[type=minecraft:zombie]', `
                   'say [verify] phase-SWORD', `
                   '# now the other talisman, everything else identical', `
                   'setblock 0 -60 0 fulu:formation_eye[talisman=sword]', `
                   'data merge block 0 -60 0 {Talisman:"sword"}', `
                   'summon minecraft:zombie 1 -60 0 {PersistenceRequired:1b}', `
                   'summon minecraft:zombie -1 -60 0 {PersistenceRequired:1b}', `
                   'schedule function verify:t3 1t'
        # ??????????????**????**????????
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\t3.mcfunction") `
            -Value 'execute as @e[type=minecraft:zombie] store result score @s fuHp run data get entity @s Health', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches ..19 run say [verify] SWORD-HURT', `
                   'execute if entity @e[type=minecraft:zombie] run say [verify] SWORD-ALIVE', `
                   'execute if entity @e[type=fulu:flying_sword] run say [verify] ENTITY-SEEN', `
                   '# ???**?**?????????????????', `
                   'execute as @e[type=minecraft:zombie] unless data entity @s {ActiveEffects:[{"forge:id":"fulu:infighting"}]} run say [verify] SWORD-NO-MARK', `
                   'execute as @e[type=minecraft:zombie] if data entity @s {ActiveEffects:[{"forge:id":"fulu:infighting"}]} run say [verify] SWORD-MARKED', `
                   'scoreboard players add #s fuHp 1', `
                   'execute if score #s fuHp matches ..79 run schedule function verify:t3 1t', `
                   'execute if score #s fuHp matches 80.. run say [verify] phase-CHECK'
        Write-Host " [setup] datapack (two talismans) -> $dp"
        Write-Host " [setup] chaos: fighting + marked; sword: damage from the sword, no mark"
    } elseif ($CodexPersistTest) {
        # --------------------------------------------- codex persistence --
        # "??????" - the only honest test is to actually stop the server and
        # start a new one, which is what -Phase 1 / -Phase 2 do.
        #
        # Two things make this different from the other restart test:
        #   * it is run with -GracefulStop, so the server is ASKED to close and its
        #     shutdown hook saves the world (a taskkill /F does not, and then the
        #     scoreboard would legitimately be missing - that would be the harness
        #     lying about the mod).
        #   * what it checks is the unlock progress itself.
        if ($Phase -eq "1") {
            Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
                -Value 'schedule function verify:p1 3s'
            Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\p1.mcfunction") `
                -Value '# pretend the player has seen AND activated the formation', `
                       'scoreboard objectives add fuluCodex dummy', `
                       'scoreboard players set Tester fuluCodex 3', `
                       'execute if score Tester fuluCodex matches 3 run say [verify] SET-OK', `
                       'say [verify] phase-SET'
            Write-Host " [setup] datapack (persistence, phase 1: unlock, then close cleanly) -> $dp"
        } else {
            Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
                -Value 'schedule function verify:p2 3s'
            Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\p2.mcfunction") `
                -Value '# the objective comes back from disk by itself; adding it again would', `
                       '# only matter if the save never happened', `
                       'scoreboard objectives add fuluCodex dummy', `
                       'execute if score Tester fuluCodex matches 3 run say [verify] SCORE-SURVIVED', `
                       'execute unless score Tester fuluCodex matches 3 run say [verify] SCORE-LOST', `
                       'say [verify] phase-CHECK'
            Write-Host " [setup] datapack (persistence, phase 2: is it still there?) -> $dp"
        }
    } elseif ($BlockTest) {
        # ------------------------------------------------------ block test --
        #   setblock            -> the block exists and can live in the world
        #   execute if block    -> reading it back proves it really is that block
        #   loot spawn .. mine  -> this is the "breaking it" step: it runs the
        #                          block's OWN loot table, so if that table is
        #                          missing or points elsewhere, nothing (or the
        #                          wrong thing) ends up on the ground.
        #   summon item         -> proves the ITEM id resolves; a bad id makes
        #                          summon fail loudly in the log
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'setblock 0 -60 0 fulu:formation_base', `
                   'execute if block 0 -60 0 fulu:formation_base run say [verify] BASE-PLACED', `
                   'execute unless block 0 -60 0 fulu:formation_base run say [verify] BASE-NOT-PLACED', `
                   'loot spawn 0 -59 0 mine 0 -60 0', `
                   'execute if data entity @e[type=item,limit=1,distance=..4] {Item:{id:"fulu:formation_base"}} run say [verify] BASE-DROPS-SELF', `
                   'execute unless data entity @e[type=item,limit=1,distance=..4] {Item:{id:"fulu:formation_base"}} run say [verify] BASE-DROPS-WRONG', `
                   'kill @e[type=item]', `
                   'summon item 0 -59 0 {Item:{id:"fulu:cinnabar",Count:1b}}', `
                   'execute if data entity @e[type=item,limit=1,distance=..2] {Item:{id:"fulu:cinnabar"}} run say [verify] CINNABAR-EXISTS', `
                   'execute unless data entity @e[type=item,limit=1,distance=..2] {Item:{id:"fulu:cinnabar"}} run say [verify] CINNABAR-MISSING', `
                   'kill @e[type=item]', `
                   'setblock 0 -60 0 air', `
                   'execute if block 0 -60 0 air run say [verify] BASE-REMOVED', `
                   'say [verify] phase-DONE'
        Write-Host " [setup] datapack (block test) -> $dp"
        Write-Host " [setup] expecting BASE-PLACED, BASE-DROPS-SELF, CINNABAR-EXISTS"
    } elseif ($CodexTest) {
        # ------------------------------------------------------ codex test --
        # Three stages, matching how a player would unlock them:
        #   NEW-WORLD  nothing unlocked          -> the entry must read ???
        #   PLACED     bit 1 (placed an eye)     -> the name shows, detail still locked
        #   ACTIVATED  bit 3 (fitted a talisman) -> the full detail (5 lines)
        # Plus one bogus id, to show the "no such formation" path.
        #
        # The unlock TRIGGERS need a player (placing a block, right-clicking), so
        # they cannot be exercised here; everything downstream can. The state is a
        # scoreboard entry, so the test writes it by command and the codex renders
        # from it exactly as it would for a real player.
        #
        # Needs a temporary build with DEBUG_CODEX_LOG = true.
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   '# the codex creates this lazily on success, but a test that SETS',
                   '# scores needs it to exist up front', `
                   'scoreboard objectives add fuluCodex dummy', `
                   'say [verify] phase-NEW-WORLD', `
                   'fulu codex list Tester', `
                   'schedule function verify:c2 2s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\c2.mcfunction") `
            -Value '# bit 1 = placed an eye', `
                   'scoreboard players set Tester fuluCodex 1', `
                   'say [verify] phase-PLACED', `
                   'fulu codex list Tester', `
                   'fulu codex show infighting Tester', `
                   'fulu codex find infight', `
                   'schedule function verify:c3 2s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\c3.mcfunction") `
            -Value '# bit 2 as well = fitted a talisman and activated it', `
                   'scoreboard players set Tester fuluCodex 3', `
                   'say [verify] phase-ACTIVATED', `
                   'fulu codex list Tester', `
                   'fulu codex show infighting Tester', `
                   'fulu codex show nosuchformation Tester', `
                   'fulu codex find infight', `
                   'fulu codex find zzzz', `
                   'say [verify] phase-DONE'
        Write-Host " [setup] datapack (codex test) -> $dp"
        Write-Host " [setup] expecting named 0 -> 1, and detail lines 3 -> 5"
    } elseif ($OutlineTest) {
        # ---------------------------------------------------- outline test --
        # Right-clicking with the outline tool (a stick for now) draws the range
        # boundary as particles, for that player only.
        #
        # A headless server has no player, so the right-click itself cannot be
        # exercised. Instead this runs a temp build with DEBUG_OUTLINE_ALWAYS,
        # which makes the tick request the outline over and over - the same code
        # path the click leads to - and logs '[outline] points=.. r=.. y=..'.
        # That pins the GEOMETRY (how many dots, on which ring, at what height);
        # whether they look right needs a client.
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   '# y=100 so the ring is in open air and the height is readable', `
                   'setblock 0 100 0 fulu:formation_eye', `
                   'say [verify] phase-PLACED', `
                   'schedule function verify:o2 6s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\o2.mcfunction") `
            -Value 'say [verify] phase-WINDOW-END'
        Write-Host " [setup] datapack (outline test) -> $dp"
        Write-Host " [setup] expecting [outline] points=80 r=10 y=100.1 sent=0"
    } elseif ($ParticleTest) {
        # --------------------------------------------------- particle test --
        # The debug line says: how many monsters it emitted particles for, how
        # many particles that is, and how many players were close enough to get
        # them. With three monsters inside the formation and ONE parked outside
        # it, every line must say 3 - the outsider must never be counted.
        #
        # Nobody is connected, so no packet is actually sent; the count is the
        # intent. Whether the particles LOOK right needs a client, which this
        # harness cannot provide.
        #
        # Needs a temporary build with DEBUG_SCAN_LOG = true.
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'gamerule doMobSpawning false', `
                   'gamerule doDaylightCycle false', `
                   'gamerule doWeatherCycle false', `
                   'time set midnight', `
                   'weather clear', `
                   'fill -3 -60 -3 3 -59 -3 minecraft:stone', `
                   'fill -3 -60 3 3 -59 3 minecraft:stone', `
                   'fill -3 -60 -3 -3 -59 3 minecraft:stone', `
                   'fill 3 -60 -3 3 -59 3 minecraft:stone', `
                   'setblock 0 -60 0 fulu:formation_eye', `
                   '# the fighting needs a talisman, but the MARKING (and therefore the', `
                   '# particles) does not - fitting one keeps this test closer to real use', `
                   'data merge block 0 -60 0 {Talisman:"chaos"}', `
                   'say [verify] phase-PLACED', `
                   'schedule function verify:pt2 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\pt2.mcfunction") `
            -Value '# three inside the formation (x/z within +-5 of the eye)', `
                   'summon minecraft:zombie 2 -60 0 {PersistenceRequired:1b}', `
                   'summon minecraft:zombie -2 -60 0 {PersistenceRequired:1b}', `
                   'summon minecraft:zombie 0 -60 2 {PersistenceRequired:1b}', `
                   '# and one well outside. NoAI so it cannot wander in and ruin the count.', `
                   'summon minecraft:zombie 12 -60 0 {PersistenceRequired:1b,NoAI:1b,NoGravity:1b}', `
                   'say [verify] phase-SPAWNED', `
                   'schedule function verify:pt3 4s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\pt3.mcfunction") `
            -Value '# the monster outside must still be outside', `
                   'execute if entity @e[type=minecraft:zombie,distance=..5] run say [verify] SOMEONE-INSIDE', `
                   'say [verify] phase-WINDOW-END'
        Write-Host " [setup] datapack (particle test) -> $dp"
        Write-Host " [setup] expecting every [mark] line to say marked=3 (never 4)"
    } elseif ($CombatTest) {
        # ----------------------------------------------------- combat test --
        # Three claims:
        #   a marked Husk hits a Zombie -> the zombie picks up HUNGER
        #        (the husk's own "hit a player -> hunger" effect, passed on)
        #   inside the formation -> the marker's duration is INFINITE (-1)
        #   stepped out          -> the same marker becomes a 20 s countdown (400)
        #
        # Both mobs get 200 max health so neither dies mid-test: a dead zombie
        # cannot be checked for hunger, and that would look like a product bug.
        # Runs on the SHIPPED build (no temp flags).
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'gamerule doMobSpawning false', `
                   'gamerule doDaylightCycle false', `
                   'gamerule doWeatherCycle false', `
                   'time set midnight', `
                   'weather clear', `
                   'scoreboard objectives add fuFlag dummy', `
                   'fill -3 -60 -3 3 -59 -3 minecraft:stone', `
                   'fill -3 -60 3 3 -59 3 minecraft:stone', `
                   'fill -3 -60 -3 -3 -59 3 minecraft:stone', `
                   'fill 3 -60 -3 3 -59 3 minecraft:stone', `
                   'setblock 0 -60 0 fulu:formation_eye', `
                   '# the marker is handed out with or without a talisman, but the FIGHTING',
                   '# only happens once one is fitted (step 10 parts 4 and 7) - so fit it,',
                   '# otherwise this test watches two marked mobs stand around politely', `
                   'data merge block 0 -60 0 {Talisman:"chaos"}', `
                   'say [verify] phase-PLACED', `
                   'schedule function verify:f1 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\f1.mcfunction") `
            -Value '# a husk (attack effect: hunger) and a zombie (no attack effect)', `
                   'summon minecraft:husk 2 -60 0 {PersistenceRequired:1b,Tags:["atk"],Attributes:[{Name:"minecraft:generic.max_health",Base:200.0}],Health:200f}', `
                   'summon minecraft:zombie -2 -60 0 {PersistenceRequired:1b,Tags:["vic"],Attributes:[{Name:"minecraft:generic.max_health",Base:200.0}],Health:200f}', `
                   'say [verify] phase-SPAWNED', `
                   'schedule function verify:f2 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\f2.mcfunction") `
            -Value '# inside the formation the marker must be INFINITE (-1)', `
                   'execute store result score #d fuFlag run data get entity @e[tag=atk,limit=1] ActiveEffects[0].Duration', `
                   'execute if score #d fuFlag matches ..-1 run say [verify] INSIDE-INFINITE', `
                   'execute if score #d fuFlag matches 0.. run say [verify] INSIDE-NOT-INFINITE', `
                   'say [verify] phase-MARKED', `
                   'schedule function verify:f3 5s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\f3.mcfunction") `
            -Value '# 5 s of fighting. Hunger is the vanilla id 17. The two effects on the',
                   '# victim (our marker + hunger) sit in a hash map, so CHECK BOTH SLOTS -',
                   '# which slot is which is not defined.',
                   'execute store result score #i0 fuFlag run data get entity @e[tag=vic,limit=1] ActiveEffects[0].Id', `
                   'execute store result score #i1 fuFlag run data get entity @e[tag=vic,limit=1] ActiveEffects[1].Id', `
                   'execute if score #i0 fuFlag matches 17 run say [verify] VICTIM-GOT-HUNGER', `
                   'execute if score #i1 fuFlag matches 17 run say [verify] VICTIM-GOT-HUNGER', `
                   'execute unless score #i0 fuFlag matches 17 run say [verify] SLOT0-NOT-HUNGER', `
                   'execute unless score #i1 fuFlag matches 17 run say [verify] SLOT1-NOT-HUNGER', `
                   'execute if score #i0 fuFlag matches 1..16 run say [verify] SLOT0-OTHER-VANILLA', `
                   'execute if score #i0 fuFlag matches 18.. run say [verify] SLOT0-OTHER-VANILLA', `
                   'execute store result score #h fuFlag run data get entity @e[tag=vic,limit=1] Health', `
                   'execute if score #h fuFlag matches ..199 run say [verify] VICTIM-TOOK-DAMAGE', `
                   'execute if entity @e[tag=vic] run say [verify] VICTIM-ALIVE', `
                   'say [verify] phase-FIGHT', `
                   'schedule function verify:f4 8s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\f4.mcfunction") `
            -Value '# a second sample, later in the fight: the transfer keeps happening', `
                   'execute store result score #i0 fuFlag run data get entity @e[tag=vic,limit=1] ActiveEffects[0].Id', `
                   'execute store result score #i1 fuFlag run data get entity @e[tag=vic,limit=1] ActiveEffects[1].Id', `
                   'execute if score #i0 fuFlag matches 17 run say [verify] VICTIM-GOT-HUNGER-LATER', `
                   'execute if score #i1 fuFlag matches 17 run say [verify] VICTIM-GOT-HUNGER-LATER', `
                   '# step out of the formation: the infinite marker becomes 20 s.', `
                   '# x=13: outside the +-10 column (step 11 part 1 widened it), but still',
                   '# inside the +16 clear margin - so the conversion happens right here.', `
                   'tp @e[tag=atk] 13 -60 0', `
                   'say [verify] phase-STEPPED-OUT', `
                   'schedule function verify:f5 1s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\f5.mcfunction") `
            -Value 'execute store result score #d fuFlag run data get entity @e[tag=atk,limit=1] ActiveEffects[0].Duration', `
                   'execute if score #d fuFlag matches 1..400 run say [verify] OUTSIDE-20S', `
                   'execute if score #d fuFlag matches ..-1 run say [verify] OUTSIDE-STILL-INFINITE', `
                   'say [verify] phase-CHECK'
        Write-Host " [setup] datapack (combat test) -> $dp"
        Write-Host " [setup] expecting: INSIDE-INFINITE, VICTIM-GOT-HUNGER, OUTSIDE-20S"
    } elseif ($EffectTest) {
        # ----------------------------------------------------- effect test --
        # Three separate claims, tested separately:
        #   in range         -> the marker is applied by the tick
        #   just stepped out -> removed AT ONCE, not by waiting it out. Proved by
        #                       checking 10 ticks (0.5 s) after the teleport: the
        #                       effect lasts 2 s, so at that moment only an
        #                       explicit removal can explain it being gone.
        #   ran far away     -> it decays by itself (the tick cannot even reach it
        #                       any more - the safety net for runaways)
        #
        # The effect's NBT key was renamed in a later Minecraft version, so both
        # spellings are probed AND checked; the analysis accepts either.
        #
        # Runs on the SHIPPED build: the marking is in the every-tick path.
        $hasCaps = 'execute if entity @e[tag=t,nbt={ActiveEffects:[{Id:"fulu:infighting"}]}] run say [verify] MARKED-CAPS'
        $hasLower = 'execute if entity @e[tag=t,nbt={active_effects:[{Id:"fulu:infighting"}]}] run say [verify] MARKED-LOWER'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'gamerule doMobSpawning false', `
                   'gamerule doDaylightCycle false', `
                   'gamerule doWeatherCycle false', `
                   'time set midnight', `
                   'weather clear', `
                   'scoreboard objectives add fuFlag dummy', `
                   'setblock 0 -60 0 fulu:formation_eye', `
                   'say [verify] phase-PLACED', `
                   'schedule function verify:x1 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\x1.mcfunction") `
            -Value '# PROBE: what does "has this effect" look like in 1.20.1 + Forge?',
                   '#   - vanilla effects store a NUMBER in ActiveEffects[0].Id',
                   '#   - a MODDED effect does not: reading that path comes back 0, so an',
                   '#     nbt selector cannot filter modded effects by id here',
                   '#   - Duration / Amplifier read fine, so the checks below use Duration',
                   '#     as "is an effect active on this mob" (in this test the formation',
                   '#     is the only thing handing out effects, so it can only be ours)',
                   'summon minecraft:zombie 2 -60 0 {PersistenceRequired:1b,Tags:["probe"]}', `
                   'effect give @e[tag=probe] fulu:infighting 10 3', `
                   'execute if data entity @e[tag=probe,limit=1] ActiveEffects[0].Id run say [verify] HAS-BYTE-ID', `
                   'execute if data entity @e[tag=probe,limit=1] ActiveEffects[{Id:"fulu:infighting"}] run say [verify] STRING-ID-MATCHES', `
                   'execute if data entity @e[tag=probe,limit=1] ActiveEffects[{"forge:id":"fulu:infighting"}] run say [verify] KEY-forge-id', `
                   'execute if data entity @e[tag=probe,limit=1] ActiveEffects[{id:"fulu:infighting"}] run say [verify] KEY-lower-id', `
                   'execute if data entity @e[tag=probe,limit=1] ActiveEffects[0].Duration run say [verify] SLOT0-DURATION', `
                   'execute if data entity @e[tag=probe,limit=1] ActiveEffects[0].ShowIcon run say [verify] SLOT0-SHOWICON', `
                   'execute store result score #dur fuFlag run data get entity @e[tag=probe,limit=1] ActiveEffects[0].Duration', `
                   'execute store result score #amp fuFlag run data get entity @e[tag=probe,limit=1] ActiveEffects[0].Amplifier', `
                   'execute if score #dur fuFlag matches 1.. run say [verify] EFFECT-REGISTERED', `
                   'execute if score #amp fuFlag matches 3 run say [verify] AMPLIFIER-READS-BACK', `
                   'execute store result score #want fuFlag run data get entity @e[tag=probe,limit=1] ActiveEffects[0].Duration', `
                   'kill @e[tag=probe]', `
                   'say [verify] phase-PROBE', `
                   'schedule function verify:x2 2s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\x2.mcfunction") `
            -Value '# a fresh monster inside the formation: the tick has to mark it', `
                   'summon minecraft:zombie 2 -60 0 {PersistenceRequired:1b,Tags:["t"]}', `
                   'say [verify] phase-SPAWNED', `
                   'schedule function verify:x3 2s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\x3.mcfunction") `
            -Value '# inside the formation the marker is INFINITE, so the duration reads -1', `
                   'execute store result score #d fuFlag run data get entity @e[tag=t,limit=1] ActiveEffects[0].Duration', `
                   'execute if score #d fuFlag matches ..-1 run say [verify] MARKED-INSIDE', `
                   'execute if score #d fuFlag matches 0.. run say [verify] NOT-MARKED-INSIDE', `
                   'execute if data entity @e[tag=t,limit=1] ActiveEffects run say [verify] CARRIES-SOMETHING', `
                   '# and it really is OUR effect, not just "some effect" - Forge keeps a',
                   '# modded effect''s identity in the string key forge:id', `
                   'execute if entity @e[tag=t,nbt={ActiveEffects:[{"forge:id":"fulu:infighting"}]}] run say [verify] ID-MATCH-INSIDE', `
                   'say [verify] phase-MARKED', `
                   'schedule function verify:x4 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\x4.mcfunction") `
            -Value '# step just outside the formation, but well inside the clear margin.',
                   '# x=13 because the horizontal limit is +-10 now (step 11 part 1).', `
                   'tp @e[tag=t] 13 -60 0', `
                   'say [verify] phase-STEPPED-OUT', `
                   'schedule function verify:x5 10t'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\x5.mcfunction") `
            -Value '# 0.5 s after stepping out the rule says: a 20 s countdown, not "infinite"',
                   '# any more. (It used to be deleted outright - the rule changed in step',
                   '# 10 part 10: leaving hands you 20 s, it does not switch off instantly.)', `
                   'execute store result score #d fuFlag run data get entity @e[tag=t,limit=1] ActiveEffects[0].Duration', `
                   'execute if score #d fuFlag matches 1..400 run say [verify] NEAR-20S', `
                   'execute if score #d fuFlag matches ..-1 run say [verify] NEAR-STILL-INFINITE', `
                   'execute if score #d fuFlag matches 0 run say [verify] NEAR-GONE', `
                   'say [verify] phase-NEAR-CHECK', `
                   'schedule function verify:x6 4s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\x6.mcfunction") `
            -Value '# back inside, so the tick marks it infinite again', `
                   'tp @e[tag=t] 2 -60 0', `
                   'say [verify] phase-BACK-IN', `
                   'schedule function verify:x7 2s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\x7.mcfunction") `
            -Value 'execute store result score #d fuFlag run data get entity @e[tag=t,limit=1] ActiveEffects[0].Duration', `
                   'execute if score #d fuFlag matches ..-1 run say [verify] MARKED-AGAIN', `
                   '# step out again - and this time walk it, THEN run far. A short hop is',
                   '# all the margin query needs; a TELEPORT straight past the +16 margin',
                   '# would skip the conversion and leave the infinite marker on the mob', `
                   'tp @e[tag=t] 13 -60 0', `
                   'say [verify] phase-OUT-AGAIN', `
                   'schedule function verify:x8 10t'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\x8.mcfunction") `
            -Value 'execute store result score #d fuFlag run data get entity @e[tag=t,limit=1] ActiveEffects[0].Duration', `
                   'execute if score #d fuFlag matches 1..400 run say [verify] AGAIN-20S', `
                   'execute if score #d fuFlag matches ..-1 run say [verify] AGAIN-STILL-INFINITE', `
                   '# now run far away: nothing refreshes it any more, so it only counts down', `
                   'tp @e[tag=t] 40 -60 0', `
                   'say [verify] phase-FAR', `
                   'schedule function verify:x9 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\x9.mcfunction") `
            -Value 'execute store result score #d fuFlag run data get entity @e[tag=t,limit=1] ActiveEffects[0].Duration', `
                   'execute if score #d fuFlag matches 1..399 run say [verify] FAR-COUNTING-DOWN', `
                   'execute if score #d fuFlag matches 400 run say [verify] FAR-STILL-400', `
                   'execute if score #d fuFlag matches ..-1 run say [verify] FAR-INFINITE-AGAIN', `
                   'say [verify] phase-CHECK'
        Write-Host " [setup] datapack (effect test) -> $dp"
        Write-Host " [setup] expecting: MARKED-INSIDE, CLEARED-NEAR after 0.5 s, CLEARED-FAR by itself"
    } elseif ($RestartTest) {
        # ---------------------------------------------------- restart test --
        # "The state is permanent" cannot be read off the source: saveAdditional
        # and load can both exist and still be wired wrong. The honest test is to
        # close the game and open it again - which is what the two phases do (two
        # separate server processes, one world):
        #
        #   PHASE 1 (wipe):  place the eye, fit the talisman, force the chunk to
        #                    unload (which writes it to disk), then stop the server.
        #   PHASE 2 (keep):  a brand new process loads that chunk back and asks:
        #                    - is the nbt flag still there?
        #                    - is the blockstate (the "with talisman" LOOK) still there?
        #                    - does the formation work right away, with nobody
        #                      having switched anything on?
        #
        # Both phases run on the SHIPPED build - everything is checked with
        # /data get, /execute if block and mob health.
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\beat.mcfunction") `
            -Value 'say [verify] heartbeat', 'schedule function verify:beat 5s'
        if ($Phase -eq "1") {
            Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
                -Value 'schedule function verify:place 3s', `
                       'schedule function verify:beat 5s'
            Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
                -Value 'forceload add 0 0', `
                       'gamerule doMobSpawning false', `
                       'gamerule doDaylightCycle false', `
                       'gamerule doWeatherCycle false', `
                       'time set midnight', `
                       'weather clear', `
                       'scoreboard objectives add fuFlag dummy', `
                       'scoreboard objectives add fuHp dummy', `
                       'fill -3 -60 -3 3 -59 -3 minecraft:stone', `
                       'fill -3 -60 3 3 -59 3 minecraft:stone', `
                       'fill -3 -60 -3 -3 -59 3 minecraft:stone', `
                       'fill 3 -60 -3 3 -59 3 minecraft:stone', `
                       'setblock 0 -60 0 fulu:formation_eye', `
                       'say [verify] phase-PLACED', `
                       'schedule function verify:r2 3s'
            Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\r2.mcfunction") `
                -Value '# fit the talisman (this goes through load(), then setChanged())', `
                       'data merge block 0 -60 0 {Talisman:"chaos"}', `
                       '# ALSO set the block state: that is the "look", and the real',
                       '# interaction does both. setblock keeps the block entity (same block).',
                       'setblock 0 -60 0 fulu:formation_eye[talisman=chaos]', `
                       'execute if data block 0 -60 0 {Talisman:"chaos"} run say [verify] SET-OK', `
                       'execute unless data block 0 -60 0 {Talisman:"chaos"} run say [verify] SET-FAILED', `
                       'say [verify] phase-SET', `
                       'schedule function verify:r3 3s'
            Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\r3.mcfunction") `
                -Value '# drop the chunk: it is written to disk, then unloaded. The server is', `
                       '# killed soon after, so the DISK is all phase 2 can read from.', `
                       'forceload remove 0 0', `
                       'say [verify] phase-UNLOAD'
            Write-Host " [setup] datapack (restart test, phase 1: fit it and save it) -> $dp"
        } else {
            Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
                -Value 'schedule function verify:q1 3s', `
                       'schedule function verify:beat 5s', `
                       '# NOTE: the scoreboard is NOT part of a chunk save - it lives in the',
                       '# world data and is only written on a full world save. These runs end',
                       '# with a hard kill, so phase 1''s objectives are gone here and phase 2',
                       '# must create its own. (That is why the nbt check below uses',
                       '# "execute if data block" instead of storing a score.)',
                       'scoreboard objectives add fuHp dummy'
            Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\q1.mcfunction") `
                -Value '# load the chunk back from disk: this is where load() runs', `
                       'forceload add 0 0', `
                       'say [verify] phase-RELOADED', `
                       'schedule function verify:q2 2s'
            Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\q2.mcfunction") `
                -Value '# nobody is connected - that is the point (see the note at the end)', `
                       'execute unless entity @a run say [verify] NO-PLAYERS', `
                       '# "execute if data" needs no scoreboard, so it still works after a kill', `
                       'execute if data block 0 -60 0 {Talisman:"chaos"} run say [verify] NBT-SURVIVED', `
                       'execute unless data block 0 -60 0 {Talisman:"chaos"} run say [verify] NBT-LOST', `
                       'execute if block 0 -60 0 fulu:formation_eye[talisman=chaos] run say [verify] LOOK-SURVIVED', `
                       'execute unless block 0 -60 0 fulu:formation_eye[talisman=chaos] run say [verify] LOOK-LOST', `
                       'say [verify] phase-RESTORED', `
                       'schedule function verify:q3 3s'
            Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\q3.mcfunction") `
                -Value '# and it must WORK, with nobody having touched anything', `
                       'summon minecraft:zombie 2 -60 0 {PersistenceRequired:1b,Tags:["t"]}', `
                       'summon minecraft:zombie -2 -60 0 {PersistenceRequired:1b,Tags:["t"]}', `
                       'summon minecraft:zombie 0 -60 2 {PersistenceRequired:1b,Tags:["t"]}', `
                       'say [verify] phase-SPAWNED', `
                       'schedule function verify:q4 16s'
            Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\q4.mcfunction") `
                -Value 'execute as @e[type=minecraft:zombie] store result score @s fuHp run data get entity @s Health', `
                       'execute as @e[type=minecraft:zombie] if score @s fuHp matches ..19 run say [verify] FIGHTING', `
                       'execute if entity @e[type=minecraft:zombie] run say [verify] SOME-ALIVE', `
                       'execute unless entity @e[type=minecraft:zombie] run say [verify] ALL-DEAD', `
                       'say [verify] phase-CHECK'
            Write-Host " [setup] datapack (restart test, phase 2: read it back and use it) -> $dp"
        }
    } elseif ($DisplayTest) {
        # ---------------------------------------------------- display test --
        # Scanning runs every tick; the announce and the particles walk a 5 s
        # beat. To make that measurable, the world changes the count EVERY
        # SECOND: one zombie arrives per second, for the whole run.
        #
        #   - if the announce were still "fire whenever the count changed",
        #     it would speak ~20 times in a 20 s window;
        #   - with the 5 s beat it must speak ~4 times (once per beat), because
        #     the count differs from the last announced one at every beat.
        #
        # Needs a temporary build with DEBUG_SCAN_LOG = true.
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'gamerule doMobSpawning false', `
                   'gamerule doDaylightCycle false', `
                   'gamerule doWeatherCycle false', `
                   'time set midnight', `
                   'weather clear', `
                   'fill -3 -60 -3 3 -59 -3 minecraft:stone', `
                   'fill -3 -60 3 3 -59 3 minecraft:stone', `
                   'fill -3 -60 -3 -3 -59 3 minecraft:stone', `
                   'fill 3 -60 -3 3 -59 3 minecraft:stone', `
                   'setblock 0 -60 0 fulu:formation_eye', `
                   'say [verify] phase-PLACED', `
                   'schedule function verify:dt2 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\dt2.mcfunction") `
            -Value '# from here the count grows by one every second', `
                   'say [verify] phase-RISING', `
                   'schedule function verify:drip 1s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\drip.mcfunction") `
            -Value 'summon minecraft:zombie 2 -60 0 {PersistenceRequired:1b,Tags:["d"]}', `
                   'schedule function verify:drip 1s'
        Write-Host " [setup] datapack (display test) -> $dp"
        Write-Host " [setup] one zombie per second; expecting ~4 announces in 20 s, not ~20"
    } elseif ($TalismanTest) {
        # --------------------------------------------------- talisman test --
        # The tick is now two segments:
        #   segment 1 (always)      scan + announce, only when the count changes
        #   segment 2 (only w/ ?)  make every monster pick a target
        # So: put three zombies in range WITHOUT a talisman and check that the
        # scan still speaks while nobody gets hurt; then fit the talisman
        # through /data merge and check that they start hitting each other.
        #
        # Health is read through a scoreboard (20 = untouched), so "did they
        # fight" is answered by real damage, not by "setTarget was called".
        #
        # The pen: mobs with AI wander (and without AI they cannot swing at all).
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'gamerule doMobSpawning false', `
                   'gamerule doDaylightCycle false', `
                   'gamerule doWeatherCycle false', `
                   'time set midnight', `
                   'weather clear', `
                   'scoreboard objectives add fuHp dummy', `
                   'fill -3 -60 -3 3 -59 -3 minecraft:stone', `
                   'fill -3 -60 3 3 -59 3 minecraft:stone', `
                   'fill -3 -60 -3 -3 -59 3 minecraft:stone', `
                   'fill 3 -60 -3 3 -59 3 minecraft:stone', `
                   'setblock 0 -60 0 fulu:formation_eye', `
                   'say [verify] phase-PLACED', `
                   'schedule function verify:tt2 5s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\tt2.mcfunction") `
            -Value '# three monsters, no talisman fitted', `
                   'summon minecraft:zombie 2 -60 0 {PersistenceRequired:1b,Tags:["t"]}', `
                   'summon minecraft:zombie -2 -60 0 {PersistenceRequired:1b,Tags:["t"]}', `
                   'summon minecraft:zombie 0 -60 2 {PersistenceRequired:1b,Tags:["t"]}', `
                   'say [verify] phase-NO-TALISMAN', `
                   'schedule function verify:tt3 14s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\tt3.mcfunction") `
            -Value 'execute as @e[type=minecraft:zombie] store result score @s fuHp run data get entity @s Health', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 20 run say [verify] FULL-HP', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches ..19 run say [verify] HURT', `
                   'say [verify] phase-NO-TALISMAN-DONE', `
                   'schedule function verify:tt4 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\tt4.mcfunction") `
            -Value '# fit the talisman (the tick reads the nbt field, so a merge is enough)', `
                   'data merge block 0 -60 0 {Talisman:"chaos"}', `
                   'say [verify] phase-WITH-TALISMAN', `
                   'schedule function verify:tt5 16s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\tt5.mcfunction") `
            -Value 'execute as @e[type=minecraft:zombie] store result score @s fuHp run data get entity @s Health', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 20 run say [verify] FULL-HP', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches ..19 run say [verify] HURT', `
                   'say [verify] phase-WITH-TALISMAN-DONE'
        Write-Host " [setup] datapack (talisman test) -> $dp"
        Write-Host " [setup] expecting: the scan speaks but nobody is hurt, then they fight"
    } elseif ($StateTest) {
        # ------------------------------------------------- blockstate test --
        # The second look is chosen by the blockstate property talisman.
        # A headless server cannot render it, but it CAN answer two questions:
        #   - does the property exist, with the right default?
        #   - can the state actually be switched (i.e. is it registered in the
        #     state definition, and can the blockstate json pick it up)?
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'scoreboard objectives add fuFlag dummy', `
                   'setblock 0 -60 0 fulu:formation_eye', `
                   'say [verify] phase-PLACED', `
                   'schedule function verify:st2 4s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\st2.mcfunction") `
            -Value '# a freshly placed eye must default to "no talisman"', `
                   'execute if block 0 -60 0 fulu:formation_eye[talisman=empty] run say [verify] DEFAULT-OFF-OK', `
                   'execute unless block 0 -60 0 fulu:formation_eye[talisman=empty] run say [verify] DEFAULT-OFF-BAD', `
                   '# same block, other state: proves the property is in the state definition', `
                   'setblock 0 -60 0 fulu:formation_eye[talisman=chaos]', `
                   'say [verify] phase-SET-TRUE', `
                   'schedule function verify:st3 4s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\st3.mcfunction") `
            -Value 'execute if block 0 -60 0 fulu:formation_eye[talisman=chaos] run say [verify] SWITCH-ON-OK', `
                   'execute unless block 0 -60 0 fulu:formation_eye[talisman=chaos] run say [verify] SWITCH-ON-BAD', `
                   '# setblock keeps the block entity (same block, different state), so the',
                   '# nbt flag readable through /data is still there and still false',
                   'scoreboard players set #f fuFlag 0', `
                   'execute if data block 0 -60 0 {Talisman:"chaos"} run scoreboard players set #f fuFlag 1', `
                   'execute if score #f fuFlag matches 0 run say [verify] BE-KEPT-OK', `
                   'execute if score #f fuFlag matches 1 run say [verify] BE-KEPT-UNEXPECTED', `
                   '# is ??? a real registered item? summon a drop carrying it and match',
                   '# it back by nbt - an id that does not exist would come back as air',
                   'summon minecraft:item 0 -55 0 {Item:{id:"fulu:chaos_talisman",Count:1b},Tags:["ftest"]}', `
                   'execute if entity @e[tag=ftest,nbt={Item:{id:"fulu:chaos_talisman"}}] run say [verify] ITEM-OK', `
                   'execute unless entity @e[tag=ftest,nbt={Item:{id:"fulu:chaos_talisman"}}] run say [verify] ITEM-MISSING', `
                   'kill @e[tag=ftest]', `
                   'say [verify] phase-CHECK'
        Write-Host " [setup] datapack (blockstate test) -> $dp"
        Write-Host " [setup] expecting DEFAULT-OFF-OK, then SWITCH-ON-OK"
    } elseif ($PersistTest) {
        # ------------------------------------------------- persistence test --
        # The "has talisman" flag is saved to NBT. Two ways to be wrong:
        #   - saved but never read back -> lost after a reload
        #   - read but never saved      -> never survives anything
        # So: set it, force the chunk to unload (that writes the block entity to
        # disk), reload the chunk, then read the flag again.
        #
        # 'data merge block' is used to set it because there is no in-game way to
        # put a talisman in yet - and it doubles as a test of load(), since the
        # merge goes through BlockEntity.load().
        #
        # This mode does NOT need the temporary build: persistence has nothing to
        # do with the on/off switch, so it runs against the shipped jar.
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'scoreboard objectives add fuFlag dummy', `
                   'setblock 0 -60 0 fulu:formation_eye', `
                   'say [verify] phase-PLACED', `
                   'schedule function verify:pt2 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\pt2.mcfunction") `
            -Value '# set the flag through NBT (this also exercises load())', `
                   'data merge block 0 -60 0 {Talisman:"chaos"}', `
                   'scoreboard players set #f fuFlag 0', `
                   'execute if data block 0 -60 0 {Talisman:"chaos"} run scoreboard players set #f fuFlag 1', `
                   'execute if score #f fuFlag matches 1 run say [verify] SET-OK', `
                   'execute if score #f fuFlag matches 0 run say [verify] SET-FAILED', `
                   'say [verify] phase-SET', `
                   'schedule function verify:pt3 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\pt3.mcfunction") `
            -Value '# drop the chunk: this saves the block entity, then unloads it.', `
                   '# The wait must be long enough for the unload queue to get to it -', `
                   '# 10 s was NOT enough, and the "it survived" check then passed for the', `
                   '# wrong reason (the block entity had never left memory at all).', `
                   'forceload remove 0 0', `
                   'say [verify] phase-UNLOAD', `
                   'schedule function verify:pt4 25s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\pt4.mcfunction") `
            -Value '# load the chunk again: the block entity is rebuilt by load()', `
                   'forceload add 0 0', `
                   'say [verify] phase-RELOAD', `
                   'schedule function verify:pt5 5s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\pt5.mcfunction") `
            'scoreboard players set #f fuFlag 0', `
            -Value 'execute if data block 0 -60 0 {Talisman:"chaos"} run scoreboard players set #f fuFlag 1', `
                   'execute if score #f fuFlag matches 1 run say [verify] PERSIST-OK', `
                   'execute if score #f fuFlag matches 0 run say [verify] PERSIST-LOST', `
                   'say [verify] phase-CHECK'
        Write-Host " [setup] datapack (persistence test) -> $dp"
        Write-Host " [setup] expecting SET-OK, then PERSIST-OK after a chunk unload + reload"
    } elseif ($FightTest) {
        # ---------------------------------------------------- fight test --
        # Question: does the block really make two monsters hurt each other,
        # and does it leave a lone monster alone?
        #
        #   PLACED      (t=3)   place the eye
        #   ALONE       (t=12)  summon ONE zombie    -> it must stay at 20 HP
        #   ALONE-CHECK (t=21)  read every zombie's Health via a scoreboard
        #   PAIR        (t=24)  summon a SECOND one  -> they must fight
        #   PAIR-CHECK  (t=38)  read Health again -> at least one below 20
        #
        # Health is read with 'execute store result score @s fuHp run data get
        # entity @s Health' (20.0 for an untouched zombie), so this checks real
        # damage rather than "setTarget was called".
        #
        # NOTE: the test mobs deliberately have AI (no NoAI) - a mob with NoAI
        # gets its target cleared every tick and would never swing. But a mob
        # WITH AI also wanders, and the first version of this test lost both
        # zombies out of the +-5 scan range that way (the count fell to 0 and
        # nobody ever fought). Hence the 7x7 pen: 2 blocks high, so they cannot
        # climb out, while the eye stays comfortably inside the scan range.
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value 'forceload add 0 0', `
                   'gamerule doMobSpawning false', `
                   'gamerule doDaylightCycle false', `
                   'gamerule doWeatherCycle false', `
                   'time set midnight', `
                   'weather clear', `
                   'scoreboard objectives add fuHp dummy', `
                   '# 7x7 pen, 2 blocks high: the test mobs cannot wander out of range', `
                   'fill -3 -60 -3 3 -59 -3 minecraft:stone', `
                   'fill -3 -60 3 3 -59 3 minecraft:stone', `
                   'fill -3 -60 -3 -3 -59 3 minecraft:stone', `
                   'fill 3 -60 -3 3 -59 3 minecraft:stone', `
                   'setblock 0 -60 0 fulu:formation_eye', `
                   '# the fighting segment only runs WITH a talisman (step 10 part 4),',
                   '# so fit one here - otherwise the ALONE phase below would pass for',
                   '# the wrong reason (nothing fights without a talisman anyway)',
                   'data merge block 0 -60 0 {Talisman:"chaos"}', `
                   'say [verify] phase-PLACED', `
                   'schedule function verify:f2 9s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\f2.mcfunction") `
            -Value '# one zombie: nothing to fight', `
                   'summon minecraft:zombie 2 -60 0 {PersistenceRequired:1b,Tags:["fight"]}', `
                   'say [verify] phase-ALONE', `
                   'schedule function verify:f3 9s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\f3.mcfunction") `
            -Value 'execute as @e[type=minecraft:zombie] store result score @s fuHp run data get entity @s Health', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches 20 run say [verify] ALONE-UNTOUCHED', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches ..19 run say [verify] ALONE-HURT', `
                   'say [verify] phase-ALONE-CHECK', `
                   'schedule function verify:f4 3s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\f4.mcfunction") `
            -Value '# now THREE zombies: every single one must get a target each round', `
                   'summon minecraft:zombie -2 -60 0 {PersistenceRequired:1b,Tags:["fight"]}', `
                   'summon minecraft:zombie 0 -60 2 {PersistenceRequired:1b,Tags:["fight"]}', `
                   'say [verify] phase-PAIR', `
                   'schedule function verify:f5 14s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\f5.mcfunction") `
            -Value 'execute as @e[type=minecraft:zombie] store result score @s fuHp run data get entity @s Health', `
                   'execute as @e[type=minecraft:zombie] if score @s fuHp matches ..19 run say [verify] FIGHTING', `
                   'say [verify] phase-PAIR-CHECK'
        Write-Host " [setup] datapack (fight test) -> $dp"
        Write-Host " [setup] expecting ALONE-UNTOUCHED first, then FIGHTING once there are two"
    } elseif ($MobTest) {
        # ---------------------------------------------------- mob scan test --
        # Phases, each announced with a 'say' marker so the log can be split.
        # Times are relative to datapack load; a scan happens every 5 s.
        #
        # This timeline checks the SHAPE of the new scan box:
        #   horizontal +-5, and vertically "10 blocks below ... up to the sky".
        #
        #   ZERO    (t=3)   nothing summoned                     -> speaks 0
        #   THREE   (t=15)  3 zombies at eye level (in range)    -> speaks 3
        #   DOWN10  (t=21)  +1 zombie exactly 10 below           -> speaks 4
        #   DOWN11  (t=27)  +1 zombie 11 below                   -> SILENT (not counted)
        #   HIGH    (t=33)  +1 zombie 100 above                  -> speaks 5
        #   X10     (t=39)  +1 zombie exactly 10 to the side     -> speaks 6
        #   X11     (t=45)  +1 zombie 11 to the side             -> SILENT (not counted)
        #   CLEAR   (t=51)  kill them all                        -> speaks 0
        #
        # The horizontal limit became +-10 in step 11 part 1 (it used to be +-5, and
        # the old phase summoned at x=7 expecting silence - which the new radius
        # turned into a counted monster, so the phases now mirror DOWN10/DOWN11).
        #
        # The SILENT phases are assertions: their allow-list of numbers is EMPTY,
        # so if an out-of-range zombie were counted the phase would show a number
        # and the test would fail.
        #
        # The eye is placed at y=100 (in the air) so that "11 blocks below" is in
        # open air instead of inside bedrock, and every test mob gets NoGravity
        # so it stays exactly where it was summoned. NoAI stops it wandering.
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\place.mcfunction") `
            -Value '# keep the chunk entity-ticking, otherwise the block falls asleep', `
                   'forceload add 0 0', `
                   'gamerule doMobSpawning false', `
                   'gamerule doDaylightCycle false', `
                   'gamerule doWeatherCycle false', `
                   'time set midnight', `
                   'weather clear', `
                   '# high up in the air: 11 blocks below is still open sky', `
                   'setblock 0 100 0 fulu:formation_eye', `
                   'say [verify] phase-ZERO', `
                   'schedule function verify:p2 12s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\p2.mcfunction") `
            -Value 'summon minecraft:zombie 2 100 2 {NoAI:1b,NoGravity:1b,Tags:["scan"]}', `
                   'summon minecraft:zombie 3 100 1 {NoAI:1b,NoGravity:1b,Tags:["scan"]}', `
                   'summon minecraft:zombie 1 100 3 {NoAI:1b,NoGravity:1b,Tags:["scan"]}', `
                   'say [verify] phase-THREE', `
                   'schedule function verify:p3 6s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\p3.mcfunction") `
            -Value '# exactly 10 below the eye: still inside', `
                   'summon minecraft:zombie 0 90 0 {NoAI:1b,NoGravity:1b,Tags:["scan"]}', `
                   'say [verify] phase-DOWN10', `
                   'schedule function verify:p4 6s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\p4.mcfunction") `
            -Value '# 11 below: outside, must NOT count', `
                   'summon minecraft:zombie 0 89 0 {NoAI:1b,NoGravity:1b,Tags:["scan"]}', `
                   'say [verify] phase-DOWN11', `
                   'schedule function verify:p5 6s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\p5.mcfunction") `
            -Value '# 100 above: upward is unlimited, must count', `
                   'summon minecraft:zombie 0 200 0 {NoAI:1b,NoGravity:1b,Tags:["scan"]}', `
                   'say [verify] phase-HIGH', `
                   'schedule function verify:p6 6s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\p6.mcfunction") `
            -Value '# exactly 10 to the side: still inside (the horizontal limit is +-10 since',
                   '# step 11 part 1). This phase used to summon at x=7 and expect silence,',
                   '# which the new radius turned into a counted monster - the test had to',
                   '# follow the spec, not the other way round.', `
                   'summon minecraft:zombie 10 100 0 {NoAI:1b,NoGravity:1b,Tags:["scan"]}', `
                   'say [verify] phase-X10', `
                   'schedule function verify:p7 6s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\p7.mcfunction") `
            -Value '# 11 to the side: outside, must NOT count', `
                   'summon minecraft:zombie 11 100 0 {NoAI:1b,NoGravity:1b,Tags:["scan"]}', `
                   'say [verify] phase-X11', `
                   'schedule function verify:p8 6s'
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\p8.mcfunction") `
            -Value 'kill @e[type=minecraft:zombie]', `
                   'say [verify] phase-CLEAR'
        # phase functions must exist at load time, so schedule them from start
        Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\start.mcfunction") `
            -Value 'schedule function verify:place 3s'
        Write-Host " [setup] datapack (mob scan test) -> $dp"
        Write-Host " [setup] shape: horizontal +-5, vertical: -10 ... +infinity"
    } else {
    # smoke test: if fulu:spirit_mote were not registered, THIS function would fail
    # to load and Minecraft would log "Failed to load function".
    Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\mote.mcfunction") `
        -Value '# the particle id must exist, otherwise this function fails to load', `
               'particle fulu:spirit_mote 0 -59 0 0 0 0 0 1'
    # An INDEPENDENT 5-second clock that has nothing to do with our mod. It tells
    # us how long the server really ran, so "the log ends early" can be told
    # apart from "the block stopped ticking".
    Set-Content -Encoding ASCII -Path (Join-Path $dp "data\verify\functions\beat.mcfunction") `
        -Value 'say [verify] heartbeat', 'schedule function verify:beat 5s'
    Write-Host " [setup] datapack -> $dp"
    }

    # ---------------------------------------------------------- 2. server --
    # (a) throw away the previous run's log so "Done (" cannot be a false hit
    Remove-Item -LiteralPath $log -Force -ErrorAction SilentlyContinue

    $launchTime = Get-Date
    Write-Host " [run]   starting the dedicated server..."
    $buildScript = Join-Path $root "tools\build.ps1"
    $proc = Start-Process -FilePath "powershell" -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput $serverOut -RedirectStandardError $serverErr `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $buildScript, "-Task", "runServer")

    $bootDeadline = (Get-Date).AddSeconds(300)
    $booted = $false
    while ((Get-Date) -lt $bootDeadline) {
        Start-Sleep -Seconds 2
        if ($proc.HasExited) { break }
        if ((Test-Path $log) -and ((Get-Item $log).LastWriteTime -gt $launchTime)) {
            if (Get-Content $log -Encoding UTF8 -ErrorAction SilentlyContinue |
                Select-String -SimpleMatch "Done (" -Quiet) { $booted = $true; break }
        }
    }

    if (-not $booted) {
        Write-Host " [FAIL]  the server never reached 'Done'." -ForegroundColor Red
        if (Test-Path $log) { Get-Content $log -Encoding UTF8 -Tail 25 | ForEach-Object { Write-Host "    $_" } }
        else { Write-Host "        no log at $log - see $serverOut" }
        $exitCode = 1
    } else {
        Write-Host " [run]   server is up; letting the block tick for $Seconds s..."
        Start-Sleep -Seconds $Seconds

        Write-Host " [stop]  stopping the server..."
        Stop-TestServer $proc $launchTime

        # ------------------------------------------------------- 3. check --
        Write-Head "result"

        $lines = @(Get-Content $log -Encoding UTF8 | Select-String -SimpleMatch $TAG)
        $beats = @(Get-Content $log -Encoding UTF8 | Select-String -SimpleMatch "[verify] heartbeat")
        $count = $lines.Count
        $loadErrors = @(Get-Content $log -Encoding UTF8 | Select-String -SimpleMatch "Failed to load function")

        if ($BloodPoolTest) {
            # ------------------------------------------------- blood analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            Write-Host " log        : $log"
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            function Count($marker) {
                return @($all | Select-String -SimpleMatch "[verify] $marker").Count
            }
            if ((@($all | Select-String -SimpleMatch "Failed to load function")).Count -eq 0) {
                Say-Check $true "no datapack function failed to load" "the commands actually ran"
            } else {
                $allGood = $false
                Say-Check $false "no datapack function failed to load" "a function did not parse"
            }
            if ((Count "BLOOD-TOO-EARLY") -eq 0) {
                Say-Check $true "no pool while the animal is alive" "nothing had died yet"
            } else {
                $allGood = $false
                Say-Check $false "no pool while the animal is alive" "a pool already existed"
            }
            if ((Count "BLOOD-NONE-AFTER-KILLED-COW") -ge 1 -and (Count "BLOOD-POOL-WITHOUT-PLAYER") -eq 0) {
                Say-Check $true "an animal killed without a player leaves nothing" "the cow was /kill-ed, no pool appeared"
            } else {
                $allGood = $false
                Say-Check $false "an animal killed without a player leaves nothing" "POOL=$(Count 'BLOOD-POOL-WITHOUT-PLAYER') NONE=$(Count 'BLOOD-NONE-AFTER-KILLED-COW')"
            }
            if ((Count "BLOOD-NONE-AFTER-ZOMBIE") -ge 1 -and (Count "BLOOD-FROM-NON-ANIMAL") -eq 0) {
                Say-Check $true "a non-animal leaves nothing" "the zombie died and no pool appeared"
            } else {
                $allGood = $false
                Say-Check $false "a non-animal leaves nothing" "POOL=$(Count 'BLOOD-FROM-NON-ANIMAL') NONE=$(Count 'BLOOD-NONE-AFTER-ZOMBIE')"
            }
            Write-Host ""
            $verdict = "FAIL"
            $why = "the blood pool did not appear the way it should"
            if ($allGood) {
                $verdict = "PASS"
                $why = "nothing appears without a player kill (the positive case needs a real player)"
            }
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($AntiGravityTest) {
            # --------------------------------------- anti-gravity analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            Write-Host " log        : $log"
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-52} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            function Count($marker) {
                return @($all | Select-String -SimpleMatch "[verify] $marker").Count
            }
            if ((@($all | Select-String -SimpleMatch "Failed to load function")).Count -eq 0) {
                Say-Check $true "no datapack function failed to load" "the commands actually ran"
            } else {
                $allGood = $false
                Say-Check $false "no datapack function failed to load" "a function did not load"
            }
            if ((Count "FLOAT-LIFTED") -gt 0) {
                Say-Check $true "the monster is lifted into the air" `
                    "its Y went above -58 (it started on the floor at -60)"
            } else {
                $allGood = $false
                Say-Check $false "the monster is lifted into the air" `
                    "LIFTED=$(Count 'FLOAT-LIFTED'), ONGROUND=$(Count 'FLOAT-ONGROUND')"
            }
            if ((Count "FLOAT-CAPPED") -gt 0 -and (Count "FLOAT-OVER") -eq 0) {
                Say-Check $true "it stops at the 5 block ceiling" `
                    "reached -55 and never went past -53"
            } else {
                $allGood = $false
                Say-Check $false "it stops at the 5 block ceiling" `
                    "CAPPED=$(Count 'FLOAT-CAPPED'), OVER=$(Count 'FLOAT-OVER')"
            }
            # ????????Duration = -1??????? ??
            if ((Count "FLOAT-INFINITE") -gt 0 -and (Count "FLOAT-TIMED") -eq 0) {
                Say-Check $true "inside the formation the effect is infinite" `
                    "Duration stayed -1 the whole time"
            } else {
                $allGood = $false
                Say-Check $false "inside the formation the effect is infinite" `
                    "INFINITE=$(Count 'FLOAT-INFINITE'), TIMED=$(Count 'FLOAT-TIMED')"
            }
            if ((Count "FROZEN") -gt 0 -and (Count "MOVED-FAR") -eq 0) {
                Say-Check $true "it cannot walk away" `
                    "X stayed inside 2..4 the whole time (it started at 3)"
            } else {
                $allGood = $false
                Say-Check $false "it cannot walk away" `
                    "FROZEN=$(Count 'FROZEN'), MOVED-FAR=$(Count 'MOVED-FAR')"
            }
            if ((Count "FLOAT-NOMARK") -eq 0) {
                Say-Check $true "the marker never lapses while it is inside" `
                    "every sample had the effect on"
            } else {
                $allGood = $false
                Say-Check $false "the marker never lapses while it is inside" `
                    "NOMARK=$(Count 'FLOAT-NOMARK') sample(s) without the effect"
            }
            if ((Count "FLOAT-EFFECT") -gt 0) {
                Say-Check $true "it carries the anti-gravity effect" "fulu:antigravity was on it"
            } else {
                $allGood = $false
                Say-Check $false "it carries the anti-gravity effect" "no FLOAT-EFFECT line"
            }
            $verdict = "FAIL"
            $why = "the anti-gravity formation does not mark and freeze monsters"
            if ($allGood) {
                $verdict = "PASS"
                $why = "lifted, capped at 5, infinite inside, frozen in place"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($EnemyScanTest) {
            # ------------------------------------------- enemy scan analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            Write-Host " log        : $log"
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-52} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            function Count($marker) {
                return @($all | Select-String -SimpleMatch "[verify] $marker").Count
            }

            if ((@($all | Select-String -SimpleMatch "Failed to load function")).Count -eq 0) {
                Say-Check $true "no datapack function failed to load" "the commands actually ran"
            } else {
                $allGood = $false
                Say-Check $false "no datapack function failed to load" "a function did not load"
            }
            # ??????? Enemy ?????? Monster ?? ????????
            foreach ($pair in @(@("slime", "SLIME"), @("hoglin", "HOGLIN"))) {
                $name = $pair[0]
                $tag = $pair[1]
                if ((Count "$tag-MARKED") -gt 0 -and (Count "$tag-MISSED") -eq 0) {
                    Say-Check $true "$name (Enemy but not Monster) is recognised" `
                        "$tag-MARKED seen, never $tag-MISSED"
                } else {
                    $allGood = $false
                    Say-Check $false "$name (Enemy but not Monster) is recognised" `
                        "MARKED=$(Count "$tag-MARKED"), MISSED=$(Count "$tag-MISSED")"
                }
            }
            # ??????????
            if ((Count "WOLF-UNMARKED") -gt 0 -and (Count "WOLF-MARKED") -eq 0) {
                Say-Check $true "a tamed wolf is left alone" "WOLF-UNMARKED, never WOLF-MARKED"
            } else {
                $allGood = $false
                Say-Check $false "a tamed wolf is left alone" `
                    "UNMARKED=$(Count 'WOLF-UNMARKED'), MARKED=$(Count 'WOLF-MARKED')"
            }
            # ??????
            if ((Count "VILLAGER-UNMARKED") -gt 0 -and (Count "VILLAGER-MARKED") -eq 0) {
                Say-Check $true "a villager is left alone" "VILLAGER-UNMARKED, never VILLAGER-MARKED"
            } else {
                $allGood = $false
                Say-Check $false "a villager is left alone" `
                    "UNMARKED=$(Count 'VILLAGER-UNMARKED'), MARKED=$(Count 'VILLAGER-MARKED')"
            }

            $verdict = "FAIL"
            $why = "the eye still cannot see some enemies"
            if ($allGood) {
                $verdict = "PASS"
                $why = "slimes and hoglins are seen; tamed wolves and villagers are not"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($EntityTest) {
            # ---------------------------------------------- entity analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            Write-Host " log        : $log"
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            function Count($marker) {
                return @($all | Select-String -SimpleMatch "[verify] $marker").Count
            }

            $loadErrors = @($all | Select-String -SimpleMatch "Failed to load function")
            if ($loadErrors.Count -eq 0) {
                Say-Check $true "no datapack function failed to load" "the commands actually ran"
            } else {
                $allGood = $false
                Say-Check $false "no datapack function failed to load" "$($loadErrors.Count) failed"
            }
            if ((Count "SWORD-ALIVE") -gt 0 -and (Count "SWORD-NOT-SUMMONED") -eq 0) {
                Say-Check $true "the entity can be summoned" "fulu:flying_sword exists in the world"
            } else {
                $allGood = $false
                Say-Check $false "the entity can be summoned" "summon did not produce the entity"
            }
            $drops = (Count "HP-15") + (Count "HP-10") + (Count "HP-5") + (Count "HP-LOW")
            if ($drops -gt 0) {
                Say-Check $true "it flew over and hit the monster on its own" `
                    "health dropped with no formation and no player involved"
            } else {
                $allGood = $false
                Say-Check $false "it flew over and hit the monster on its own" "the zombie stayed at full health"
            }
            if ((Count "HP-WRONG") -eq 0) {
                Say-Check $true "the hit takes exactly 5" "no health value between the 5-point steps"
            } else {
                $allGood = $false
                Say-Check $false "the hit takes exactly 5" "$(Count 'HP-WRONG') off-step sample(s)"
            }
            if ((Count "SWORD-GONE") -gt 0 -and (Count "SWORD-STILL-THERE") -eq 0) {
                Say-Check $true "it removed itself after hitting" "SWORD-GONE, never SWORD-STILL-THERE"
            } else {
                $allGood = $false
                Say-Check $false "it removed itself after hitting" `
                    "GONE=$(Count 'SWORD-GONE'), STILL-THERE=$(Count 'SWORD-STILL-THERE')"
            }
            # 第三部分：撞到方块就消失（墙后的僵尸一点血都不该掉）
            if ((Count "SWORD-BLOCKED-GONE") -gt 0 -and (Count "SWORD-WENT-THROUGH") -eq 0 `
                -and (Count "WALL-ZOMBIE-UNHURT") -gt 0 -and (Count "WALL-ZOMBIE-HURT") -eq 0) {
                Say-Check $true "a block in the way stops it" `
                    "the sword vanished at the wall and the monster behind it was untouched"
            } else {
                $allGood = $false
                Say-Check $false "a block in the way stops it" `
                    "GONE=$(Count 'SWORD-BLOCKED-GONE'), THROUGH=$(Count 'SWORD-WENT-THROUGH'), UNHURT=$(Count 'WALL-ZOMBIE-UNHURT')"
            }

            $verdict = "FAIL"
            $why = "the flying sword entity does not behave"
            if ($allGood) {
                $verdict = "PASS"
                $why = "summoned, sought its target, hit for 5, removed itself"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($SwordTest) {
            # ----------------------------------------------- sword analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            Write-Host " log        : $log"
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            function Count($marker) {
                return @($all | Select-String -SimpleMatch "[verify] $marker").Count
            }

            # ? ?????????????????????? ??
            #    ??????forge:id ????????? s2 ??????
            #    ???????????????"?????"?
            $loadErrors = @($all | Select-String -SimpleMatch "Failed to load function")
            if ($loadErrors.Count -eq 0) {
                Say-Check $true "no datapack function failed to load" "the commands actually ran"
            } else {
                $allGood = $false
                Say-Check $false "no datapack function failed to load" `
                    "$($loadErrors.Count) failed - later lines in them never ran"
            }

            # (a) no monsters -> nothing launched
            if ((Count "NO-VOLLEY-EMPTY") -gt 0 -and (Count "VOLLEY-EMPTY-WRONG") -eq 0) {
                Say-Check $true "with no monsters it launches nothing" `
                    "NO-VOLLEY-EMPTY, never VOLLEY-EMPTY-WRONG"
            } else {
                $allGood = $false
                Say-Check $false "with no monsters it launches nothing" `
                    "EMPTY=$(Count 'NO-VOLLEY-EMPTY'), WRONG=$(Count 'VOLLEY-EMPTY-WRONG')"
            }
            # (c) a volley is ten swords
            if ((Count "SWORDS-10") -gt 0) {
                Say-Check $true "a volley is ten swords" "ten flying swords existed at once"
            } else {
                $allGood = $false
                Say-Check $false "a volley is ten swords" `
                    "SWORDS-10 never seen (SWORDS-FEW=$(Count 'SWORDS-FEW'))"
            }
            # (b) three monsters -> 3/3/4. The two near ones take 3 swords (15 -> 5 hp)
            # and the far one takes 4 (20 -> dead), so both signatures must appear.
            $survivors = (Count "HP-5") + (Count "HP-15")
            if ($survivors -gt 0 -and (Count "LEFT-2") -gt 0) {
                Say-Check $true "three monsters split 3/3/4 (near ones 15, far one 20)" `
                    "a survivor was seen (3 swords) and LEFT-2 (the far one died from 4)"
            } else {
                $allGood = $false
                Say-Check $false "three monsters split 3/3/4 (near ones 15, far one 20)" `
                    "survivor samples=$(survivors), LEFT-2=$(Count 'LEFT-2')"
            }
            if ((Count "NO-MARK") -gt 0) {
                Say-Check $true "it does not put the infighting mark on them" "NO-MARK seen"
            } else {
                $allGood = $false
                Say-Check $false "it does not put the infighting mark on them" "NO-MARK=$(Count 'NO-MARK')"
            }
            # 每把剑各打 5 点：一只 100 血僵尸挨一轮齐射（10 把）应该正好剩 50。
            # 如果原版的无敌帧没被清掉，只有第一把生效，血量会停在 95。
            if ((Count "BIG-AFTER-ONE-VOLLEY") -gt 0 -and (Count "BIG-ONE-HIT-ONLY") -eq 0) {
                Say-Check $true "each sword deals its own 5 (ten swords = 50 off a 100 hp monster)" `
                    "the 100 hp monster went to exactly 50 after one volley"
            } else {
                $allGood = $false
                Say-Check $false "each sword deals its own 5 (ten swords = 50 off a 100 hp monster)" `
                    "at-50=$(Count 'BIG-AFTER-ONE-VOLLEY'), stuck-at-95=$(Count 'BIG-ONE-HIT-ONLY')"
            }
            # 这一步方块实体不再自己算飞行，而是放出**飞剑实体** ——
            # 所以这里查的是"世界里真的出现过 fulu:flying_sword"。
            if ((Count "ENTITY-SEEN") -gt 0) {
                Say-Check $true "the formation launches a flying sword entity" `
                    "an entity of type fulu:flying_sword was in the world"
            } else {
                $allGood = $false
                Say-Check $false "the formation launches a flying sword entity" "no entity seen"
            }

            $verdict = "FAIL"
            $why = "the flying sword does not do what it should"
            if ($allGood) {
                $verdict = "PASS"
                $why = "ten swords a second, shared over the monsters, no infighting mark"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($TwoTalismanTest) {
            # ------------------------------------------- two talismans ------
            $all = @(Get-Content $log -Encoding UTF8)
            Write-Host " log        : $log"
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            function Count($marker) {
                return @($all | Select-String -SimpleMatch "[verify] $marker").Count
            }

            $chaosHurt = (Count "CHAOS-HURT") + (Count "CHAOS-KILLED")
            if ($chaosHurt -gt 0) {
                Say-Check $true "with the chaos talisman they fight" `
                    "damage or deaths seen ($chaosHurt evidence line(s))"
            } else {
                $allGood = $false
                Say-Check $false "with the chaos talisman they fight" "they stayed at full health"
            }
            if ((Count "CHAOS-MARKED") -gt 0) {
                Say-Check $true "and the chaos talisman marks them" "CHAOS-MARKED seen"
            } else {
                $allGood = $false
                Say-Check $false "and the chaos talisman marks them" "no infighting mark on any zombie"
            }
            if ((Count "SWORD-NO-MARK") -gt 0 -and (Count "SWORD-MARKED") -eq 0) {
                Say-Check $true "the flying sword talisman does NOT mark them" `
                    "SWORD-NO-MARK seen, never SWORD-MARKED - the mark belongs to the other formation"
            } else {
                $allGood = $false
                Say-Check $false "the flying sword talisman does NOT mark them" `
                    "NO-MARK=$(Count 'SWORD-NO-MARK'), MARKED=$(Count 'SWORD-MARKED')"
            }
            if ((Count "SWORD-ALIVE") -gt 0) {
                Say-Check $true "and they were still there to be counted" `
                    "SWORD-ALIVE seen, so the result is not 'no mobs at all'"
            } else {
                $allGood = $false
                Say-Check $false "and they were still there to be counted" `
                    "no SWORD-ALIVE: the comparison is meaningless"
            }
            $loadErrors = @($all | Select-String -SimpleMatch "Failed to load function")
            if ($loadErrors.Count -eq 0) {
                Say-Check $true "no datapack function failed to load" "the commands actually ran"
            } else {
                $allGood = $false
                Say-Check $false "no datapack function failed to load" "$($loadErrors.Count) failed"
            }

            $verdict = "FAIL"
            $why = "the talismans do not behave differently"
            if ($allGood) {
                $verdict = "PASS"
                $why = "chaos: they fight and get marked; sword: swords damage them, no mark"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($CodexPersistTest) {
            # --------------------------------------- persistence analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            Write-Host " log        : $log"
            Write-Host " phase      : $Phase"
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            function Has($marker) {
                return @($all | Select-String -SimpleMatch "[verify] $marker").Count -gt 0
            }

            if ($Phase -eq "1") {
                if (Has "SET-OK") {
                    Say-Check $true "phase 1 unlocked the formation" "Tester = 3 (seen + mastered)"
                } else {
                    $allGood = $false
                    Say-Check $false "phase 1 unlocked the formation" "the score was not set"
                }
                if (-not $script:GracefulFailed) {
                    Say-Check $true "the world was written to disk on request" `
                        "rcon save-all flush was confirmed before the run ended"
                } else {
                    $allGood = $false
                    Say-Check $false "the world was written to disk on request" `
                        "no confirmed save - phase 2 cannot prove anything"
                }
            } else {
                if (Has "SCORE-SURVIVED" -and -not (Has "SCORE-LOST")) {
                    Say-Check $true "the unlock progress came back after a restart" `
                        "a new server process read Tester = 3 from disk"
                } else {
                    $allGood = $false
                    Say-Check $false "the unlock progress came back after a restart" `
                        "$(if ($script:GracefulFailed) { "unclear: the previous run had to be killed" } else { "the score was gone" })"
                }
            }

            $verdict = "FAIL"
            $why = "persistence did not hold"
            if ($allGood) {
                $verdict = "PASS"
                $why = if ($Phase -eq "1") { "unlocked and saved" } else { "still there in a brand new process" }
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($BlockTest) {
            # ----------------------------------------------- block analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            Write-Host " log        : $log"
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            function Has($marker) {
                return @($all | Select-String -SimpleMatch "[verify] $marker").Count -gt 0
            }

            if (Has "BASE-PLACED" -and -not (Has "BASE-NOT-PLACED")) {
                Say-Check $true "the formation base can stand in the world" "setblock + read back = fulu:formation_base"
            } else {
                $allGood = $false
                Say-Check $false "the formation base can stand in the world" "the block did not read back"
            }
            if (Has "BASE-DROPS-SELF" -and -not (Has "BASE-DROPS-WRONG")) {
                Say-Check $true "breaking it drops the block itself" `
                    "loot ran the real loot table and produced fulu:formation_base"
            } else {
                $allGood = $false
                Say-Check $false "breaking it drops the block itself" `
                    "no formation_base item appeared (a missing loot table looks exactly like this)"
            }
            if (Has "CINNABAR-EXISTS" -and -not (Has "CINNABAR-MISSING")) {
                Say-Check $true "the cinnabar item id resolves" "summon item fulu:cinnabar worked"
            } else {
                $allGood = $false
                Say-Check $false "the cinnabar item id resolves" "summon did not produce the item"
            }
            if (Has "BASE-REMOVED") {
                Say-Check $true "it can be taken away again" "the position reads as air afterwards"
            } else {
                $allGood = $false
                Say-Check $false "it can be taken away again" "the block is still there"
            }
            # ???????????? id ??????????
            $recipeErrors = @($all | Select-String "Failed to parse recipe|Unknown item|Parsing error loading recipe")
            if ($recipeErrors.Count -eq 0) {
                Say-Check $true "no recipe failed to load" "3 recipes parsed without complaint"
            } else {
                $allGood = $false
                Say-Check $false "no recipe failed to load" "$($recipeErrors.Count) complaint(s) in the log"
            }

            $verdict = "FAIL"
            $why = "?? / ?? ???"
            if ($allGood) {
                $verdict = "PASS"
                $why = "cinnabar resolves, the base places, and breaking it drops itself"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($CodexTest) {
            # ----------------------------------------------- codex analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            # bucket the [codex] summary lines by the phase they happened in.
            # 'find' has to be in this list too: leaving it out silently dropped the
            # search lines and the search check reported "no hit line" for a search
            # that had actually run fine.
            $cur = "start"
            $buckets = @{}
            foreach ($l in $all) {
                if ($l -match '\[verify\] phase-([A-Z-]+)') { $cur = $Matches[1]; continue }
                if ($l -notmatch '\[codex\] (list|detail|find) ') { continue }
                if (-not $buckets.ContainsKey($cur)) { $buckets[$cur] = @() }
                $buckets[$cur] += $l
            }
            $newWorld = @($buckets["NEW-WORLD"])
            $placed = @($buckets["PLACED"])
            $activated = @($buckets["ACTIVATED"])

            # Named capture groups on purpose: $Matches[0] is the WHOLE match, so
            # positional indices are off by one - and an off-by-one here reads a
            # perfectly good line as a failure (it did, on the first run).
            $pick = {
                param($lines, $pattern)
                foreach ($l in $lines) { if ($l -match $pattern) { return $Matches } }
                return $null
            }

            Write-Host " log        : $log"
            Write-Host ("  NEW-WORLD: {0} codex lines; PLACED: {1}; ACTIVATED: {2}" -f `
                $newWorld.Count, $placed.Count, $activated.Count)
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }

            # ? first: a function that fails to LOAD kills every later line in it
            #    silently. That is exactly what happened the first time this test ran
            #    (an extra argument on 'fulu codex find' made c2/c3 unloadable, so the
            #    later phases produced nothing at all and four checks went red).
            $loadErrors = @($all | Select-String -SimpleMatch "Failed to load function")
            if ($loadErrors.Count -eq 0) {
                Say-Check $true "no datapack function failed to load" "no 'Failed to load function' in the log"
            } else {
                $allGood = $false
                Say-Check $false "no datapack function failed to load" `
                    "$($loadErrors.Count) function(s) failed - later lines never ran"
            }

            # ? a fresh world: nothing named, one locked (???)
            $m = & $pick $newWorld 'list player=\S+ bits=(?<bits>\d+) lines=(?<lines>\d+) named=(?<named>\d+) locked=(?<locked>\d+)'
            if ($m -and $m["bits"] -eq "0" -and $m["named"] -eq "0" -and $m["locked"] -eq "1") {
                Say-Check $true "a fresh world shows one ??? and no names" "bits=0 named=0 locked=1"
            } else {
                $allGood = $false
                Say-Check $false "a fresh world shows one ??? and no names" `
                    $(if ($m) { "bits=$($m['bits']) named=$($m['named']) locked=$($m['locked'])" } else { "no list line found" })
            }

            # ? after placing an eye: the name appears, the detail is still shut
            #    (??? = 3 ??? + 1 ??? = 4 ?)
            $m = & $pick $placed 'list player=\S+ bits=(?<bits>\d+) lines=(?<lines>\d+) named=(?<named>\d+) locked=(?<locked>\d+)'
            $d = & $pick $placed 'detail player=\S+ id=(?<id>\S+) bits=(?<bits>\d+) unlocked=(?<unlocked>\S+) lines=(?<lines>\d+)'
            if ($m -and $m["named"] -eq "1" -and $m["locked"] -eq "0" -and $d -and $d["unlocked"] -eq "false" -and $d["lines"] -eq "4") {
                Say-Check $true "placing an eye reveals the seen-level detail" `
                    "list named=1; detail unlocked=false lines=4 (title + name/range/effect)"
            } else {
                $allGood = $false
                Say-Check $false "placing an eye reveals the seen-level detail" `
                    "$(if ($m) { "named=$($m['named'])" } else { "no list" }); $(if ($d) { "unlocked=$($d['unlocked']) lines=$($d['lines'])" } else { "no detail" })"
            }

            # ? after activating: the full detail
            #    (??? = 6 ??? + 1 ??? = 7 ?)
            $d = & $pick $activated 'detail player=\S+ id=infighting bits=(?<bits>\d+) unlocked=(?<unlocked>\S+) lines=(?<lines>\d+)'
            if ($d -and $d["unlocked"] -eq "true" -and $d["lines"] -eq "7") {
                Say-Check $true "activating unlocks the full detail" `
                    "unlocked=true, 7 lines (title + name/range/effect/material/activate/deactivate)"
            } else {
                $allGood = $false
                Say-Check $false "activating unlocks the full detail" `
                    $(if ($d) { "unlocked=$($d['unlocked']) lines=$($d['lines'])" } else { "no detail line found" })
            }

            # ? a bogus id is reported, not crashed on
            $d = & $pick $activated 'detail player=\S+ id=nosuchformation bits=(?<bits>-?\d+) lines=(?<lines>\d+)'
            if ($d -and $d["lines"] -eq "1") {
                Say-Check $true "an unknown formation id is reported" "1 line: no such formation"
            } else {
                $allGood = $false
                Say-Check $false "an unknown formation id is reported" `
                    $(if ($d) { "lines=$($d['lines'])" } else { "the command did not run at all" })
            }

            # ? the screen's model: the three stages, and the search filter.
            #    These are the exact functions the GUI calls (stage/matches/detailLines),
            #    which is why they are pure - so that "what the screen would show" can
            #    be checked here even though the drawing itself cannot.
            $s = & $pick $placed 'list player=\S+ bits=(?<bits>\d+) .*stages=(?<stages>\S+)'
            $s2 = & $pick $activated 'list player=\S+ bits=(?<bits>\d+) .*stages=(?<stages>\S+)'
            if ($s -and $s["stages"] -eq "SEEN" -and $s2 -and $s2["stages"] -eq "MASTERED") {
                Say-Check $true "the GUI's stage model: SEEN then MASTERED" `
                    "bits=1 -> SEEN, bits=3 -> MASTERED (drives the icon brightness)"
            } else {
                $allGood = $false
                Say-Check $false "the GUI's stage model: SEEN then MASTERED" `
                    "$(if ($s) { "bits=1 -> $($s['stages'])" } else { "no SEEN list" }); $(if ($s2) { "bits=3 -> $($s2['stages'])" } else { "no MASTERED list" })"
            }

            $hit = & $pick $activated 'find query=infight hits=(?<hits>\d+)'
            $miss = & $pick $activated 'find query=zzzz hits=(?<hits>\d+)'
            if ($hit -and $hit["hits"] -eq "1" -and $miss -and $miss["hits"] -eq "0") {
                Say-Check $true "the search box filters (same rule the screen uses)" `
                    "infight -> 1 hit, zzzz -> 0 hits"
            } else {
                $allGood = $false
                Say-Check $false "the search box filters (same rule the screen uses)" `
                    "$(if ($hit) { "infight -> $($hit['hits'])" } else { "no hit line" }); $(if ($miss) { "zzzz -> $($miss['hits'])" } else { "no miss line" })"
            }

            $verdict = "FAIL"
            $why = "the codex does not follow the unlock stages"
            if ($allGood) {
                $verdict = "PASS"
                $why = "??? until you place one, name after placing, full detail after activating"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($OutlineTest) {
            # --------------------------------------------- outline analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            $lines = @($all | Select-String -SimpleMatch "[outline] ")
            $points = New-Object System.Collections.ArrayList
            $radii = New-Object System.Collections.ArrayList
            $heights = New-Object System.Collections.ArrayList
            $sents = New-Object System.Collections.ArrayList
            foreach ($l in $lines) {
                if ($l.Line -match '\[outline\] points=(\d+) r=(\d+) y=([\d.\-]+) sent=(\d+)') {
                    [void]$points.Add([int]$Matches[1])
                    [void]$radii.Add([int]$Matches[2])
                    [void]$heights.Add([double]$Matches[3])
                    [void]$sents.Add([int]$Matches[4])
                }
            }
            $distinctPoints = @($points | Sort-Object -Unique)
            $distinctRadii = @($radii | Sort-Object -Unique)
            $distinctHeights = @($heights | Sort-Object -Unique)

            Write-Host " log        : $log"
            Write-Host ("  {0} outline bursts; points [{1}]; r [{2}]; y [{3}]" -f `
                $lines.Count, ($distinctPoints -join ", "), ($distinctRadii -join ", "), ($distinctHeights -join ", "))
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            if ($lines.Count -ge 3) {
                Say-Check $true "the boundary is redrawn over and over" `
                    "$($lines.Count) bursts (the effect lasts 3 s, the dots do not)"
            } else {
                $allGood = $false
                Say-Check $false "the boundary is redrawn over and over" "only $($lines.Count) bursts logged"
            }
            # one dot per block on the ring: (2r+1)*2 + (2r-1)*2 = 80 at r=10
            if ($distinctPoints.Count -eq 1 -and $distinctPoints[0] -eq 80) {
                Say-Check $true "one dot per block, 80 of them" "every burst said points=80"
            } else {
                $allGood = $false
                Say-Check $false "one dot per block, 80 of them" `
                    "points seen: [$($distinctPoints -join ", ")] - expected 80 for r=10"
            }
            if ($distinctRadii.Count -eq 1 -and $distinctRadii[0] -eq 10) {
                Say-Check $true "the ring sits on the +-10 boundary" "r=10 on every burst"
            } else {
                $allGood = $false
                Say-Check $false "the ring sits on the +-10 boundary" "r seen: [$($distinctRadii -join ", ")]"
            }
            if ($distinctHeights.Count -eq 1 -and [math]::Abs($distinctHeights[0] - 100.1) -lt 0.01) {
                Say-Check $true "the ring is drawn just above the eye's own layer" `
                    "y=$($distinctHeights[0]) for an eye at y=100 (ground ring)"
            } else {
                $allGood = $false
                Say-Check $false "the ring is drawn just above the eye's own layer" `
                    "y seen: [$($distinctHeights -join ", ")] - expected 100.1"
            }
            $zeroSent = @($sents | Where-Object { $_ -eq 0 }).Count
            if ($zeroSent -eq $sents.Count) {
                Say-Check $true "nothing was actually sent (no player to send to)" `
                    "sent=0 on all $($sents.Count) bursts; geometry verified, delivery needs a client"
            } else {
                $allGood = $false
                Say-Check $false "nothing was actually sent (no player to send to)" "some bursts reported sent>0"
            }

            $verdict = "FAIL"
            $why = "the boundary geometry is wrong"
            if ($allGood) {
                $verdict = "PASS"
                $why = "80 dots on the +-10 ring, just above the eye, redrawn for 3 s"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($ParticleTest) {
            # -------------------------------------------- particle analysis --
            # The window starts where the monsters were summoned: before that
            # there was nothing to mark, so those lines would say 0.
            $all = @(Get-Content $log -Encoding UTF8)
            $seenSpawn = $false
            $marked = New-Object System.Collections.ArrayList
            $particles = New-Object System.Collections.ArrayList
            $watchers = New-Object System.Collections.ArrayList
            $storedCounts = New-Object System.Collections.ArrayList
            foreach ($line in $all) {
                if ($line -match '\[verify\] phase-SPAWNED') { $seenSpawn = $true; continue }
                if ($line -match '\[verify\] phase-WINDOW-END') { break }
                if (-not $seenSpawn) { continue }
                if ($line -match '\[mark\] marked=(\d+) particles=(\d+) watchers=(\d+)(?: count=(\d+))?') {
                    [void]$marked.Add([int]$Matches[1])
                    [void]$particles.Add([int]$Matches[2])
                    [void]$watchers.Add([int]$Matches[3])
                    if ($Matches[4]) { [void]$storedCounts.Add([int]$Matches[4]) }
                }
            }
            $distinctMarked = @($marked | Sort-Object -Unique)
            $maxParticles = if ($particles.Count -gt 0) { ($particles | Measure-Object -Maximum).Maximum } else { 0 }
            $minParticles = if ($particles.Count -gt 0) { ($particles | Measure-Object -Minimum).Minimum } else { 0 }

            Write-Host " log        : $log"
            Write-Host ("  {0} particle ticks in the window; monsters counted: [{1}]; particles per tick: {2}..{3}" -f `
                $marked.Count, ($distinctMarked -join ", "), $minParticles, $maxParticles)
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            if ($marked.Count -ge 20) {
                Say-Check $true "particles are emitted every tick" "$($marked.Count) ticks in a 4 s window"
            } else {
                $allGood = $false
                Say-Check $false "particles are emitted every tick" "only $($marked.Count) ticks logged"
            }
            if ($distinctMarked.Count -eq 1 -and $distinctMarked[0] -eq 3) {
                Say-Check $true "only the monsters inside the formation count" `
                    "every tick said marked=3, never 4 (one zombie is parked outside)"
            } else {
                $allGood = $false
                Say-Check $false "only the monsters inside the formation count" `
                    "saw marked values: [$($distinctMarked -join ", ")] - expected only 3"
            }
            if ($minParticles -ge 3 -and $maxParticles -le 6) {
                Say-Check $true "1-2 particles per monster, no more" `
                    "3 monsters -> $minParticles..$maxParticles particles per tick"
            } else {
                $allGood = $false
                Say-Check $false "1-2 particles per monster, no more" `
                    "particles per tick: $minParticles..$maxParticles (expected 3..6 for 3 monsters)"
            }
            $zeroWatchers = @($watchers | Where-Object { $_ -eq 0 }).Count
            if ($zeroWatchers -eq $watchers.Count) {
                Say-Check $true "nobody was close enough, so nothing was actually sent" `
                    "watchers=0 on all $($watchers.Count) ticks (the counts above are the intent)"
            } else {
                $allGood = $false
                Say-Check $false "nobody was close enough, so nothing was actually sent" `
                    "some ticks reported watchers>0 - a player was online?"
            }
            # The number ???????? will hand to the player is the one the tick
            # stores every tick, not a fresh scan at click time. This checks that the
            # stored number is being refreshed (and matches the scan) on every tick.
            $distinctStored = @($storedCounts | Sort-Object -Unique)
            if ($storedCounts.Count -ge 20 -and $distinctStored.Count -eq 1 -and $distinctStored[0] -eq 3) {
                Say-Check $true "the stored monster count is refreshed every tick" `
                    "count=3 on all $($storedCounts.Count) ticks - that is what the info screen reads"
            } else {
                $allGood = $false
                Say-Check $false "the stored monster count is refreshed every tick" `
                    "saw count values [$($distinctStored -join ", ")] over $($storedCounts.Count) ticks"
            }

            $verdict = "FAIL"
            $why = "the particles are not scoped to the formation"
            if ($allGood) {
                $verdict = "PASS"
                $why = "emitted every tick, only for monsters inside, 1-2 each"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($CombatTest) {
            # ---------------------------------------------- combat analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            function Count-Of($pattern) { @($all | Select-String -SimpleMatch $pattern).Count }
            $infinite = Count-Of "[verify] INSIDE-INFINITE"
            $notInfinite = Count-Of "[verify] INSIDE-NOT-INFINITE"
            $gotHunger = (Count-Of "[verify] VICTIM-GOT-HUNGER") + (Count-Of "[verify] VICTIM-GOT-HUNGER-LATER")
            $tookDamage = Count-Of "[verify] VICTIM-TOOK-DAMAGE"
            $slot0Other = Count-Of "[verify] SLOT0-OTHER-VANILLA"
            $second = Count-Of "[verify] VICTIM-HAS-SECOND-EFFECT"
            $transfers = (Count-Of "[combat] passed")
            $outside20 = Count-Of "[verify] OUTSIDE-20S"
            $stillInfinite = Count-Of "[verify] OUTSIDE-STILL-INFINITE"

            Write-Host " log        : $log"
            Write-Host ("  the handler passed an effect on {0} time(s); the victim took damage: {1};" -f `
                $transfers, $(if ($tookDamage -gt 0) { "yes" } else { "no" }))
            Write-Host ("  the other vanilla effect seen on the victim: {0}" -f `
                $(if ($slot0Other -gt 0) { "yes (so the transfer lands, just not hunger?)" } else { "no" }))
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            if ($infinite -ge 1 -and $notInfinite -eq 0) {
                Say-Check $true "inside the formation the marker is INFINITE" "duration read back as -1"
            } else {
                $allGood = $false
                Say-Check $false "inside the formation the marker is INFINITE" `
                    "infinite x$infinite, not infinite x$notInfinite"
            }
            if ($gotHunger -ge 1) {
                Say-Check $true "a marked husk passes hunger to the zombie it hits" `
                    "found the vanilla hunger effect (id 17) on the victim"
            } else {
                $allGood = $false
                Say-Check $false "a marked husk passes hunger to the zombie it hits" `
                    "hunger never appeared; handler transfers: $transfers, victim took damage: $tookDamage"
            }
            if ($outside20 -ge 1 -and $stillInfinite -eq 0) {
                Say-Check $true "stepping out turns it into a 20 s countdown" `
                    "duration read back within 1..400 ticks"
            } else {
                $allGood = $false
                Say-Check $false "stepping out turns it into a 20 s countdown" `
                    "20s x$outside20, still infinite x$stillInfinite"
            }
            $verdict = "FAIL"
            $why = "the combat transfer or the duration rule is wrong"
            if ($allGood) {
                $verdict = "PASS"
                $why = "attack effect passed on; infinite inside, 20 s outside"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($EffectTest) {
            # ---------------------------------------------- effect analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            function Count-Of($pattern) { @($all | Select-String -SimpleMatch $pattern).Count }
            $marked = Count-Of "[verify] MARKED-INSIDE"
            $notMarked = Count-Of "[verify] NOT-MARKED-INSIDE"
            $again = Count-Of "[verify] MARKED-AGAIN"
            $registered = Count-Of "[verify] EFFECT-REGISTERED"
            $hasByteId = Count-Of "[verify] HAS-BYTE-ID"
            $stringId = Count-Of "[verify] STRING-ID-MATCHES"
            $ampBack = Count-Of "[verify] AMPLIFIER-READS-BACK"
            $stillNear = Count-Of "[verify] NEAR-STILL-INFINITE"
            $clearedNear = Count-Of "[verify] NEAR-20S"
            $nearGone = Count-Of "[verify] NEAR-GONE"
            $againNear = Count-Of "[verify] AGAIN-20S"
            $againInfinite = Count-Of "[verify] AGAIN-STILL-INFINITE"
            $stillFar = Count-Of "[verify] FAR-STILL-400"
            $clearedFar = Count-Of "[verify] FAR-COUNTING-DOWN"
            $farInfinite = Count-Of "[verify] FAR-INFINITE-AGAIN"
            $idInside = Count-Of "[verify] ID-MATCH-INSIDE"

            Write-Host " log        : $log"
            Write-Host ("  how 'has the effect' was read: duration (amplifier read-back: {0};" -f `
                $(if ($ampBack -gt 0) { "works" } else { "no" }))
            Write-Host ("                                 byte Id key present: {0}; string Id match: {1})" -f `
                $(if ($hasByteId -gt 0) { "yes" } else { "no" }), $(if ($stringId -gt 0) { "yes" } else { "no" }))
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            if ($registered -ge 1) {
                Say-Check $true "the effect is registered" "effect give fulu:infighting worked"
            } else {
                $allGood = $false
                Say-Check $false "the effect is registered" "the probe could not read a duration back"
            }
            if ($marked -ge 1 -and $notMarked -eq 0) {
                Say-Check $true "a monster inside the formation is marked" "MARKED-INSIDE x$marked"
            } else {
                $allGood = $false
                Say-Check $false "a monster inside the formation is marked" `
                    "marked x$marked, not marked x$notMarked"
            }
            if ($clearedNear -ge 1 -and $stillNear -eq 0) {
                Say-Check $true "stepping out turns it into a 20 s countdown" `
                    "NEAR-20S x$clearedNear, still infinite x$stillNear (checked 0.5 s later)"
            } else {
                $allGood = $false
                Say-Check $false "stepping out turns it into a 20 s countdown" `
                    "NEAR-20S x$clearedNear, still infinite x$stillNear, gone x$nearGone"
            }
            if ($again -ge 1) {
                Say-Check $true "walking back in marks it again" "MARKED-AGAIN x$again"
            } else {
                $allGood = $false
                Say-Check $false "walking back in marks it again" "never re-marked"
            }
            if ($againNear -ge 1 -and $againInfinite -eq 0) {
                Say-Check $true "walking out a second time converts it again" `
                    "AGAIN-20S x$againNear, still infinite x$againInfinite"
            } else {
                $allGood = $false
                Say-Check $false "walking out a second time converts it again" `
                    "AGAIN-20S x$againNear, still infinite x$againInfinite"
            }
            if ($clearedFar -ge 1 -and $stillFar -eq 0 -and $farInfinite -eq 0) {
                Say-Check $true "far away it just counts down (nothing tops it up)" `
                    "FAR-COUNTING-DOWN x$clearedFar (still 400: $stillFar, infinite again: $farInfinite)"
            } else {
                $allGood = $false
                Say-Check $false "far away it just counts down (nothing tops it up)" `
                    "counting down x$clearedFar, still 400 x$stillFar, infinite again x$farInfinite"
            }
            # The duration above only says "an effect is active". This one says it is
            # OUR effect (Forge stores a modded effect's identity as a string under
            # forge:id; the numeric Id byte is a placeholder 0 for modded effects, so
            # an nbt selector written against Id can never match here).
            if ($idInside -ge 1) {
                Say-Check $true "and that marker really is fulu:infighting" `
                    "matched by the forge:id string filter while inside"
            } else {
                $allGood = $false
                Say-Check $false "and that marker really is fulu:infighting" `
                    "the forge:id filter never matched (x$idInside)"
            }

            $verdict = "FAIL"
            $why = "the marker does not behave"
            if ($allGood) {
                $verdict = "PASS"
                $why = "infinite inside; a 20 s countdown once out; still counting down far away"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($RestartTest) {
            # --------------------------------------------- restart analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            function Count-Of($pattern) { @($all | Select-String -SimpleMatch $pattern).Count }
            $beats = Count-Of "[verify] heartbeat"

            Write-Host " log        : $log"
            Write-Host " phase      : $Phase"
            Write-Host " heartbeats : $beats  (the datapack clock: the server really was alive)"
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }

            if ($Phase -eq "1") {
                $setOk = Count-Of "[verify] SET-OK"
                $setBad = Count-Of "[verify] SET-FAILED"
                if ($setOk -ge 1 -and $setBad -eq 0) {
                    Say-Check $true "the talisman was fitted and stored" "SET-OK x$setOk"
                } else {
                    $allGood = $false
                    Say-Check $false "the talisman was fitted and stored" "SET-OK x$setOk, SET-FAILED x$setBad"
                }
                # Independent: is it on the disk? Phase 2 can only see the disk, so
                # if this fails the next phase would fail for a vanilla reason.
                $region = Join-Path $worldDir "region\r.0.0.mca"
                if (Test-Path $region) {
                    $hits = @(Get-NbtKeyInRegion $region "Talisman" "chaos")
                    if (-not $script:RegionReaderBroken -and $hits.Count -ge 1) {
                        Say-Check $true "it is on the disk before the game is closed" `
                            "$($hits.Count) hit(s) in chunk $($hits[0].x),$($hits[0].z), value byte $($hits[0].value)"
                    } else {
                        $allGood = $false
                        Say-Check $false "it is on the disk before the game is closed" `
                            "$($hits.Count) hit(s); reader broken: $($script:RegionReaderBroken)"
                    }
                } else {
                    $allGood = $false
                    Say-Check $false "it is on the disk before the game is closed" "no region file"
                }
                Write-Host ""
                Write-Host " now run phase 2 - a NEW server process on the same world:"
                Write-Host "   tools\verify_tick.ps1 -RestartTest -Phase 2 -KeepWorld"
            } else {
                $nbtOk = Count-Of "[verify] NBT-SURVIVED"
                $nbtBad = Count-Of "[verify] NBT-LOST"
                $lookOk = Count-Of "[verify] LOOK-SURVIVED"
                $lookBad = Count-Of "[verify] LOOK-LOST"
                $noPlayers = Count-Of "[verify] NO-PLAYERS"
                $fighting = Count-Of "[verify] FIGHTING"
                $allDead = Count-Of "[verify] ALL-DEAD"

                if ($nbtOk -ge 1 -and $nbtBad -eq 0) {
                    Say-Check $true "the talisman kind came back after a restart" "NBT-SURVIVED x$nbtOk"
                } else {
                    $allGood = $false
                    Say-Check $false "the talisman kind came back after a restart" "SURVIVED x$nbtOk, LOST x$nbtBad"
                }
                if ($lookOk -ge 1 -and $lookBad -eq 0) {
                    Say-Check $true "the blockstate came back too (the look)" "LOOK-SURVIVED x$lookOk"
                } else {
                    $allGood = $false
                    Say-Check $false "the blockstate came back too (the look)" "SURVIVED x$lookOk, LOST x$lookBad"
                }
                if ($noPlayers -ge 1) {
                    Say-Check $true "it ran with NO player connected" "NO-PLAYERS confirmed in game"
                } else {
                    $allGood = $false
                    Say-Check $false "it ran with NO player connected" "could not confirm nobody was online"
                }
                if ($fighting -ge 1 -or $allDead -ge 1) {
                    Say-Check $true "and it works right away, nothing switched on" `
                        "FIGHTING x$fighting, ALL-DEAD x$allDead (16 s)"
                } else {
                    $allGood = $false
                    Say-Check $false "and it works right away, nothing switched on" "nobody got hurt after the restart"
                }
                if ($beats -ge 2) {
                    Say-Check $true "the new server process really ran" "$beats heartbeats"
                } else {
                    $allGood = $false
                    Say-Check $false "the new server process really ran" "only $beats heartbeats"
                }
            }

            $verdict = "FAIL"
            $why = "the state did not survive"
            if ($allGood) {
                $verdict = "PASS"
                $why = if ($Phase -eq "1") { "fitted and written to disk" } else { "restored from disk and working" }
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($DisplayTest) {
            # --------------------------------------------- display analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            $seenRising = $false
            $announces = 0
            $lastCount = -1
            foreach ($line in $all) {
                if ($line -match '\[verify\] phase-RISING') { $seenRising = $true; continue }
                if (-not $seenRising) { continue }
                if ($line -match '\[scan\] monsters=(\d+)') {
                    $announces++
                    $lastCount = [int]$Matches[1]
                }
            }

            Write-Host " log        : $log"
            Write-Host ""
            Write-Host ("  in the window the count grew by 1 every second; the eye announced {0} time(s), last number {1}" -f `
                $announces, $lastCount)
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            # 20 s of growth, 5 s beat -> 4 announces (allow 3..7 for boundary
            # alignment). Without the beat it would be ~20.
            if ($announces -ge 3 -and $announces -le 7) {
                Say-Check $true "the announce walks the 5 s beat" `
                    "$announces announces while the count changed every second (~20 without a beat)"
            } else {
                $allGood = $false
                Say-Check $false "the announce walks the 5 s beat" `
                    "$announces announces - expected 3..7 (about 20 once per second, about 1 if it never fired)"
            }
            if ($lastCount -ge 3) {
                Say-Check $true "the scan still sees every arrival" `
                    "last announced number was $lastCount, so the scan kept up"
            } else {
                $allGood = $false
                Say-Check $false "the scan still sees every arrival" "last announced number was $lastCount"
            }

            $verdict = "FAIL"
            $why = "the announce is not on a 5 s beat"
            if ($allGood) {
                $verdict = "PASS"
                $why = "scanning every tick, announcing on a 5 s beat"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($TalismanTest) {
            # -------------------------------------------- talisman analysis --
            # Walk the log once and bucket everything by the phase it happened in.
            # The phase markers are emitted AFTER the health checks, so the
            # check lines fall inside the window they belong to.
            $all = @(Get-Content $log -Encoding UTF8)
            $phase = "start"
            $buckets = @{}
            $newBucket = { @{ scan = 0; fight = 0; full = 0; hurt = 0; counts = (New-Object System.Collections.ArrayList) } }
            $buckets[$phase] = & $newBucket
            foreach ($line in $all) {
                if ($line -match '\[verify\] phase-([A-Z-]+)') {
                    $phase = $Matches[1]
                    if (-not $buckets.ContainsKey($phase)) { $buckets[$phase] = & $newBucket }
                    continue
                }
                if (-not $buckets.ContainsKey($phase)) { $buckets[$phase] = & $newBucket }
                $b = $buckets[$phase]
                if ($line -match '\[scan\] monsters=(\d+)') {
                    $b.scan++
                    # remember the sequence: a count that goes DOWN means casualties
                    [void]$b.counts.Add([int]$Matches[1])
                }
                if ($line -match '\[fight\] \d+/\d+') { $b.fight++ }
                if ($line -match '\[verify\] FULL-HP') { $b.full++ }
                if ($line -match '\[verify\] HURT') { $b.hurt++ }
            }
            $empty = @{ scan = 0; fight = 0; full = 0; hurt = 0; counts = (New-Object System.Collections.ArrayList) }
            $no = $buckets["NO-TALISMAN"]
            $yes = $buckets["WITH-TALISMAN"]
            if ($null -eq $no) { $no = $empty }
            if ($null -eq $yes) { $yes = $empty }
            # casualties = how far the monster count fell inside the window
            $noDead = if ($no.counts.Count -ge 2) { $no.counts[0] - $no.counts[$no.counts.Count - 1] } else { 0 }
            $yesDead = if ($yes.counts.Count -ge 2) { $yes.counts[0] - $yes.counts[$yes.counts.Count - 1] } else { 0 }

            Write-Host " log        : $log"
            Write-Host ""
            Write-Host ("  no talisman  : scan x{0}, fight x{1}, untouched x{2}, hurt x{3}, casualties {4}" -f `
                $no.scan, $no.fight, $no.full, $no.hurt, $noDead)
            Write-Host ("  with talisman: scan x{0}, fight x{1}, untouched x{2}, hurt x{3}, casualties {4}" -f `
                $yes.scan, $yes.fight, $yes.full, $yes.hurt, $yesDead)
            Write-Host ""

            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            if ($no.scan -ge 1) {
                Say-Check $true "segment 1 runs without a talisman" "the scan spoke $($no.scan)x while no talisman was fitted"
            } else {
                $allGood = $false
                Say-Check $false "segment 1 runs without a talisman" "the scan never spoke"
            }
            if ($no.fight -eq 0 -and $no.hurt -eq 0 -and $noDead -eq 0) {
                Say-Check $true "segment 2 is skipped without a talisman" `
                    "0 fight rounds, 0 hurt, 0 casualties (3 zombies stayed at 20 HP)"
            } else {
                $allGood = $false
                Say-Check $false "segment 2 is skipped without a talisman" `
                    "fight x$($no.fight), hurt x$($no.hurt), casualties $noDead - they were set on each other anyway"
            }
            # "the fight happened" must accept a kill as evidence too: a zombie that
            # died is no longer a "hurt zombie", so hurt can legitimately be 0 when
            # the brawl was the most violent.
            if ($yes.fight -ge 1 -and ($yes.hurt -ge 1 -or $yesDead -ge 1)) {
                Say-Check $true "segment 2 runs once the talisman is fitted" `
                    "fight x$($yes.fight), hurt x$($yes.hurt), casualties $yesDead"
            } else {
                $allGood = $false
                Say-Check $false "segment 2 runs once the talisman is fitted" `
                    "fight x$($yes.fight), hurt x$($yes.hurt), casualties $yesDead - nobody fought"
            }
            # The cooldown is gone: the second segment should now run EVERY tick,
            # not once per 5 s. The WITH-TALISMAN window is 16 s = 320 ticks with
            # 3 monsters = about 960 assignment lines. With the old 5 s cooldown
            # the same window produced 9. 300 is a threshold that cannot be met
            # by accident on the old design, and it still holds if one zombie
            # dies early (2 lines per tick = 640).
            if ($yes.fight -ge 300) {
                Say-Check $true "segment 2 runs every tick (no cooldown)" `
                    "$($yes.fight) lines in a 16 s window (~$([int]($yes.fight / 16)) per second)"
            } else {
                $allGood = $false
                Say-Check $false "segment 2 runs every tick (no cooldown)" `
                    "only $($yes.fight) lines in a 16 s window - that looks like a cooldown is still in"
            }

            $verdict = "FAIL"
            $why = "the two segments do not separate"
            if ($allGood) {
                $verdict = "PASS"
                $why = "scanning always, fighting only with the talisman"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($StateTest) {
            # ------------------------------------------- blockstate analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            $defaultOk = @($all | Select-String -SimpleMatch "DEFAULT-OFF-OK").Count
            $defaultBad = @($all | Select-String -SimpleMatch "DEFAULT-OFF-BAD").Count
            $switchOk = @($all | Select-String -SimpleMatch "SWITCH-ON-OK").Count
            $switchBad = @($all | Select-String -SimpleMatch "SWITCH-ON-BAD").Count
            $beKept = @($all | Select-String -SimpleMatch "BE-KEPT-OK").Count
            $beLost = @($all | Select-String -SimpleMatch "BE-KEPT-UNEXPECTED").Count
            $itemOk = @($all | Select-String -SimpleMatch "ITEM-OK").Count
            $itemMissing = @($all | Select-String -SimpleMatch "ITEM-MISSING").Count

            Write-Host " log        : $log"
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            if ($defaultOk -ge 1 -and $defaultBad -eq 0) {
                Say-Check $true "a new eye defaults to talisman=empty" "DEFAULT-OFF-OK x$defaultOk"
            } else {
                $allGood = $false
                Say-Check $false "a new eye defaults to talisman=empty" "OK x$defaultOk, BAD x$defaultBad"
            }
            if ($switchOk -ge 1 -and $switchBad -eq 0) {
                Say-Check $true "the state can be switched to a talisman" "SWITCH-ON-OK x$switchOk"
            } else {
                $allGood = $false
                Say-Check $false "the state can be switched to a talisman" "OK x$switchOk, BAD x$switchBad"
            }
            if ($beKept -ge 1 -and $beLost -eq 0) {
                Say-Check $true "switching the state keeps the block entity" "BE-KEPT-OK x$beKept (nbt flag still readable)"
            } else {
                $allGood = $false
                Say-Check $false "switching the state keeps the block entity" "OK x$beKept, UNEXPECTED x$beLost"
            }
            if ($itemOk -ge 1 -and $itemMissing -eq 0) {
                Say-Check $true "the chaos talisman item really exists" "ITEM-OK x$itemOk (summoned, matched by nbt)"
            } else {
                $allGood = $false
                Say-Check $false "the chaos talisman item really exists" "OK x$itemOk, MISSING x$itemMissing"
            }
            Write-Host ""
            Write-Host " what this does NOT prove: that the block actually RENDERS the other model,"
            Write-Host " nor the right-click itself (that needs a real player)."
            Write-Host " The files are checked by tools\verify_jar.py (blockstate variants -> both"
            Write-Host " models -> both textures), the look is in tools\out\formation_eye_two_looks.png,"
            Write-Host " and the hand/talisman logic is exercised by hand in game."

            $verdict = "FAIL"
            $why = "the blockstate property is not usable"
            if ($allGood) {
                $verdict = "PASS"
                $why = "talisman is three-valued, defaults to empty, and can be switched"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($PersistTest) {
            # ------------------------------------------ persistence analysis --
            $all = @(Get-Content $log -Encoding UTF8)
            $setOk = @($all | Select-String -SimpleMatch "SET-OK").Count
            $setBad = @($all | Select-String -SimpleMatch "SET-FAILED").Count
            $keepOk = @($all | Select-String -SimpleMatch "PERSIST-OK").Count
            $keepBad = @($all | Select-String -SimpleMatch "PERSIST-LOST").Count

            Write-Host " log        : $log"
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            if ($setOk -ge 1 -and $setBad -eq 0) {
                Say-Check $true "the flag can be written and read back" "SET-OK x$setOk, SET-FAILED x$setBad"
            } else {
                $allGood = $false
                Say-Check $false "the flag can be written and read back" "SET-OK x$setOk, SET-FAILED x$setBad"
            }
            if ($keepOk -ge 1 -and $keepBad -eq 0) {
                Say-Check $true "it survived a chunk unload + reload" "PERSIST-OK x$keepOk, PERSIST-LOST x$keepBad"
            } else {
                $allGood = $false
                Say-Check $false "it survived a chunk unload + reload" "PERSIST-OK x$keepOk, PERSIST-LOST x$keepBad"
            }

            # Independent check: read the value straight out of the region file.
            # If saveAdditional() never ran, the key would not be on disk at all.
            $region = Join-Path $worldDir "region\r.0.0.mca"
            Write-Host " region     : $region"
            Write-Host " python     : $(Get-PythonExe)"
            if (Test-Path $region) {
                # NOTE: @(...) is not decoration. PowerShell unrolls a returned
                # collection, so with exactly ONE hit the caller receives a bare
                # object and $hits.Count is $null - "-gt 0" is then false and a
                # perfectly good read was reported as "0 hits" (which then looked
                # like "the mod never saved"). Forcing an array fixes it.
                $hits = @(Get-NbtKeyInRegion $region "Talisman" "chaos")
                if ($script:RegionReaderBroken) {
                    $allGood = $false
                    Say-Check $false "the region file could be read" `
                        "reader broken - $($script:RegionReaderOutput)"
                } elseif ($hits.Count -gt 0 -and @($hits | Where-Object { $_.value -eq 1 }).Count -eq $hits.Count) {
                    Say-Check $true "the value is physically in the region file" `
                        "$($hits.Count) hit(s), value byte 1: $(($hits | ForEach-Object { "chunk $($_.x),$($_.z)" }) -join '; ')"
                } else {
                    $allGood = $false
                    Say-Check $false "the value is physically in the region file" `
                        "0 hits (reader errors: $($script:RegionReadErrors)) - key NOT found in the saved chunk"
                }
            } else {
                $allGood = $false
                Say-Check $false "the value is physically in the region file" "no region file at $region"
            }

            $verdict = "FAIL"
            $why = "the flag did not survive"
            if ($allGood) {
                $verdict = "PASS"
                $why = "saved to NBT, read back after a chunk unload, and present on disk"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($FightTest) {
            $all = @(Get-Content $log -Encoding UTF8)
            $aloneOk = @($all | Select-String -SimpleMatch "ALONE-UNTOUCHED").Count
            $aloneHurt = @($all | Select-String -SimpleMatch "ALONE-HURT").Count
            $fighting = @($all | Select-String -SimpleMatch "FIGHTING").Count
            $fightCalls = @($all | Select-String -SimpleMatch "[fight] ")
            $selfTargets = 0
            # Each debug line carries "index/total" for its round (1/3, 2/3, 3/3),
            # so a round can be reconstructed without guessing: it must start at
            # 1, count up without gaps, and end at exactly `total`.
            # (Grouping by the "[scan] monsters=" line does NOT work - that line
            # only appears when the count CHANGES, not once per round.)
            $rounds = New-Object System.Collections.ArrayList
            $round = $null
            foreach ($line in $all) {
                if ($line -notmatch '\[fight\] (\d+)/(\d+) #(\d+) .+? -> #(\d+) .+$') { continue }
                $idx = [int]$Matches[1]
                $total = [int]$Matches[2]
                $attackerId = [int]$Matches[3]
                $targetId = [int]$Matches[4]
                # identity = entity id, NOT position: in a brawl two monsters often
                # share the same block, and comparing coordinates reported a bogus
                # "targets itself" that way
                if ($attackerId -eq $targetId) { $selfTargets++ }
                if ($idx -eq 1) {
                    $round = [pscustomobject]@{ total = $total; seen = 0; next = 1; bogus = $false }
                    [void]$rounds.Add($round)
                }
                if ($null -eq $round) { continue }
                if ($idx -ne $round.next) { $round.bogus = $true }
                $round.next = $idx + 1
                $round.seen++
            }
            $wrongRounds = @($rounds | Where-Object { $_.bogus -or $_.seen -ne $_.total })

            Write-Host " log        : $log"
            Write-Host ""
            $allGood = $true
            function Say-Check($ok, $label, $detail) {
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-46} {1}  {2}" -f $label, $(if ($ok) { "OK" } else { "FAIL" }), $detail) -ForegroundColor $col
            }
            if ($aloneOk -ge 1 -and $aloneHurt -eq 0) {
                Say-Check $true "one zombie stays untouched" "ALONE-UNTOUCHED x$aloneOk, ALONE-HURT x$aloneHurt"
            } else {
                $allGood = $false
                Say-Check $false "one zombie stays untouched" "ALONE-UNTOUCHED x$aloneOk, ALONE-HURT x$aloneHurt"
            }
            if ($fighting -ge 1) {
                Say-Check $true "two zombies hurt each other" "FIGHTING x$fighting"
            } else {
                $allGood = $false
                Say-Check $false "two zombies hurt each other" "FIGHTING x$fighting (nobody took damage)"
            }
            if ($fightCalls.Count -ge 1 -and $selfTargets -eq 0) {
                Say-Check $true "a monster never targets itself" "$($fightCalls.Count) assignments, 0 self"
            } else {
                $allGood = $false
                Say-Check $false "a monster never targets itself" "$($fightCalls.Count) assignments, $selfTargets self"
            }
            if ($rounds.Count -ge 1 -and $wrongRounds.Count -eq 0) {
                $sizes = ($rounds | ForEach-Object { "$($_.seen)/$($_.total)" }) -join ", "
                Say-Check $true "EVERY monster gets a target each round" `
                    "$($rounds.Count) rounds [assigned/total]: $sizes"
            } else {
                $allGood = $false
                $sizes = ($rounds | ForEach-Object { "$($_.seen)/$($_.total)" }) -join ", "
                Say-Check $false "EVERY monster gets a target each round" `
                    "$($rounds.Count) rounds, $($wrongRounds.Count) wrong: $sizes"
            }
            Write-Host ""
            Write-Host " the assignments the block made:"
            $fightCalls | Select-Object -First 6 | ForEach-Object {
                $i = $_.Line.IndexOf("[fight]")
                Write-Host ("    " + $_.Line.Substring($i))
            }

            $verdict = "FAIL"
            $why = "the monsters did not behave as expected"
            if ($allGood) {
                $verdict = "PASS"
                $why = "a lone monster was left alone; a pair actually fought"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } elseif ($MobTest) {
            # ------------------------------------------- mob scan analysis --
            # Walk the log in order: 'phase-X' markers switch the expected count,
            # and every mod line in between is parsed for its number. The message
            # is Chinese and this script is ASCII-only, so the number is taken
            # from whatever follows "]: " - that is the log message itself.
            # Per phase, the set of numbers that are ALLOWED to appear. An empty
            # list means "the block must stay completely silent in this phase" -
            # that is how "an out-of-range zombie is not counted" is verified.
            $expected = @{ ZERO = @(0); THREE = @(3); DOWN10 = @(4); DOWN11 = @()
                           HIGH = @(5); X10 = @(6); X11 = @(); CLEAR = @(0) }
            $order = @("ZERO", "THREE", "DOWN10", "DOWN11", "HIGH", "X10", "X11", "CLEAR")
            # 0, 3, 4, 5, 6, 0 - one line per CHANGE. (X10 adds a monster and speaks;
            # X11 adds one that is out of range, so nothing is spoken.)
            $expectedTotal = 6
            $events = New-Object System.Collections.ArrayList
            foreach ($raw in (Get-Content $log -Encoding UTF8)) {
                if ($raw -match '\[verify\] phase-(\w+)') {
                    [void]$events.Add([pscustomobject]@{ kind = "phase"; value = $Matches[1] })
                    continue
                }
                if ($raw -match 'com\.lengyang\.fulu\.FuLu/\]') {
                    # Only the announce line counts here. Since step 11 the tick also
                    # logs a '[mark] ...' line every single tick, and gobbling those up
                    # as if they were announcements turned this analysis into "745
                    # lines where 6 were expected" - a fake failure.
                    if ($raw -notmatch '\[scan\] monsters=') { continue }
                    $idx = $raw.IndexOf("]: ")
                    if ($idx -ge 0) {
                        $msg = $raw.Substring($idx + 3)
                        if ($msg -match '(\d+)') {
                            [void]$events.Add([pscustomobject]@{ kind = "count"; value = [int]$Matches[1] })
                        }
                    }
                }
            }

            $scanPhase = $null
            $observed = @{}
            foreach ($e in $events) {
                if ($e.kind -eq "phase") { $scanPhase = $e.value; if (-not $observed.ContainsKey($scanPhase)) { $observed[$scanPhase] = @() } }
                elseif ($scanPhase) { $observed[$scanPhase] += $e.value }
            }

            Write-Host " log        : $log"
            Write-Host " phases     : which numbers the block printed in each phase"
            Write-Host ""
            $allGood = $true
            $total = 0
            foreach ($name in $order) {
                $seen = @($observed[$name])
                $allowed = @($expected[$name])
                $total += $seen.Count
                # every printed number must be one of the allowed ones; a phase
                # that should be silent must have printed nothing at all
                $bad = @($seen | Where-Object { $allowed -notcontains $_ })
                $ok = ($bad.Count -eq 0)
                if (-not $ok) { $allGood = $false }
                $want = if ($allowed.Count -eq 0) { "silent" } else { ($allowed -join "/") }
                $col = if ($ok) { "Green" } else { "Red" }
                Write-Host ("  {0,-8} expect {1,-7} saw [{2}]  {3}" -f `
                    $name, $want, ($seen -join ", "), $(if ($ok) { "OK" } else { "MISMATCH" })) -ForegroundColor $col
            }
            Write-Host ""
            # Count every ANNOUNCE line the mod produced in the whole run, not just
            # the ones that landed inside a phase window: whether the very first
            # scan happens just before or just after the opening marker is a
            # tick-ordering detail we should not depend on.
            #
            # Only '[scan] monsters=' lines count. The tick also writes a '[mark]'
            # line every tick (step 11), and counting those made this read "745
            # lines where 6 were expected" - a failure invented by the test.
            $scanLines = @($lines | Where-Object { $_.Line -match '\[scan\] monsters=' })
            Write-Host (" lines total: {0}  (expect {1}: one line per increase, nothing on the way down)" -f `
                $scanLines.Count, $expectedTotal) -ForegroundColor Cyan
            if ($scanLines.Count -ne $expectedTotal) {
                $allGood = $false
                Write-Host "   -> the block is either repeating itself or staying quiet when it should speak" -ForegroundColor Red
            }
            Write-Host " (DOWN11 = 11 blocks below, X11 = 11 blocks to the side: both are out of"
            Write-Host "  range and must stay silent; DOWN10/X10 = exactly 10 and must be counted;"
            Write-Host "  HIGH = 100 blocks up and must be counted)"

            $verdict = "FAIL"
            $why = "the reported counts do not match the summoned mobs"
            if ($loadErrors.Count -gt 0) {
                Write-Host ""
                Write-Host " registry   : FAIL - a function failed to load:" -ForegroundColor Red
                $loadErrors | Select-Object -First 3 | ForEach-Object { Write-Host "      $($_.Line)" }
                $why = "a datapack function failed to load (bad entity/particle id?)"
            } elseif ($allGood) {
                $verdict = "PASS"
                $why = "the 21x21 column (horizontal +-10, 10 down, up to the sky) counted exactly the hostile mobs in range"
            }
            Write-Host ""
            Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
            if ($verdict -like "PASS*") { $exitCode = 0 }
        } else {

        Write-Host " log        : $log"
        Write-Host " grep       : $TAG"
        Write-Host " lines      : $count  block events (expect 0: nothing is written to the log)"
        Write-Host " heartbeats : $($beats.Count)  datapack clock, proves the server was alive"
        Write-Host ""

        $verdict = "FAIL"
        $why = "unexpected state"

        if ($count -gt 0) {
            # The eye does work every tick now, but it announces through the
            # ACTION BAR and has no console print left, so a log line would mean
            # somebody put a print back in.
            Write-Host " block events (should have been none):" -ForegroundColor Yellow
            $lines | Select-Object -First 5 | ForEach-Object { Write-Host "    $($_.Line)" }
            Write-Host ""
            Write-Host " [FAIL]  the block wrote to the log." -ForegroundColor Red
            Write-Host "           - is DEBUG_SCAN_LOG back to false?"
            Write-Host "           - was a FuLu.LOGGER.info left in the tick path?"
            $why = "the formation printed to the console"
        } elseif ($beats.Count -lt 2) {
            Write-Host " last 20 lines of the log:" -ForegroundColor Yellow
            Get-Content $log -Encoding UTF8 -Tail 20 | ForEach-Object { Write-Host "    $_" }
            Write-Host ""
            Write-Host " [FAIL]  the server barely ran, so 'silence' proves nothing." -ForegroundColor Red
            Write-Host "           - see the datapack heartbeat: it should tick every 5 s"
            $why = "the server did not run long enough to judge"
        } else {
            Write-Host " block events: none" -ForegroundColor Green
            Write-Host " heartbeats  :" -ForegroundColor Green
            $beats | Select-Object -First 3 | ForEach-Object { Write-Host "    $($_.Line)" }
            Write-Host ""
            $verdict = "PASS"
            $why = "no console output as expected (it only speaks through the action bar)"
        }

        # the datapack's 'particle fulu:spirit_mote' smoke test
        if ($loadErrors.Count -gt 0) {
            Write-Host " registry   : FAIL - a function failed to load:" -ForegroundColor Red
            $loadErrors | Select-Object -First 3 | ForEach-Object { Write-Host "      $($_.Line)" }
            $verdict = "FAIL"
            $why = "fulu:spirit_mote is probably not registered"
        } else {
            Write-Host " registry   : PASS - no 'Failed to load function' in the log" -ForegroundColor Green
        }
        Write-Host ""
        Write-Host " verdict    : $verdict - $why" -ForegroundColor $(if ($verdict -like "PASS*") { "Green" } else { "Red" })
        if ($verdict -like "PASS*") { $exitCode = 0 }
        }
    }
} finally {
    # ------------------------------------------------ 4. always clean up --
    if (-not $KeepDatapack) {
        Remove-Item -LiteralPath $dp -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host " [clean] removed the throw-away datapack"
    } else {
        Write-Host " [keep]  datapack left in place: $dp"
    }
}

exit $exitCode
