# Raven Backup 1.6.8

- Make guided setup faster: check root, folder operations and disposable-app backup/restore without scanning every existing backup.
- Fully validate selected archives before restore. Failed archives no longer block reviewing other selected backups.
- Flag failed validations with the filename and reason. Flags survive app restart and are cleared when the file changes or full validation passes.
- Offer selection and a separate confirmation before deleting failed backups; keep file identity and folder-location checks.
- Keep **Advanced > Verify backup folder** as an optional full scan, reporting failed archives instead of stopping at the first.

Tested on Odin 3: setup passed with an invalid synthetic ZIP present, the failure flag survived restart, a mixed selection continued to restore review with the invalid archive marked skipped, and confirmed deletion removed only the disposable test ZIP. All 41 remaining personal backup paths were preserved. No personal app-data restore was performed for this change. All eight host suites and the FirmwareCheck integration harness passed. The final signed rebuild matches the device-tested APK byte for byte.

Install over an existing Raven Backup to keep settings and permissions. After a factory reset or fresh install, choose the backup folder, allow storage access if requested, press **Run checks**, wait for **Checks passed**, then **Continue**. Existing archives are checked when selected for restore, or through the optional Advanced scan.

Includes the previous Odin 3 restore fixes and compatibility reports. File verification does not prove game progress or account access. Retroid hardware, other untested firmware and Enforcing SELinux remain unverified. GPL-3.0-only.

[Installation](https://github.com/Rsotbiemrptson/Raven-Backup/blob/main/docs/INSTALL.md) · [Factory-reset recovery](https://github.com/Rsotbiemrptson/Raven-Backup/blob/main/docs/FACTORY-RESET.md) · [Removal](https://github.com/Rsotbiemrptson/Raven-Backup/blob/main/docs/UNINSTALL.md)
