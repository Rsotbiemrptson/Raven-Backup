# Raven Backup 1.7.0

- Stream optional primary external app files directly into the backup ZIP. ZIP64 supports large archives, and Raven records a SHA-256 checksum as the stream finishes.
- Use accent-colored selection toggles in Backup and Restore. Configure signature and version overrides under Restore preferences.
- Read existing backup ZIPs, including older external-data backups. New streamed external-data ZIPs require Raven Backup 1.7.0 or newer.

The Odin 3 passed a streamed backup and restore with a 4.125 GiB disposable file, including a full checksum comparison after restore. An interrupted write did not appear as a finished backup. The Odin 3 also restored an older external-data backup and installed and restored a disposable app from a 1.6.9 ZIP made on the Odin 2.

Install the signed APK over Raven Backup to preserve settings and folder access. On a fresh install or after a factory reset, choose your backup folder and run guided setup. External restore stages a full copy on primary storage before replacing the app’s external files. Retained recovery ZIPs cover private app data only.

[Installation](https://github.com/Rsotbiemrptson/Raven-Backup/blob/v1.7.0/docs/INSTALL.md) · [Factory-reset recovery](https://github.com/Rsotbiemrptson/Raven-Backup/blob/v1.7.0/docs/FACTORY-RESET.md) · [Usage](https://github.com/Rsotbiemrptson/Raven-Backup/blob/v1.7.0/docs/USAGE.md)
