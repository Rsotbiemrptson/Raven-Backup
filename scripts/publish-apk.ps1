param(
 [Parameter(Mandatory)][string]$Apk,
 [string]$FinalDirectory='C:\APK-Workbench\Final apks',
 [string]$BuildToolsDirectory='C:\APK-Workbench\Toolchain\Android-Build-Tools',
 [string]$JavaHome='C:\APK-Workbench\Toolchain\Java'
)
$ErrorActionPreference='Stop'
$aapt=Join-Path $BuildToolsDirectory 'aapt.exe'
$java=Join-Path $JavaHome 'bin/java.exe'
$signer=Join-Path $BuildToolsDirectory 'lib/apksigner.jar'
function Read-Metadata([string]$Path) {
 $badging=& $aapt dump badging $Path 2>&1
 $badgingExit=$LASTEXITCODE
 $match=[regex]::Match(($badging -join "`n"),"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'")
 if(!$match.Success){throw "Missing APK identity: $Path"}
 # Some unrelated games have resources this aapt version cannot fully inspect.
 # Their emitted package identity is sufficient to leave those APKs untouched.
 if($badgingExit -and $match.Groups[1].Value -eq 'dev.raven.backup'){throw "Cannot fully inspect Raven APK metadata: $Path"}
 [pscustomobject]@{Path=$Path;Package=$match.Groups[1].Value;Code=[long]$match.Groups[2].Value;Version=$match.Groups[3].Value}
}
function Read-Certificate([string]$Path) {
 $result=& $java -jar $signer verify --print-certs $Path 2>&1
 if($LASTEXITCODE){throw "APK signature did not verify: $Path"}
 $match=[regex]::Match(($result -join "`n"),'certificate SHA-256 digest: ([0-9a-fA-F]+)')
 if(!$match.Success){throw 'Signing certificate unavailable.'};$match.Groups[1].Value.ToLowerInvariant()
}
$source=(Resolve-Path -LiteralPath $Apk).Path
$candidate=Read-Metadata $source
if($candidate.Package -ne 'dev.raven.backup'){throw 'Only Raven Backup releases may be published by this script.'}
$certificate=Read-Certificate $source
New-Item -ItemType Directory -Force -Path $FinalDirectory | Out-Null
$FinalDirectory=(Resolve-Path -LiteralPath $FinalDirectory).Path
if((Get-Item -LiteralPath $FinalDirectory).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Final directory must not be a reparse point.'}
$archive=Join-Path $FinalDirectory 'Archive'
if((Test-Path -LiteralPath $archive) -and ((Get-Item -LiteralPath $archive).Attributes -band [IO.FileAttributes]::ReparsePoint)){throw 'Archive must not be a reparse point.'}
$previous=@(Get-ChildItem -LiteralPath $FinalDirectory -File -Filter '*.apk' | ForEach-Object {Read-Metadata $_.FullName} | Where-Object Package -eq $candidate.Package)
foreach($old in $previous){
 if($old.Code -gt $candidate.Code){throw 'Refusing to publish an older version over a newer Raven release.'}
 if((Read-Certificate $old.Path) -ne $certificate){throw 'Signing certificate differs from the published Raven APK.'}
}
$safeVersion=$candidate.Version -replace '[^A-Za-z0-9._-]','_'
$destination=Join-Path $FinalDirectory "RavenBackup-$safeVersion.apk"
if((Test-Path -LiteralPath $destination) -and $destination -notin $previous.Path){throw 'Destination belongs to another application.'}
$hash=(Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash
if($previous.Count -eq 1 -and $previous[0].Path -eq $destination -and (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -eq $hash){Write-Output "Already published: $destination";return}
$stage=Join-Path $FinalDirectory ('.raven-publish-'+[guid]::NewGuid().ToString('N')+'.pending')
Copy-Item -LiteralPath $source -Destination $stage
if((Get-FileHash -LiteralPath $stage -Algorithm SHA256).Hash -ne $hash){throw 'Staged APK differs; existing releases were not moved.'}
New-Item -ItemType Directory -Force -Path $archive | Out-Null
foreach($old in $previous){
 $resolved=(Resolve-Path -LiteralPath $old.Path).Path
 if([IO.Path]::GetDirectoryName($resolved) -ne $FinalDirectory){throw 'APK archive source is outside FinalDirectory.'}
 $archived=Join-Path $archive ([IO.Path]::GetFileName($resolved))
 if((Test-Path -LiteralPath $archived) -or (Test-Path -LiteralPath ($archived+'.sha256'))){$dated=Join-Path $archive ((Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N'));New-Item -ItemType Directory -Path $dated | Out-Null;$archived=Join-Path $dated ([IO.Path]::GetFileName($resolved))}
 $oldHash=(Get-FileHash -LiteralPath $resolved -Algorithm SHA256).Hash
 Move-Item -LiteralPath $resolved -Destination $archived
 if((Test-Path -LiteralPath $resolved) -or (Get-FileHash -LiteralPath $archived -Algorithm SHA256).Hash -ne $oldHash){throw 'Archived APK verification failed.'}
 [pscustomobject]@{Source=$resolved;Destination=$archived;SHA256=$oldHash} | ConvertTo-Json -Compress | Add-Content -LiteralPath (Join-Path $archive 'RavenBackup-publish-journal.jsonl')
 if(Test-Path -LiteralPath ($resolved+'.sha256')){
  $sidecarHash=(Get-FileHash -LiteralPath ($resolved+'.sha256') -Algorithm SHA256).Hash
  Move-Item -LiteralPath ($resolved+'.sha256') -Destination ($archived+'.sha256')
  if((Get-FileHash -LiteralPath ($archived+'.sha256') -Algorithm SHA256).Hash -ne $sidecarHash){throw 'Archived checksum verification failed.'}
  [pscustomobject]@{Source=($resolved+'.sha256');Destination=($archived+'.sha256');SHA256=$sidecarHash} | ConvertTo-Json -Compress | Add-Content -LiteralPath (Join-Path $archive 'RavenBackup-publish-journal.jsonl')
 }
}
Move-Item -LiteralPath $stage -Destination $destination
if((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -ne $hash){throw 'Final APK verification failed.'}
$remaining=@(Get-ChildItem -LiteralPath $FinalDirectory -File -Filter '*.apk' | ForEach-Object {Read-Metadata $_.FullName} | Where-Object Package -eq $candidate.Package)
if($remaining.Count -ne 1){throw 'Expected exactly one current Raven APK.'}
[IO.File]::WriteAllText($destination+'.sha256',$hash.ToLowerInvariant()+'  '+[IO.Path]::GetFileName($destination)+"`n")
Write-Output "Published: $destination"
