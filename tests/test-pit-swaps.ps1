param(
    [Parameter(Mandatory=$true)][string]$MinecraftSrg,
    [Parameter(Mandatory=$true)][string]$Libraries,
    [string]$Jdk = 'C:/Program Files/Eclipse Adoptium/jdk-8.0.482.8-hotspot/bin'
)

. (Join-Path $PSScriptRoot 'test-scaffold.ps1') @PSBoundParameters
foreach ($test in @('PitSwapChecks', 'myau.PitSwapIntegrationRegression')) {
    & "$Jdk/java.exe" -ea -cp ((@($out) + $classpath) -join ';') $test $MinecraftSrg (Join-Path $root 'build/bettermyau u6.1.jar')
    if ($LASTEXITCODE -ne 0) { throw "$test failed" }
}
