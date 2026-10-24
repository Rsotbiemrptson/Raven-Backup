[CmdletBinding()]
param([Parameter(Mandatory)][string]$JsonJar,[string]$JavaHome=$env:JAVA_HOME,[string]$BuildDirectory)
$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot
if(!$JavaHome){throw 'Pass -JavaHome pointing to a JDK.'}
if((Get-FileHash -LiteralPath $JsonJar -Algorithm SHA256).Hash -ne '3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed'){throw 'Expected the unmodified org.json:json:20240303 JAR. See docs/BUILD.md.'}
if(!$BuildDirectory){$BuildDirectory=Join-Path $project '.build'}
$classes=Join-Path $BuildDirectory ('firmware-tests-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$sources=@('FirmwareCheck','FirmwarePolicy','MigrationPolicy') | ForEach-Object {Join-Path $project "src/dev/raven/backup/$_.java"}
$sources+=@(Get-ChildItem -LiteralPath (Join-Path $project 'tests/firmware') -Filter *.java | ForEach-Object FullName)
& (Join-Path $JavaHome 'bin/javac.exe') -encoding UTF-8 -cp $JsonJar -d $classes $sources
if($LASTEXITCODE){throw 'Firmware integration compilation failed.'}
& (Join-Path $JavaHome 'bin/java.exe') -cp ($classes+[IO.Path]::PathSeparator+(Resolve-Path -LiteralPath $JsonJar).Path) dev.raven.backup.FirmwareCheckIntegrationTest
if($LASTEXITCODE){throw 'Firmware integration checks failed.'}
