param(
    [string]$BaseJar = (Join-Path $env:USERPROFILE 'Downloads/BetterMyau-4.6.1-debug(1).jar'),
    [string]$MinecraftSrg = (Join-Path $env:USERPROFILE 'Downloads/redo/BetterMyau-1.0.0-decompiled/build/libs/mc-srg.jar'),
    [string]$Libraries = "$env:APPDATA/PrismLauncher/libraries",
    [string]$Jdk = (Join-Path $env:ProgramFiles 'BellSoft/LibericaJDK-8/bin')
)

$ErrorActionPreference = 'Stop'
$BaseJar = (Resolve-Path -LiteralPath $BaseJar).Path
$MinecraftSrg = (Resolve-Path -LiteralPath $MinecraftSrg).Path
$dependencies = @(
    'com/google/code/gson/gson/2.2.4/gson-2.2.4.jar'
    'com/google/guava/guava/17.0/guava-17.0.jar'
    'com/mojang/authlib/1.5.21/authlib-1.5.21.jar'
    'io/netty/netty-all/4.0.23.Final/netty-all-4.0.23.Final.jar'
    'net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar'
    'net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9-universal.jar'
    'org/apache/commons/commons-lang3/3.3.2/commons-lang3-3.3.2.jar'
    'org/apache/logging/log4j/log4j-api/2.0-beta9-fixed/log4j-api-2.0-beta9-fixed.jar'
    'org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar'
    'org/lwjgl/lwjgl/lwjgl_util/2.9.4-nightly-20150209/lwjgl_util-2.9.4-nightly-20150209.jar'
)
$classpath = @($BaseJar, $MinecraftSrg) + @($dependencies | ForEach-Object {
    (Resolve-Path -LiteralPath (Join-Path $Libraries $_)).Path
})

# Overlay the modified classes on the original archive.
$sources = @(
    'ScaffoldModule', 'HypixelScaffoldPacing', 'DisplaceModule',
    'modules/Scaffold', 'modules/scaffold/ScaffoldMinecraft',
    'RotationUpdateEvent', 'RotationState', 'PreWalkingUpdateEvent', 'mixin/MixinEntityPlayerSP',
    'FlyModule', 'mixin/AccessorC03PacketPlayer', 'mixin/MixinFlyPlayer',
    'HUDModule', 'TabGui', 'ModuleCategory', 'ClickGuiScreen', 'MyauForgeMod',
    'KillAuraModule', 'ModuleModeIds', 'AutoClickerModule', 'OutgoingBufferOwner',
    'NoSlowModule', 'NoSlowBlink', 'mixin/MixinNoSlowBlinkInput', 'mixin/MixinNoSlowBlinkController',
    'mixin/MixinNoSlowBlinkItemRenderer', 'mixin/MixinNoSlowBlinkRenderPlayer'
)

$out = Join-Path $PSScriptRoot 'build'
New-Item -ItemType Directory -Force -Path $out | Out-Null
$stage = Join-Path $out ([Guid]::NewGuid().ToString('N'))
$classes = Join-Path $stage 'classes'
$jarDir = Join-Path $stage 'jar'
$empty = Join-Path $stage 'sourcepath'
New-Item -ItemType Directory -Path $stage,$classes,$jarDir,$empty | Out-Null
try {
    $javaFiles = @($sources | ForEach-Object { Join-Path $PSScriptRoot "myau/$_.java" })
    & "$Jdk/javac.exe" -nowarn -encoding UTF-8 -source 8 -target 8 -proc:none `
        -cp ($classpath -join ';') -sourcepath $empty -d $classes @javaFiles
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::ExtractToDirectory($BaseJar, $jarDir)
    Copy-Item -Path (Join-Path $PSScriptRoot 'resources/*') -Destination $jarDir -Recurse -Force
    foreach ($source in $sources) {
        $base = Split-Path $source -Leaf
        $directory = Split-Path (Join-Path $jarDir "myau/$source")
        if (-not (Test-Path -LiteralPath $directory)) { continue }
        Get-ChildItem -LiteralPath $directory -Filter "$base*.class" |
            Where-Object { $_.BaseName -eq $base -or $_.BaseName.StartsWith("$base`$") } |
            Remove-Item -Force
    }
    Copy-Item -Recurse -Force (Join-Path $classes 'myau') $jarDir
    $archive = Join-Path $stage '5.2.jar'
    Push-Location $jarDir
    try {
        & "$Jdk/jar.exe" cfm $archive META-INF/MANIFEST.MF .
        if ($LASTEXITCODE -ne 0) { throw 'JAR packaging failed' }
    } finally {
        Pop-Location
    }
    $destination = Join-Path $out '5.2.jar'
    Move-Item -Force -LiteralPath $archive -Destination $destination
    Write-Output "Built $destination"
} finally {
    $resolved = (Resolve-Path -LiteralPath $stage).Path
    $resolvedOut = (Resolve-Path -LiteralPath $out).Path
    if ($resolved.StartsWith($resolvedOut + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
