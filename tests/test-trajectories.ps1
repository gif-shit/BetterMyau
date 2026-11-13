param(
    [Parameter(Mandatory=$true)][string]$MinecraftSrg,
    [Parameter(Mandatory=$true)][string]$Libraries,
    [string]$Jdk = 'C:/Program Files/Eclipse Adoptium/jdk-8.0.482.8-hotspot/bin'
)

. (Join-Path $PSScriptRoot 'test-pit-swaps.ps1') @PSBoundParameters
& "$Jdk/java.exe" -ea -cp ((@($out) + $classpath) -join ';') myau.TrajectoriesRegression
if ($LASTEXITCODE -ne 0) { throw 'Trajectories regression checks failed' }
