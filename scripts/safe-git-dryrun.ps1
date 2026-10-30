<#
.SYNOPSIS
    Smart Campus Lost and Found - Safe Git Dry-Run Tool
.DESCRIPTION
    Performs a read-only, non-destructive audit and dry-run synchronization check:
    1. Verifies branch, clean index, and remote configuration.
    2. Gathers working tree status using NUL-delimited porcelain parsing (git status --porcelain=v1 -z).
    3. Evaluates candidates against strict allowlist (blocking scripts, config, gitignore, deletions, renames).
    4. Runs Java compiler verification (javac) strictly into bin/ without executing code.
    5. Stages candidates exclusively into an ISOLATED temporary Git index ($env:GIT_INDEX_FILE).
    6. Scans staged diff in the temporary index for credentials, secrets, and PII patterns without printing values.
    7. Checks remote ancestry fast-forward status read-only without fetching or pushing.
    8. Leaves the real Git index, working tree, branch, and database completely untouched.

    LIMITATION NOTICE: Regex-based secret scanning is a heuristic check and cannot guarantee
    the detection of all secrets (e.g. unknown token formats, custom encryption keys, or obfuscated
    passwords). In addition, standard try/finally blocks cannot guarantee cleanup in cases of hard
    process termination (e.g., kill -9 or taskkill /f) or system power loss.
#>

[CmdletBinding()]
param(
    [switch]$DryRunOnly = $true
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Define Paths
$RepoRoot = $PSScriptRoot
if (-not (Test-Path -LiteralPath (Join-Path $RepoRoot ".git"))) {
    $RepoRoot = (Get-Item -LiteralPath $PSScriptRoot).Parent.FullName
}

$JavacPath = "C:\Program Files\Java\jdk-26.0.1\bin\javac.exe"
$FlatLafJar = Join-Path $RepoRoot "lib\flatlaf-3.5.4.jar"
$MySqlJar = Join-Path $RepoRoot "lib\mysql-connector-j-8.4.0.jar"
$BinDir = Join-Path $RepoRoot "bin"
$SrcDir = Join-Path $RepoRoot "src"
$ExpectedBranch = "main"
$ExpectedRemoteUrl = "https://github.com/Abhi-Poddar1423/SmartCampusLostAndFound.git"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  SMART CAMPUS LOST AND FOUND - GIT SAFE DRY-RUN TOOL    " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. ACQUIRE NAMED MUTEX
$MutexName = "Local\SmartCampusLostAndFound_GitSyncMutex"
$CreatedNew = $false
$Mutex = New-Object System.Threading.Mutex($false, $MutexName, [ref]$CreatedNew)

$HasAcquired = $false
try {
    $HasAcquired = $Mutex.WaitOne(1000, $false)
    if (-not $HasAcquired) {
        Write-Error "Another sync or dry-run instance is currently running. Exiting."
        return
    }

    # 2. VALIDATE ENVIRONMENT & PRECONDITIONS
    Write-Host "[1/7] Inspecting environment and repository pre-conditions..." -ForegroundColor Yellow

    if (-not (Test-Path -LiteralPath $JavacPath)) {
        throw "JDK compiler not found at: $JavacPath"
    }
    if (-not (Test-Path -LiteralPath $FlatLafJar)) {
        throw "FlatLaf library not found at: $FlatLafJar"
    }
    if (-not (Test-Path -LiteralPath $MySqlJar)) {
        throw "MySQL Connector library not found at: $MySqlJar"
    }

    # Branch verification
    $CurrentBranch = (git -C $RepoRoot symbolic-ref --short HEAD)
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to determine current Git branch (detached HEAD or repository error)."
    }
    $CurrentBranch = $CurrentBranch.Trim()
    if ($CurrentBranch -ne $ExpectedBranch) {
        throw "Branch check failed. Expected '$ExpectedBranch', got '$CurrentBranch'."
    }

    # Remote verification
    $RemoteUrl = (git -C $RepoRoot config --get remote.origin.url)
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to get remote.origin.url."
    }
    $RemoteUrl = $RemoteUrl.Trim()
    if ($RemoteUrl -ne $ExpectedRemoteUrl) {
        throw "Remote check failed. Expected '$ExpectedRemoteUrl', got '$RemoteUrl'."
    }

    # Verify real index is completely clean of pre-staged files
    $StagedChanges = (git -C $RepoRoot diff --cached --name-only)
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to check real index diff."
    }
    if ($StagedChanges) {
        throw "Pre-staged changes exist in real Git index. Aborting to avoid altering user state."
    }

    Write-Host "      Pre-conditions verified: On branch '$CurrentBranch', remote valid, index clean." -ForegroundColor Green

    # 3. ROBUST NUL-DELIMITED STATUS PARSER
    Write-Host "[2/7] Parsing working tree status (NUL-delimited)..." -ForegroundColor Yellow

    $pinfo = New-Object System.Diagnostics.ProcessStartInfo
    $pinfo.FileName = "git"
    $pinfo.Arguments = "-C `"$RepoRoot`" status --porcelain=v1 -z"
    $pinfo.RedirectStandardOutput = $true
    $pinfo.UseShellExecute = $false
    $pinfo.CreateNoWindow = $true

    $p = [System.Diagnostics.Process]::Start($pinfo)
    $stdoutStream = $p.StandardOutput
    $ms = New-Object System.IO.MemoryStream
    $stdoutStream.BaseStream.CopyTo($ms)
    $p.WaitForExit()

    if ($p.ExitCode -ne 0) {
        throw "git status --porcelain=v1 -z failed with exit code $($p.ExitCode)"
    }

    $rawBytes = $ms.ToArray()
    $tokens = New-Object System.Collections.Generic.List[string]

    $start = 0
    for ($i = 0; $i -lt $rawBytes.Length; $i++) {
        if ($rawBytes[$i] -eq 0) {
            $len = $i - $start
            if ($len -gt 0) {
                $str = [System.Text.Encoding]::UTF8.GetString($rawBytes, $start, $len)
                $tokens.Add($str)
            }
            $start = $i + 1
        }
    }
    if ($start -lt $rawBytes.Length) {
        $len = $rawBytes.Length - $start
        if ($len -gt 0) {
            $str = [System.Text.Encoding]::UTF8.GetString($rawBytes, $start, $len)
            $tokens.Add($str)
        }
    }

    $Entries = @()
    $idx = 0
    while ($idx -lt $tokens.Count) {
        $tok = $tokens[$idx]
        if ($tok.Length -lt 3) {
            throw "Malformed status token encountered (length < 3): '$tok'"
        }
        $statusCode = $tok.Substring(0, 2)
        $filePath = $tok.Substring(3)
        $origPath = $null

        # If rename or copy in porcelain v1 -z, current token is "XY <newPath>" and next token is "<origPath>"
        if ($statusCode.StartsWith("R") -or $statusCode.StartsWith("C") -or $statusCode.EndsWith("R") -or $statusCode.EndsWith("C")) {
            $idx++
            if ($idx -ge $tokens.Count) {
                throw "Malformed rename/copy entry: missing original path token."
            }
            $origPath = $tokens[$idx]
        }

        $Entries += [PSCustomObject]@{
            StatusCode = $statusCode
            Path = $filePath
            OrigPath = $origPath
        }
        $idx++
    }

    Write-Host "      Working tree scan complete. Detected $($Entries.Count) changed entry/entries." -ForegroundColor Green

    if ($Entries.Count -eq 0) {
        Write-Host "      No working tree changes detected. Clean tree." -ForegroundColor Cyan
    }

    # 4. POLICY & ALLOWLIST ENFORCEMENT
    Write-Host "[3/7] Auditing changed files against allowlist & security rules..." -ForegroundColor Yellow

    $CandidatesToStage = @()
    $BlockedEntries = @()

    foreach ($entry in $Entries) {
        $p = $entry.Path.Replace("\", "/")
        $status = $entry.StatusCode

        # Check 1: Block any deletion or rename
        if ($status.Contains("D") -or $status.Contains("R")) {
            $BlockedEntries += [PSCustomObject]@{ Path = $p; Reason = "Deletions and renames require manual review." }
            continue
        }

        # Check 2: Block protected configuration and scripts explicitly
        if ($p -eq ".gitignore" -or $p -like "*.bat" -or $p -like "*.cmd" -or $p -like "*.ps1" -or $p -like "scripts/*" -or $p -like "config/*") {
            $BlockedEntries += [PSCustomObject]@{ Path = $p; Reason = "Script, .gitignore, or configuration file requires manual review." }
            continue
        }

        # Check 3: Strictly allow only approved paths
        $isJavaSource = $p -like "src/com/campus/lostfound/*.java" -or $p -like "src/com/campus/lostfound/**/*.java"
        $isDoc = $p -like "docs/*.md" -or $p -like "docs/**/*.md"
        $isResource = $p -like "resources/*" -or $p -like "resources/**/*"

        if ($isJavaSource -or $isDoc -or $isResource) {
            # File size check for resources using -LiteralPath
            $fullResourcePath = Join-Path $RepoRoot $entry.Path
            if ($isResource -and (Test-Path -LiteralPath $fullResourcePath)) {
                $fileItem = Get-Item -LiteralPath $fullResourcePath
                if ($fileItem.Length -gt 5MB) {
                    $BlockedEntries += [PSCustomObject]@{ Path = $p; Reason = "Resource file exceeds 5MB limit." }
                    continue
                }
            }
            $CandidatesToStage += $entry.Path
        } else {
            $BlockedEntries += [PSCustomObject]@{ Path = $p; Reason = "Path outside approved allowlist (src/, docs/, resources/)." }
        }
    }

    if ($BlockedEntries.Count -gt 0) {
        Write-Host "      [POLICY NOTICE] The following entries require manual review:" -ForegroundColor Yellow
        foreach ($b in $BlockedEntries) {
            Write-Host "        - $($b.Path) : $($b.Reason)" -ForegroundColor Yellow
        }
    }

    Write-Host "      Policy audit finished: $($CandidatesToStage.Count) candidate(s) allowable, $($BlockedEntries.Count) blocked." -ForegroundColor Green

    # 5. JAVA COMPILATION GATE
    Write-Host "[4/7] Checking Java compilation gate..." -ForegroundColor Yellow
    $JavaFiles = Get-ChildItem -LiteralPath $SrcDir -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
    if ($JavaFiles.Count -eq 0) {
        throw "No Java source files found in $SrcDir"
    }

    if (-not (Test-Path -LiteralPath $BinDir)) {
        New-Item -ItemType Directory -Path $BinDir | Out-Null
    }

    $cp = "$FlatLafJar;$MySqlJar;$BinDir"
    $tempSourcesList = $null
    try {
        $tempSourcesList = [System.IO.Path]::GetTempFileName()
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        $quotedJavaFiles = $JavaFiles | ForEach-Object { '"' + $_.Replace('\', '/') + '"' }
        [System.IO.File]::WriteAllLines($tempSourcesList, $quotedJavaFiles, $utf8NoBom)
        
        $pinfoJavac = New-Object System.Diagnostics.ProcessStartInfo
        $pinfoJavac.FileName = $JavacPath
        $pinfoJavac.Arguments = "-d `"$BinDir`" -cp `"$cp`" `@`"$tempSourcesList`""
        $pinfoJavac.RedirectStandardOutput = $true
        $pinfoJavac.RedirectStandardError = $true
        $pinfoJavac.UseShellExecute = $false
        $pJavac = [System.Diagnostics.Process]::Start($pinfoJavac)
        $javacErr = $pJavac.StandardError.ReadToEnd()
        $pJavac.WaitForExit()

        if ($pJavac.ExitCode -ne 0) {
            throw "Java compilation gate failed with exit code $($pJavac.ExitCode):`n$javacErr"
        }
        Write-Host "      Java compilation gate passed successfully (compiled into bin/)." -ForegroundColor Green
    } finally {
        if ($tempSourcesList -and (Test-Path -LiteralPath $tempSourcesList)) {
            Remove-Item -LiteralPath $tempSourcesList -Force -ErrorAction SilentlyContinue
        }
    }

    # 6. ISOLATED DRY-RUN STAGING & SECRET SCANNING
    Write-Host "[5/7] Executing isolated dry-run staging & secret scanning..." -ForegroundColor Yellow

    $OrigGitIndexSpecified = $false
    $OrigGitIndexValue = $null
    if (Test-Path env:GIT_INDEX_FILE) {
        $OrigGitIndexSpecified = $true
        $OrigGitIndexValue = $env:GIT_INDEX_FILE
    }

    $TempIndexFile = $null

    try {
        $TempIndexFile = [System.IO.Path]::GetTempFileName()
        $env:GIT_INDEX_FILE = $TempIndexFile

        # Initialize temp index safely from HEAD
        git -C $RepoRoot read-tree HEAD
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to initialize temporary Git index from HEAD (read-tree failed with exit code $LASTEXITCODE)."
        }

        if ($CandidatesToStage.Count -gt 0 -and $BlockedEntries.Count -eq 0) {
            # Stage candidates into isolated index
            foreach ($cand in $CandidatesToStage) {
                git -C $RepoRoot add -A -- $cand
                if ($LASTEXITCODE -ne 0) {
                    throw "Failed staging candidate '$cand' into temporary index."
                }
            }

            # Inspect staged diff in isolated index
            $stagedDiffLines = git -C $RepoRoot diff --cached --unified=0
            if ($LASTEXITCODE -ne 0) {
                throw "Failed to inspect diff in temporary index."
            }

            # Extended secret and PII detection patterns
            $SensitivePatterns = @(
                @{ Name = "Database Password Configuration"; Regex = '(?i)db\.password\s*=\s*[^\s]+' },
                @{ Name = "Generic Secret Assignment"; Regex = '(?i)(?:password|passwd|pwd|secret|private_key|token|api_key|apikey)\s*[:=]\s*["''][^"'']{4,}["'']' },
                @{ Name = "AWS Access Key Identifier"; Regex = '\b(?:AKIA|ABIA|ACCA|ASIA)[0-9A-Z]{16}\b' },
                @{ Name = "Private Key Header"; Regex = '-----BEGIN (?:RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----' },
                @{ Name = "GitHub Personal Access Token"; Regex = '\bgh[pousr]_[A-Za-z0-9_]{36,255}\b' },
                @{ Name = "Slack Token"; Regex = '\bxox[baprs]-[0-9a-zA-Z-]{10,72}\b' },
                @{ Name = "Email Address (PII Detection)"; Regex = '\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b' }
            )

            $Findings = @()
            try {
                foreach ($line in $stagedDiffLines) {
                    if ($line.StartsWith("+") -and -not $line.StartsWith("+++")) {
                        $addedContent = $line.Substring(1)
                        foreach ($rule in $SensitivePatterns) {
                            if ($addedContent -match $rule.Regex) {
                                $Findings += [PSCustomObject]@{
                                    Rule = $rule.Name
                                    # Strict Rule: NEVER log, print, or assign the matched secret string
                                }
                            }
                        }
                    }
                }
            } catch {
                # Fail closed on scanner error
                throw "Secret scanner encountered an internal error during diff inspection: $_"
            }

            if ($Findings.Count -gt 0) {
                Write-Host "      [SECRET GATE FAILED] Sensitive data or PII pattern detected:" -ForegroundColor Red
                foreach ($f in $Findings) {
                    Write-Host "        - Rule triggered: $($f.Rule)" -ForegroundColor Red
                }
                Write-Host "      Dry-run rejected staging. Real index remains pristine." -ForegroundColor Red
            } else {
                Write-Host "      Secret & PII scan clean: Zero sensitive patterns detected in candidate diff." -ForegroundColor Green
            }
        } else {
            Write-Host "      No staging executed (either 0 candidates or blocked entries present). Isolated index clean." -ForegroundColor Green
        }
    } finally {
        # GUARANTEED RESTORATION OF GIT_INDEX_FILE AND TEMP FILE REMOVAL
        if ($OrigGitIndexSpecified) {
            $env:GIT_INDEX_FILE = $OrigGitIndexValue
        } else {
            Remove-Item env:GIT_INDEX_FILE -ErrorAction SilentlyContinue
        }

        if ($TempIndexFile -and (Test-Path -LiteralPath $TempIndexFile)) {
            Remove-Item -LiteralPath $TempIndexFile -Force -ErrorAction SilentlyContinue
        }
    }

    # 7. REMOTE ANCESTRY CHECK (READ-ONLY, NO AUTO-FETCH)
    Write-Host "[6/7] Checking remote tracking status..." -ForegroundColor Yellow
    
    $LocalCommit = (git -C $RepoRoot rev-parse HEAD)
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to resolve HEAD commit."
    }
    $LocalCommit = $LocalCommit.Trim()

    # Check whether the remote tracking ref exists without throwing an unhandled exception
    $null = git -C $RepoRoot rev-parse --verify --quiet "origin/$ExpectedBranch"
    $trackingRefExists = ($LASTEXITCODE -eq 0)

    if (-not $trackingRefExists) {
        Write-Host "      [NOTICE] Remote tracking ref 'origin/$ExpectedBranch' does not exist locally." -ForegroundColor Yellow
        Write-Host "      Ancestry cannot be verified automatically. Automatic fetch is prohibited." -ForegroundColor Yellow
    } else {
        $RemoteTrackingCommit = (git -C $RepoRoot rev-parse "origin/$ExpectedBranch")
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to resolve tracking ref commit."
        }
        $RemoteTrackingCommit = $RemoteTrackingCommit.Trim()

        if ($LocalCommit -eq $RemoteTrackingCommit) {
            Write-Host "      Local HEAD matches origin/$ExpectedBranch ($LocalCommit)." -ForegroundColor Green
        } else {
            git -C $RepoRoot merge-base --is-ancestor $RemoteTrackingCommit HEAD
            $isAncestorExit = $LASTEXITCODE
            if ($isAncestorExit -eq 0) {
                Write-Host "      Local HEAD is ahead of origin/$ExpectedBranch and fast-forwardable." -ForegroundColor Green
            } elseif ($isAncestorExit -eq 1) {
                Write-Host "      [WARNING] Local branch has diverged from origin/$ExpectedBranch or remote has new commits." -ForegroundColor Red
                Write-Host "      Ancestry is not established; manual git review required before any push." -ForegroundColor Red
            } else {
                throw "git merge-base failed with exit code $isAncestorExit."
            }
        }
    }

    # 8. SUMMARY & INVARIANT CHECKS
    Write-Host "[7/7] Verifying safety guarantees and real index integrity..." -ForegroundColor Yellow
    $RealIndexStagedAfter = (git -C $RepoRoot diff --cached --name-only)
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to verify real index integrity."
    }
    if ($RealIndexStagedAfter) {
        Write-Error "CRITICAL: Real Git index was modified! ($RealIndexStagedAfter)"
    } else {
        Write-Host "      Real Git index verified pristine (0 staged changes)." -ForegroundColor Green
    }

    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host "  DRY-RUN COMPLETE: NO FILES COMMITTED, PUSHED, OR ALTERED" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green

} finally {
    if ($HasAcquired) {
        $Mutex.ReleaseMutex()
    }
    $Mutex.Dispose()
}
