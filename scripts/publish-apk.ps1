param(
    [Parameter(Mandatory)][string]$Apk,
    [string]$FinalDirectory = 'C:\APK-Workbench\Final apks',
    [string]$Evidence,
    [string]$OwnerOverrideSha256,
    [switch]$RecordOnly,
    [string]$BuildToolsDirectory,
    [string]$JavaHome
)
$ErrorActionPreference = 'Stop'
& 'C:\APK-Workbench\Toolchain\Publishing\Publish-Apk.ps1' -Apk $Apk -ExpectedPackage 'dev.raven.backup' -FinalDirectory $FinalDirectory `
    -Evidence $Evidence -OwnerOverrideSha256 $OwnerOverrideSha256 -RecordOnly:$RecordOnly -ReleaseNameTemplate 'RavenBackup-{version}.apk'
