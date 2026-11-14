@echo off
rem AiCoder Assist: finds the running Minecraft and starts the injector with the game's own Java.
rem This does the same as AiCoderAssist.exe, as a plain script you can read.
setlocal
set "JAR=%~dp0AiCoderAssist.jar"
echo AiCoder Assist for Minecraft 1.21.11
echo.
powershell -NoProfile -Command "$p = Get-Process java,javaw -ErrorAction SilentlyContinue; if (-not $p) { Write-Host 'Minecraft is not running. Start Minecraft 1.21.11 first, then run this again.'; exit 2 }; $ids = @($p | ForEach-Object { [string]$_.Id }); $code = 3; foreach ($d in ($p | Where-Object { $_.Path } | ForEach-Object { Split-Path $_.Path } | Select-Object -Unique)) { $j = Join-Path $d 'java.exe'; if (Test-Path $j) { & $j -jar $env:JAR @ids; $code = $LASTEXITCODE; if ($code -eq 0) { break } } }; if ($code -eq 3) { Write-Host 'Found a running Java program, but could not start its java.exe.' }; exit $code"
echo.
pause
