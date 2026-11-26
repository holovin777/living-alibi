param([switch]$Check,[switch]$NoVoice)
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$server=$null
Push-Location $root
try {
    $java=(Get-Command java.exe -ErrorAction Stop).Source
    $maven=(Get-Command mvn.cmd -ErrorAction Stop).Source
    $godot=$env:LIVING_ALIBI_GODOT
    if (-not $godot) {
        $command=Get-Command godot.exe,godot4.exe -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($command) { $godot=$command.Source }
    }
    if (-not $godot -or -not (Test-Path -LiteralPath $godot -PathType Leaf)) {
        throw 'Install Godot 4 and set LIVING_ALIBI_GODOT to the executable path.'
    }
    if ($Check) {
        & $java -version
        & $godot --version
        Write-Host 'Java, Maven and Godot found.'
        return
    }
    $existing=$false
    try { Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 -Uri 'http://127.0.0.1:8080/api/v1/world' | Out-Null; $existing=$true } catch {}
    if ($existing) { throw 'Close the existing Living Alibi server before launching another.' }
    if (-not $NoVoice) { . (Join-Path $root 'setup-voice.ps1') }
    & $maven -q -DskipTests install
    if ($LASTEXITCODE -ne 0) { throw 'Engine build failed.' }
    & $maven -q -f server/pom.xml -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Server build failed.' }
    $jar=Join-Path $root 'server\target\living-alibi-server-1.16.0.jar'
    if (-not (Test-Path -LiteralPath $jar)) { throw 'Server jar not found.' }
    $data=Join-Path $root 'data'
    New-Item -ItemType Directory -Force -Path $data | Out-Null
    $save=Join-Path $data 'world.db'
    $server=Start-Process -FilePath $java -ArgumentList @('-jar',"`"$jar`"","--living-alibi.database=`"$save`"") -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $data 'server.log') -RedirectStandardError (Join-Path $data 'server-error.log')
    $ready=$false
    for ($i=0;$i -lt 60;$i++) {
        if ($server.HasExited) { break }
        try {
            Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 -Uri 'http://127.0.0.1:8080/api/v1/world' | Out-Null
            $ready=$true
            break
        } catch { Start-Sleep -Seconds 1 }
    }
    if (-not $ready) { throw 'Server startup failed; see data/server.log and server-error.log.' }
    $project=Join-Path $root 'godot'
    if (-not (Test-Path (Join-Path $project '.godot'))) {
        Start-Process -FilePath $godot -ArgumentList @('--headless','--path',"`"$project`"",'--editor','--quit') -Wait
    }
    Start-Process -FilePath $godot -ArgumentList @('--path',"`"$project`"") -Wait
} finally {
    if ($server -and -not $server.HasExited) { Stop-Process -Id $server.Id -ErrorAction SilentlyContinue }
    Pop-Location
}
