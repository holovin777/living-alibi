# Dot-source from play.ps1. No elevation, registry edits or PATH modifications.
function Initialize-LivingAlibiVoice {
    $cache = Join-Path $env:LOCALAPPDATA 'LivingAlibi\voice'
    $model = $env:LIVING_ALIBI_WHISPER_MODEL
    if (-not $model) { $model = Join-Path $cache 'ggml-base.bin' }
    $cli = $env:LIVING_ALIBI_WHISPER_CLI
    $installed = Get-Command whisper-cli.exe -ErrorAction SilentlyContinue
    if (-not $cli -and $installed) { $cli = $installed.Source }
    $arch = $env:PROCESSOR_ARCHITECTURE
    if ($env:PROCESSOR_ARCHITEW6432) { $arch = $env:PROCESSOR_ARCHITEW6432 }
    $asset = switch ($arch) {
        'AMD64' { @('whisper-bin-x64.zip','6ba69e3482d7826214f90a6a9c84ca07782aec1e1d0c6a7c30c994fd5d816ccb') }
        'ARM64' { @('whisper-bin-win-cpu-arm64.zip','28c37e7b598c3d9bbfef94f3bd67f2ee6f12b7c86f3da5edcf5e4308d615ff6d') }
        default { $null }
    }
    $runtime = Join-Path $cache "whisper-b5454-$arch"
    if (-not $cli -and (Test-Path $runtime)) {
        $found = Get-ChildItem $runtime -Recurse -Filter whisper-cli.exe | Select-Object -First 1
        if ($found) { $cli = $found.FullName }
    }
    if (($cli -and (Test-Path -LiteralPath $cli -PathType Leaf)) -and (Test-Path -LiteralPath $model -PathType Leaf)) {
        $env:LIVING_ALIBI_WHISPER_CLI=$cli
        $env:LIVING_ALIBI_WHISPER_MODEL=$model
        return
    }
    if ((Read-Host 'Download optional offline voice components (~160 MB)? [y/N]') -notmatch '^[yY]$') { return }
    $temp = Join-Path $cache ([guid]::NewGuid().ToString())
    try {
        New-Item -ItemType Directory -Force -Path $temp | Out-Null
        if (-not $cli -or -not (Test-Path -LiteralPath $cli -PathType Leaf)) {
            if (-not $asset) { throw "Unsupported Windows architecture: $arch" }
            $zip = Join-Path $temp 'whisper.zip'
            Invoke-WebRequest -UseBasicParsing -Uri ("https://github.com/ggml-org/whisper.cpp/releases/download/b5454/" + $asset[0]) -OutFile $zip
            if ((Get-FileHash $zip -Algorithm SHA256).Hash -ne $asset[1]) { throw 'Whisper binary checksum mismatch.' }
            $expanded=Join-Path $temp 'expanded'
            Expand-Archive -LiteralPath $zip -DestinationPath $expanded
            $found=Get-ChildItem $expanded -Recurse -Filter whisper-cli.exe | Select-Object -First 1
            if (-not $found) { throw 'Missing whisper-cli.exe in archive.' }
            # Keep adjacent DLLs and never replace an existing runtime in use.
            if (Test-Path $runtime) { throw 'Existing incomplete runtime. Remove it manually before retrying.' }
            Move-Item -LiteralPath $expanded -Destination $runtime
            $cli=(Get-ChildItem $runtime -Recurse -Filter whisper-cli.exe | Select-Object -First 1).FullName
        }
        if (-not (Test-Path -LiteralPath $model -PathType Leaf)) {
            $part=Join-Path $temp 'ggml-base.bin'
            Invoke-WebRequest -UseBasicParsing -Uri 'https://huggingface.co/ggerganov/whisper.cpp/resolve/fb5ed9369c26eaf08392504d8446da431e0fffb9/ggml-base.bin' -OutFile $part
            if ((Get-FileHash $part -Algorithm SHA256).Hash -ne '60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe') { throw 'Model checksum mismatch.' }
            New-Item -ItemType Directory -Force -Path (Split-Path -Parent $model) | Out-Null
            Move-Item -LiteralPath $part -Destination $model
        }
        $env:LIVING_ALIBI_WHISPER_CLI=$cli
        $env:LIVING_ALIBI_WHISPER_MODEL=$model
        Write-Host 'Optional offline voice components ready.'
    } catch {
        Write-Warning "Voice setup skipped: $_. Typed dialogue remains available."
    } finally {
        if (Test-Path $temp) { Remove-Item -LiteralPath $temp -Recurse -Force }
    }
}
Initialize-LivingAlibiVoice
