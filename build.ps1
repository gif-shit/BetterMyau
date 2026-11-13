param(
    [Parameter(Mandatory=$true)][string]$BaseJar,
    [Parameter(Mandatory=$true)][string]$MinecraftSrg,
    [string]$Libraries = "$env:APPDATA/.minecraft/libraries",
    [string]$Jdk = $env:JAVA_HOME
)

$ErrorActionPreference = 'Stop'
$arguments = @((Join-Path $PSScriptRoot 'build.py'), '--base-jar', $BaseJar, '--minecraft-srg', $MinecraftSrg, '--libraries', $Libraries)
if ($Jdk) { $arguments += @('--jdk', $Jdk) }
& python @arguments
if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
