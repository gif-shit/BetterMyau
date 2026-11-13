param(
    [Parameter(Mandatory=$true)][string]$MinecraftSrg,
    [Parameter(Mandatory=$true)][string]$Libraries,
    [string]$Jdk = 'C:/Program Files/Eclipse Adoptium/jdk-8.0.482.8-hotspot/bin'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$out = Join-Path $root 'build/fly-tests'
New-Item -ItemType Directory -Force -Path $out | Out-Null
$dependencies = Get-ChildItem -LiteralPath $Libraries -Recurse -Filter '*.jar' |
    Where-Object { $_.Name -match '^(gson-|guava-|authlib-|netty-all-|launchwrapper-|forge-1\.8\.9-.*-universal|commons-lang3-|commons-io-|log4j-(api|core)-|lwjgl(-|_util-))' } |
    Group-Object Name | ForEach-Object { $_.Group[0].FullName }
$classpath = @((Join-Path $root 'build/bettermyau u6.1.jar'), $MinecraftSrg) + @($dependencies)
$sources = @(Get-ChildItem -LiteralPath $PSScriptRoot -Recurse -Filter '*.java' | ForEach-Object FullName)
& "$Jdk/javac.exe" -encoding UTF-8 -source 8 -target 8 -proc:none -cp ($classpath -join ';') -d $out @sources
if ($LASTEXITCODE -ne 0) { throw 'Fly test compilation failed' }
& "$Jdk/java.exe" -ea -cp ((@($out) + $classpath) -join ';') myau.FlyRegression
if ($LASTEXITCODE -ne 0) { throw 'Fly regression checks failed' }
& "$Jdk/java.exe" -ea -cp ((@($out) + $classpath) -join ';') myau.FlyHookCheck $MinecraftSrg (Join-Path $root 'build/bettermyau u6.1.jar')
if ($LASTEXITCODE -ne 0) { throw 'Fly packet hook checks failed' }
