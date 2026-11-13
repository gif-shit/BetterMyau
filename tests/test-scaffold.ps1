param(
    [Parameter(Mandatory=$true)][string]$MinecraftSrg,
    [Parameter(Mandatory=$true)][string]$Libraries,
    [string]$Jdk = 'C:/Program Files/Eclipse Adoptium/jdk-8.0.482.8-hotspot/bin'
)

. (Join-Path $PSScriptRoot 'test-fly.ps1') @PSBoundParameters
foreach ($test in @('ScaffoldCoreRegression', 'myau.ScaffoldIntegrationRegression')) {
    & "$Jdk/java.exe" -ea -cp ((@($out) + $classpath) -join ';') $test
    if ($LASTEXITCODE -ne 0) { throw "$test failed" }
}
