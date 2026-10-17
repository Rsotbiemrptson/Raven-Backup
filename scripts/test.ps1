# SPDX-License-Identifier: GPL-3.0-only
[CmdletBinding()]
param([string]$JavaHome=$env:JAVA_HOME,[string]$BuildDirectory)
$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot
if (!$JavaHome) { $compiler=Get-Command javac.exe -ErrorAction SilentlyContinue; if ($compiler) { $JavaHome=Split-Path (Split-Path $compiler.Source) } }
if (!$JavaHome) { throw 'Set JAVA_HOME or pass -JavaHome pointing to a JDK.' }
if (!$BuildDirectory) { $BuildDirectory=Join-Path $project '.build' }
$build=Join-Path $BuildDirectory ('tests-'+[Guid]::NewGuid().ToString('N'))
$classes=Join-Path $build 'classes'
New-Item -ItemType Directory -Force -Path $classes,(Join-Path $build 'work') | Out-Null
$sources=@('TarGuard','SelinuxLabels','AppearanceColors','BackupNames','FirmwarePolicy') | ForEach-Object { Join-Path $project "src/dev/raven/backup/$_.java" }
$sources+=@(Get-ChildItem -LiteralPath (Join-Path $project 'tests/host') -Filter *.java | ForEach-Object FullName)
& (Join-Path $JavaHome 'bin/javac.exe') -encoding UTF-8 -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Host test compilation failed.' }
Push-Location $build
try {
 foreach ($test in @('TarGuardTest','SelinuxLabelsTest','AppearanceColorsTest','BackupNamesTest','FirmwarePolicyTest')) {
  & (Join-Path $JavaHome 'bin/java.exe') -cp $classes "dev.raven.backup.$test"
  if ($LASTEXITCODE -ne 0) { throw "Host test failed: $test" }
 }
} finally { Pop-Location }
Write-Output 'All five host checks passed. No device operations were performed.'
