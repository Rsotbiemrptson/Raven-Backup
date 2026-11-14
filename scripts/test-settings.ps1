# SPDX-License-Identifier: GPL-3.0-only
[CmdletBinding()]
param([Parameter(Mandatory)][string]$AppClasses,[Parameter(Mandatory)][string]$AndroidJar,[Parameter(Mandatory)][string]$JsonJar,[Parameter(Mandatory)][string]$JavaHome,[Parameter(Mandatory)][string]$OutputDirectory)
$ErrorActionPreference='Stop'
if((Get-FileHash -LiteralPath $JsonJar).Hash -ne '3CF6CD6892E32E2B4C1C39E0F52F5248A2F5B37646FDFBB79A66B46B618414ED'){throw 'Use the pinned org.json 20240303 JAR.'}
$root=Split-Path $PSScriptRoot
$destination=[IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $destination | Out-Null
& (Join-Path $JavaHome 'bin/javac.exe') -encoding UTF-8 -cp "$JsonJar;$AppClasses;$AndroidJar" -d $destination @((Join-Path $root 'tests/settings/SettingsTransferTest.java'),(Join-Path $root 'tests/settings/ExternalTarTest.java'))
if($LASTEXITCODE){throw 'Settings tests did not compile.'}
& (Join-Path $JavaHome 'bin/java.exe') -cp "$destination;$JsonJar;$AppClasses;$AndroidJar" dev.raven.backup.SettingsTransferTest
if($LASTEXITCODE){throw 'Settings validation tests failed.'}

& (Join-Path $JavaHome 'bin/java.exe') -cp "$destination;$JsonJar;$AppClasses;$AndroidJar" dev.raven.backup.ExternalTarTest $destination
if($LASTEXITCODE){throw 'External staging tests failed.'}
