param(
    [Parameter(Mandatory=$true)][string]$MinecraftSrg,
    [Parameter(Mandatory=$true)][string]$Libraries,
    [string]$Jdk = 'C:/Program Files/Eclipse Adoptium/jdk-8.0.482.8-hotspot/bin'
)

. (Join-Path $PSScriptRoot 'test-trajectories.ps1') @PSBoundParameters
$sandbox = Join-Path $root ('build/codebase-tests-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $sandbox | Out-Null
Push-Location $sandbox
try {
    & "$Jdk/java.exe" -ea -cp ((@($out) + $classpath) -join ';') myau.CodebaseRegression
    if ($LASTEXITCODE -ne 0) { throw 'Codebase regression checks failed' }
    & "$Jdk/java.exe" -ea -cp ((@($out) + $classpath) -join ';') myau.Update60Regression
    if ($LASTEXITCODE -ne 0) { throw 'Update 6.0 regression checks failed' }
    & "$Jdk/java.exe" -ea -cp ((@($out) + $classpath) -join ';') myau.ClickGuiCategoriesRegression
    if ($LASTEXITCODE -ne 0) { throw 'ClickGUI category checks failed' }
    & "$Jdk/java.exe" -ea -cp ((@($out) + $classpath) -join ';') myau.ItemSpoofBlockRegression
    if ($LASTEXITCODE -ne 0) { throw 'Item-spoof visual block checks failed' }
} finally {
    Pop-Location
}
