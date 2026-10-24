# Removing Raven Backup

## Before uninstalling

1. Let any manual backup or restore finish. Do not clear Raven's app data, force-stop it or uninstall it during a restore.
2. In **Settings > Automatic backups**, choose **Disable schedule** and let any active operation finish.
3. If a restore was interrupted or Raven reports pending recovery, use **Settings > Device & recovery > Recover pending restore** and wait for verified completion. Raven's private files contain the recovery journal and temporary recovery data; uninstalling or clearing its storage removes those files.
4. Managed recovery ZIPs in Raven’s private storage will be lost on uninstall. Confirm restored app data before removing Raven; normal shared backups remain separate. See [Recovery backups](RECOVERY.md).
5. Copy backups you want to keep to a computer or another independent location. Check those copies before deleting originals.

## Uninstall the app

Open Android **Settings > Apps > Raven Backup > Uninstall**, or long-press its launcher icon and choose **App info > Uninstall**.

Optional ADB command, after the steps above:

```powershell
adb -s YOUR_SERIAL uninstall dev.raven.backup
```

Uninstalling removes Raven's private settings, live-restore approval, recovery files, scheduled jobs and its package permissions. Shared backup ZIPs and exported files are separate from Raven's private app storage and normally remain. Restored target-app data is not rolled back by uninstalling Raven. Reinstalling Raven after uninstall/clear-data requires permissions and device verification again.

## Optional shared-file cleanup

Using a file manager, review and remove only Raven files you no longer need:

- Your chosen backup folder and any computer/SD copies you deliberately want to discard.
- `RavenBackupRollbacks` in shared internal storage, if exported rollback ZIPs were retained.
- `raven-backup-job-*.sh` launcher scripts in the root of shared internal storage. They refer to Raven's private jobs and are not useful after the app is removed.

**Purge is not an uninstall step.** It permanently removes every file and subfolder inside the currently selected backup folder, including non-ZIP, hidden and unfinished files. It keeps that selected folder itself. Use it only when you intend to discard all of that folder's contents. A cancelled purge changes nothing.

Do not delete target apps' private data as part of removing Raven. If disposable fixtures were installed for development, remove only their exact packages using the commands in [Testing](TESTING.md).
