param(
    [string]$Jdk = $env:JAVA_HOME,
    [string]$Sdk = (Join-Path $PSScriptRoot 'sdk\zorynth-plugin-api-1.jar')
)
$ErrorActionPreference = 'Stop'
if ($Jdk) {
    $compiler = Join-Path $Jdk 'bin\javac.exe'
    $archiver = Join-Path $Jdk 'bin\jar.exe'
} else {
    $compiler = (Get-Command javac.exe -ErrorAction Stop).Source
    $archiver = (Get-Command jar.exe -ErrorAction Stop).Source
}
if (!(Test-Path -LiteralPath $Sdk -PathType Leaf)) { throw "Plugin SDK not found: $Sdk" }
$output = Join-Path $PSScriptRoot 'build'
$classes = Join-Path $output ('classes-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src\main\java') -Recurse -Filter *.java | ForEach-Object FullName)
if (!$sources.Count) { throw 'No plugin Java sources found' }
& $compiler --release 25 -encoding UTF-8 -classpath $Sdk -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw "javac failed: $LASTEXITCODE" }
$destination = Join-Path $output 'aicoder-themes-1.0.0.jar'
& $archiver --create --file $destination -C $classes . -C (Join-Path $PSScriptRoot 'src\main\resources') .
if ($LASTEXITCODE -ne 0) { throw "jar failed: $LASTEXITCODE" }
Write-Output "Built $destination"
