[CmdletBinding()]
param(
    [ValidateSet("test", "fresh", "package", "all")]
    [string]$Action = "test",
    [string]$Version = "0.1.0"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$projectPorts = 8080, 5175, 5176, 8848
$startedProcesses = @()

function Invoke-NativeCommand {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$ArgumentList,
        [string]$WorkingDirectory = $root
    )

    Write-Host "==> $FilePath $($ArgumentList -join ' ')" -ForegroundColor Cyan
    $previousLocation = Get-Location
    try {
        Set-Location -LiteralPath $WorkingDirectory
        & $FilePath @ArgumentList
        if ($LASTEXITCODE -ne 0) {
            throw "Command failed with exit code ${LASTEXITCODE}: $FilePath"
        }
    }
    finally {
        Set-Location -LiteralPath $previousLocation
    }
}

function Assert-Command {
    param([Parameter(Mandatory = $true)][string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $Name"
    }
}

function Stop-ProjectPortOwners {
    foreach ($port in $projectPorts) {
        $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        foreach ($connection in $connections) {
            $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($connection.OwningProcess)" -ErrorAction SilentlyContinue
            Write-Host "==> Port $port owned by PID $($connection.OwningProcess) ($($process.Name)); stopping it" -ForegroundColor Yellow
            Stop-Process -Id $connection.OwningProcess -Force -ErrorAction SilentlyContinue
        }
    }
    Start-Sleep -Seconds 1
}

function Start-BackgroundProcess {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$ArgumentList,
        [Parameter(Mandatory = $true)][string]$WorkingDirectory
    )

    $logDirectory = Join-Path ([System.IO.Path]::GetTempPath()) "freshmeal-release"
    New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
    $stdout = Join-Path $logDirectory "$Name.out.log"
    $stderr = Join-Path $logDirectory "$Name.err.log"
    $process = Start-Process `
        -FilePath $FilePath `
        -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -PassThru
    $startedProcesses += [pscustomobject]@{
        Name = $Name
        Process = $process
        Stdout = $stdout
        Stderr = $stderr
    }
    return $process
}

function Wait-HttpEndpoint {
    param(
        [Parameter(Mandatory = $true)][string]$Uri,
        [int]$TimeoutSeconds = 90
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                return $response
            }
        }
        catch {
            Start-Sleep -Milliseconds 500
        }
    }
    throw "Timed out waiting for $Uri"
}

function Read-ResponseBody {
    param([Parameter(Mandatory = $true)]$Response)

    return $Response.Content | ConvertFrom-Json
}

function Assert-BusinessSuccess {
    param(
        [Parameter(Mandatory = $true)]$Body,
        [Parameter(Mandatory = $true)][string]$Label
    )

    if ($Body.code -ne 200) {
        throw "$Label returned business code $($Body.code): $($Body.message)"
    }
    return $Body.data
}

function Invoke-ReleaseTests {
    Assert-Command "mvn"
    Assert-Command "pnpm"

    Invoke-NativeCommand -FilePath "mvn" -ArgumentList @("test") -WorkingDirectory (Join-Path $root "server")
    Invoke-NativeCommand -FilePath "pnpm" -ArgumentList @("check") -WorkingDirectory (Join-Path $root "web-shop")
    Invoke-NativeCommand -FilePath "pnpm" -ArgumentList @("check") -WorkingDirectory (Join-Path $root "web-rider")
    Invoke-NativeCommand -FilePath "pnpm" -ArgumentList @("test") -WorkingDirectory (Join-Path $root "web-admin")
    Invoke-NativeCommand -FilePath "pnpm" -ArgumentList @("typecheck") -WorkingDirectory (Join-Path $root "web-admin")
    Invoke-NativeCommand -FilePath "pnpm" -ArgumentList @("build") -WorkingDirectory (Join-Path $root "web-admin")
}

function Invoke-FreshEnvironmentCheck {
    Assert-Command "mvn"
    Assert-Command "pnpm"
    Assert-Command "mysql"

    Stop-ProjectPortOwners

    $databaseName = "freshmeal_release_" + (Get-Date -Format "yyyyMMddHHmmss")
    $mysqlPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "root" }
    $previousMysqlPassword = $env:MYSQL_PWD
    $previousDatabaseUrl = $env:DB_URL
    $previousJwtSecret = $env:JWT_SECRET_KEY

    try {
        $env:MYSQL_PWD = $mysqlPassword
        Invoke-NativeCommand -FilePath "mysql" -ArgumentList @(
            "-h127.0.0.1", "-P3306", "-uroot",
            "-e", "CREATE DATABASE $databaseName DEFAULT CHARACTER SET utf8mb4;"
        )

        $env:DB_URL = "jdbc:mysql://127.0.0.1:3306/$databaseName`?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
        $env:JWT_SECRET_KEY = [Guid]::NewGuid().ToString("N") + [Guid]::NewGuid().ToString("N")

        Invoke-NativeCommand -FilePath "mvn" -ArgumentList @("test") -WorkingDirectory (Join-Path $root "server")

        Start-BackgroundProcess `
            -Name "server" `
            -FilePath "mvn.cmd" `
            -ArgumentList @("spring-boot:run") `
            -WorkingDirectory (Join-Path $root "server") | Out-Null
        Wait-HttpEndpoint -Uri "http://127.0.0.1:8080/api/shop/product/page?current=1&size=1" | Out-Null

        Start-BackgroundProcess `
            -Name "web-shop" `
            -FilePath "pnpm.cmd" `
            -ArgumentList @("dev") `
            -WorkingDirectory (Join-Path $root "web-shop") | Out-Null
        Start-BackgroundProcess `
            -Name "web-rider" `
            -FilePath "pnpm.cmd" `
            -ArgumentList @("dev") `
            -WorkingDirectory (Join-Path $root "web-rider") | Out-Null
        Start-BackgroundProcess `
            -Name "web-admin" `
            -FilePath "pnpm.cmd" `
            -ArgumentList @("dev") `
            -WorkingDirectory (Join-Path $root "web-admin") | Out-Null

        Wait-HttpEndpoint -Uri "http://127.0.0.1:5175/" | Out-Null
        Wait-HttpEndpoint -Uri "http://127.0.0.1:5176/" | Out-Null
        Wait-HttpEndpoint -Uri "http://127.0.0.1:8848/" | Out-Null

        $productResponse = Invoke-WebRequest `
            -Uri "http://127.0.0.1:8080/api/shop/product/page?current=1&size=1" `
            -UseBasicParsing
        $products = Assert-BusinessSuccess -Body (Read-ResponseBody $productResponse) -Label "Product list"
        if ($products.total -lt 1) {
            throw "Fresh database does not contain seed products"
        }

        $logins = @(
            @{ Label = "shop"; Uri = "http://127.0.0.1:8080/api/shop/auth/login"; Body = '{"username":"user01","password":"123456"}' },
            @{ Label = "admin"; Uri = "http://127.0.0.1:8080/api/admin/auth/login"; Body = '{"username":"admin","password":"123456"}' },
            @{ Label = "rider"; Uri = "http://127.0.0.1:8080/api/rider/auth/login"; Body = '{"username":"rider01","password":"123456"}' }
        )
        foreach ($login in $logins) {
            $response = Invoke-WebRequest -Uri $login.Uri -Method Post -ContentType "application/json" -Body $login.Body -UseBasicParsing
            $data = Assert-BusinessSuccess -Body (Read-ResponseBody $response) -Label "$($login.Label) login"
            if ([string]::IsNullOrWhiteSpace($data.accessToken)) {
                throw "$($login.Label) login did not return an access token"
            }
        }

        Write-Host "==> Fresh environment verification passed for database $databaseName" -ForegroundColor Green
    }
    finally {
        foreach ($entry in ($startedProcesses | Sort-Object { $_.Process.StartTime } -Descending)) {
            if (-not $entry.Process.HasExited) {
                & taskkill.exe /PID $entry.Process.Id /T /F 2>$null | Out-Null
            }
        }
        $script:startedProcesses = @()
        Stop-ProjectPortOwners

        if ($databaseName) {
            Invoke-NativeCommand -FilePath "mysql" -ArgumentList @(
                "-h127.0.0.1", "-P3306", "-uroot",
                "-e", "DROP DATABASE IF EXISTS $databaseName;"
            )
        }
        $env:MYSQL_PWD = $previousMysqlPassword
        $env:DB_URL = $previousDatabaseUrl
        $env:JWT_SECRET_KEY = $previousJwtSecret
    }
}

function New-SourcePackage {
    Assert-Command "git"
    Assert-Command "7z"

    $releaseDirectory = Join-Path $root "release"
    New-Item -ItemType Directory -Path $releaseDirectory -Force | Out-Null
    $outputPath = Join-Path $releaseDirectory "freshmeal-$Version-src.zip"
    if (Test-Path -LiteralPath $outputPath) {
        Remove-Item -LiteralPath $outputPath -Force
    }

    $listFile = Join-Path ([System.IO.Path]::GetTempPath()) "freshmeal-release-files.txt"
    $previousLocation = Get-Location
    try {
        Set-Location -LiteralPath $root
        $files = & git -c core.quotepath=false ls-files --cached --others --exclude-standard
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to enumerate release files"
        }
        $files | Set-Content -LiteralPath $listFile -Encoding UTF8
        Invoke-NativeCommand -FilePath "7z" -ArgumentList @(
            "a", "-tzip", "-mx=9", "-scsUTF-8", $outputPath, "@$listFile"
        )
    }
    finally {
        Set-Location -LiteralPath $previousLocation
    }

    $extractDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("freshmeal-release-verify-" + [Guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Path $extractDirectory -Force | Out-Null
    try {
        Invoke-NativeCommand -FilePath "7z" -ArgumentList @("x", $outputPath, "-o$extractDirectory", "-y")
        $forbiddenDirectories = @("node_modules", "target", "dist", ".idea", ".vscode", ".git", "release")
        $forbiddenFiles = @(".env", "*.local", "*.pem", "*.key", "id_rsa", "application-local.yml")
        $violations = Get-ChildItem -LiteralPath $extractDirectory -Recurse -Force | Where-Object {
            $relativePath = $_.FullName.Substring($extractDirectory.Length).TrimStart("\", "/").Replace("\", "/")
            $segments = $relativePath -split "/"
            $directoryViolation = $false
            foreach ($segment in $segments[0..([Math]::Max(0, $segments.Count - 2))]) {
                if ($forbiddenDirectories -contains $segment) {
                    $directoryViolation = $true
                    break
                }
            }
            $fileViolation = $false
            if (-not $_.PSIsContainer) {
                foreach ($pattern in $forbiddenFiles) {
                    if ($_.Name -like $pattern) {
                        $fileViolation = $true
                        break
                    }
                }
            }
            $directoryViolation -or $fileViolation
        }
        if ($violations) {
            throw ("Release package contains forbidden paths: " + (($violations | ForEach-Object FullName) -join ", "))
        }
    }
    finally {
        Remove-Item -LiteralPath $extractDirectory -Recurse -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $listFile -Force -ErrorAction SilentlyContinue
    }

    $hash = (Get-FileHash -LiteralPath $outputPath -Algorithm SHA256).Hash.ToLowerInvariant()
    $size = (Get-Item -LiteralPath $outputPath).Length
    Write-Host "==> Created $outputPath ($size bytes, sha256=$hash)" -ForegroundColor Green
}

switch ($Action) {
    "test" { Invoke-ReleaseTests }
    "fresh" { Invoke-FreshEnvironmentCheck }
    "package" { New-SourcePackage }
    "all" {
        Invoke-ReleaseTests
        Invoke-FreshEnvironmentCheck
        New-SourcePackage
    }
}
