@echo off
rem AiCoder Assist licence server (temporary, this computer only).
rem Looks for a Java to run it with: one on the PATH, the one a running Minecraft uses, or the official launcher's.
setlocal
cd /d "%~dp0"
echo AiCoder Assist licence server
echo.
powershell -NoProfile -Command "$j = $null; $c = Get-Command java.exe -ErrorAction SilentlyContinue; if ($c) { $j = $c.Source }; if (-not $j) { $p = Get-Process java,javaw -ErrorAction SilentlyContinue | Where-Object { $_.Path } | Select-Object -First 1; if ($p) { $j = Join-Path (Split-Path $p.Path) 'java.exe' } }; if (-not $j) { foreach ($d in @((Join-Path $env:APPDATA '.minecraft\runtime'), (Join-Path $env:LOCALAPPDATA 'Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local\runtime'))) { if (Test-Path $d) { $f = Get-ChildItem $d -Recurse -Filter java.exe -ErrorAction SilentlyContinue | Select-Object -First 1; if ($f) { $j = $f.FullName; break } } } }; if (-not $j -or -not (Test-Path $j)) { Write-Host 'No Java found. Start Minecraft first (its Java will be used), then run this again.'; exit 2 }; Write-Host ('Using ' + $j); & $j -jar LicenseServer.jar data 8765"
echo.
pause
