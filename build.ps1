# SPDX-License-Identifier: GPL-3.0-only
[CmdletBinding()]
param(
 [string]$AndroidSdk = $env:ANDROID_SDK_ROOT,
 [string]$BuildToolsVersion = '35.0.0',
 [string]$JavaHome = $env:JAVA_HOME,
 [string]$Toolchain,
 [string]$SigningKey,
 [string]$LocalKeyDirectory,
 [string]$KeyAlias = 'raven-local',
 [ValidateSet('app','one','two','generic','reserve','generic-split','setup')][string]$Target = 'app',
 [string]$BuildDirectory,
 [string]$OutputDirectory,
 [switch]$PublishFinal,
 [string]$FinalApkDirectory='C:\APK-Workbench\Final apks'
)
$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
if($PublishFinal -and $Target -ne 'app'){throw 'Only the app target can be published as a final APK.'}
function Invoke-RavenTool {
 param([string]$Program,[string[]]$ToolArguments)
 & $Program @ToolArguments
 if ($LASTEXITCODE -ne 0) { throw "Build tool failed: $([IO.Path]::GetFileName($Program)) (exit $LASTEXITCODE)" }
}
if ($Toolchain) {
 $tools = Join-Path $Toolchain 'Android-Build-Tools'
 $platform = Join-Path $Toolchain 'Android-SDK/platforms/android-35/android.jar'
 if (!$JavaHome) { $JavaHome = Join-Path $Toolchain 'Java' }
} else {
 if (!$AndroidSdk) { $AndroidSdk = $env:ANDROID_HOME }
 if (!$AndroidSdk) { throw 'Set ANDROID_SDK_ROOT or pass -AndroidSdk / -Toolchain. See docs/BUILD.md.' }
 $tools = Join-Path $AndroidSdk "build-tools/$BuildToolsVersion"
 $platform = Join-Path $AndroidSdk 'platforms/android-35/android.jar'
}
if (!$JavaHome) { $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue; if ($compiler) { $JavaHome = Split-Path (Split-Path $compiler.Source) } }
if (!$JavaHome) { throw 'Set JAVA_HOME or pass -JavaHome pointing to a JDK.' }
$javaBin = Join-Path $JavaHome 'bin'
foreach ($file in @($platform,(Join-Path $javaBin 'javac.exe'),(Join-Path $javaBin 'jar.exe'),(Join-Path $javaBin 'keytool.exe'),(Join-Path $tools 'aapt.exe'),(Join-Path $tools 'd8.bat'),(Join-Path $tools 'zipalign.exe'),(Join-Path $tools 'apksigner.bat'))) {
 if (!(Test-Path -LiteralPath $file -PathType Leaf)) { throw "Required tool missing: $file" }
}
if (!$BuildDirectory) { $BuildDirectory = Join-Path $project '.build' }
if (!$OutputDirectory) { $OutputDirectory = Join-Path $project 'dist' }
$build = Join-Path $BuildDirectory ([Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path (Join-Path $build 'classes'),(Join-Path $build 'dex'),$OutputDirectory | Out-Null
$previousJavaHome = $env:JAVA_HOME
$previousStorePass = $env:RAVEN_STORE_PASS
$previousKeyPass = $env:RAVEN_KEY_PASS
try {
 $env:JAVA_HOME = $JavaHome
 if (!$SigningKey) {
  $keyFolder = if ($LocalKeyDirectory) { $LocalKeyDirectory } else { Join-Path $project '.local' }
  New-Item -ItemType Directory -Force -Path $keyFolder | Out-Null
  $SigningKey = Join-Path $keyFolder 'raven-local.jks'
  $KeyAlias = 'raven-local'
  if (!(Test-Path -LiteralPath $SigningKey)) {
   Invoke-RavenTool -Program (Join-Path $javaBin 'keytool.exe') -ToolArguments @('-genkeypair','-keystore',$SigningKey,'-alias',$KeyAlias,'-storepass','android','-keypass','android','-keyalg','RSA','-keysize','3072','-validity','3650','-dname','CN=Raven Backup Local Build','-noprompt')
  }
  $env:RAVEN_STORE_PASS = 'android'; $env:RAVEN_KEY_PASS = 'android'
  Write-Output 'Using local development signing. This APK cannot update the public release.'
 } else {
  if (!(Test-Path -LiteralPath $SigningKey -PathType Leaf)) { throw 'Signing keystore not found.' }
  if (!$env:RAVEN_STORE_PASS) { throw 'Set RAVEN_STORE_PASS in the process environment for the private keystore.' }
  if (!$env:RAVEN_KEY_PASS) { $env:RAVEN_KEY_PASS = $env:RAVEN_STORE_PASS }
 }
 $isApp = $Target -eq 'app'; $isSplit = $Target -eq 'generic-split'
 if ($isApp) {
  # Build the disposable setup app with this build's signing identity, including local builds.
  $helperOutput = Join-Path $build 'helper'
  $helperArgs = @{ Target='setup'; JavaHome=$JavaHome; SigningKey=$SigningKey; KeyAlias=$KeyAlias; BuildDirectory=$BuildDirectory; OutputDirectory=$helperOutput; BuildToolsVersion=$BuildToolsVersion }
  if ($Toolchain) { $helperArgs.Toolchain=$Toolchain } else { $helperArgs.AndroidSdk=$AndroidSdk }
  & $PSCommandPath @helperArgs
  New-Item -ItemType Directory -Force -Path (Join-Path $build 'assets') | Out-Null
  Copy-Item -LiteralPath (Join-Path $helperOutput 'fixture-setup.apk') -Destination (Join-Path $build 'assets/restore-check.apk')
 }
 $manifest = if ($isApp) { Join-Path $project 'AndroidManifest.xml' } else { Join-Path $project "tests/device/fixtures/$Target/AndroidManifest.xml" }
 if (!$isSplit) {
  $sources = if ($isApp) { @(Get-ChildItem -LiteralPath (Join-Path $project 'src') -Recurse -Filter *.java | ForEach-Object FullName) } else { @(Join-Path $project 'tests/device/fixtures/Seed.java') }
  $compileArguments = @('-encoding','UTF-8','-source','8','-target','8','-classpath',$platform,'-d',(Join-Path $build 'classes')) + $sources
  Invoke-RavenTool -Program (Join-Path $javaBin 'javac.exe') -ToolArguments $compileArguments
 }
 $unsigned = Join-Path $build 'unsigned.apk'
 $resourceArguments = @('package','-f','-M',$manifest,'-I',$platform,'-F',$unsigned)
 if ($isApp) { $resourceArguments += @('-S',(Join-Path $project 'res'),'-A',(Join-Path $build 'assets')) }
 Invoke-RavenTool -Program (Join-Path $tools 'aapt.exe') -ToolArguments $resourceArguments
 if (!$isSplit) {
  $classesJar = Join-Path $build 'classes.jar'
  Invoke-RavenTool -Program (Join-Path $javaBin 'jar.exe') -ToolArguments @('cf',$classesJar,'-C',(Join-Path $build 'classes'),'.')
  Invoke-RavenTool -Program (Join-Path $tools 'd8.bat') -ToolArguments @('--min-api','30','--lib',$platform,'--output',(Join-Path $build 'dex'),$classesJar)
  Push-Location (Join-Path $build 'dex')
  try { Invoke-RavenTool -Program (Join-Path $tools 'aapt.exe') -ToolArguments @('add',$unsigned,'classes.dex') } finally { Pop-Location }
 }
 $aligned = Join-Path $build 'aligned.apk'
 Invoke-RavenTool -Program (Join-Path $tools 'zipalign.exe') -ToolArguments @('-f','4',$unsigned,$aligned)
 $name = if ($isApp) { 'RavenBackup-local.apk' } else { "fixture-$Target.apk" }
 $output = Join-Path $OutputDirectory $name
 Invoke-RavenTool -Program (Join-Path $tools 'apksigner.bat') -ToolArguments @('sign','--ks',$SigningKey,'--ks-key-alias',$KeyAlias,'--ks-pass','env:RAVEN_STORE_PASS','--key-pass','env:RAVEN_KEY_PASS','--out',$output,$aligned)
 Invoke-RavenTool -Program (Join-Path $tools 'apksigner.bat') -ToolArguments @('verify',$output)
 Write-Output "Built and signature-verified: $output"
 if($PublishFinal){
  & (Join-Path $project 'scripts/publish-apk.ps1') -Apk $output -FinalDirectory $FinalApkDirectory -BuildToolsDirectory $tools -JavaHome $JavaHome
 }
} finally {
 $env:JAVA_HOME = $previousJavaHome
 $env:RAVEN_STORE_PASS = $previousStorePass
 $env:RAVEN_KEY_PASS = $previousKeyPass
}
