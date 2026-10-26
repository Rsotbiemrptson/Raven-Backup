# Installing Raven Backup 1.6.3

## Before installation

This is a device-specific experimental APK, not a Play Store release. It requires the tested OEM root service to back up private app data. Android's ordinary storage permission alone is insufficient. No generic root backend is included.

**Fresh-install restore setup is included.** Approval is private to this installation and firmware. After reset, clear-data or reinstall, complete the guided setup again; a plain root test or folder check is insufficient.

For recovery of existing backups on a wiped device, use [Restore after a factory reset](FACTORY-RESET.md), which covers preserving the backups, setup, app installation/data restore and store sign-in.

## Install on the device

1. Copy `releases/v1.6.3/RavenBackup-1.6.3.apk` to the device and open it in a file manager. Allow installation from that file manager if Android asks.
2. Open Raven Backup. On a fresh installation, the guided setup appears before the main app.
3. Choose a backup folder and approve Android's folder request. Use a dedicated folder; Purge in the main app empties that folder completely.
4. Tap **Allow storage access** if needed, enable access on Android's page, then return to Raven.
5. Press **Run checks**. A themed **Testing…** popup shows progress while Raven checks root, folder operations, existing archives and full backup/restore using its bundled disposable helper. The helper diagnostic screen stays hidden and it is removed afterward. The themed **Done** popup confirms **Checks passed**; tap **Continue** to enter the app with restore enabled. Failures show an explanation and a retry action. These checks do not change your other apps or original ZIPs.
6. Make a small backup and keep an independent copy before relying on the app.

Raven does not root a device or install a root manager. A working supported OEM root service is required. Android's own folder and storage approvals must still be confirmed by the user. Setup starts on first **open**, not silently in the background at APK installation.

Existing verified installations keep their settings and open normally after updating. Use **Settings > Advanced > Rerun Guided Setup** to review or repeat setup. After factory reset, clear-data, lost folder access or loss of firmware approval, the guided flow is required again. Follow [Factory-reset recovery](FACTORY-RESET.md) for restoring existing APK/data backups and recovering interrupted setup.

For scheduled backups, use **Choose apps & schedule**, select daily/weekly and any charging/idle conditions, then **Allow background backups**. Android decides the actual run time. After reboot, unlock the device once before private-data backups can run.

## Updates

Install the new signed APK over the existing package. Normal updates with the same signing certificate preserve Raven's settings and folder grants. There is no need to uninstall first. A differently signed local build cannot replace the distributed APK; see [Building](BUILD.md). Resolve any pending restore before changing builds or clearing data.

## Optional ADB installation

With Android platform-tools installed and USB debugging authorized:

```powershell
adb devices
adb -s YOUR_SERIAL install -r .\releases\v1.6.3\RavenBackup-1.6.3.apk
```

Choose the intended device serial explicitly, especially when two devices are attached. Open Raven afterward and follow the same setup steps.

## Verify the download

From the repository root:

```powershell
Get-FileHash .\releases\v1.6.3\RavenBackup-1.6.3.apk -Algorithm SHA256
```

Compare the result with `releases/v1.6.3/SHA256SUMS.txt`. A matching checksum detects file changes; it is not a substitute for trusting the source of the download.

## Migration and recovery backups

Different device model, Android version or firmware shows the backup source details and asks **Cancel / Continue** before proceeding. App version/signatures and file-security checks still apply. After restore, verify the data inside each app. Recovery copies appear in Restore with a **Recovery** tag; manage them through **Settings > Backup storage > Manage recovery backups**. The popup supports individual selection, Select all and deletion confirmation. Recovery copies are in Raven's private app storage and are lost on uninstall, clear-data or factory reset; normal backups remain separate. See [Migration](MIGRATION.md) and [Recovery backups](RECOVERY.md).
