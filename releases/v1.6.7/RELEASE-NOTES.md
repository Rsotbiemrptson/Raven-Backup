# Raven Backup 1.6.7

- Fix Odin 2-to-Odin 3 restores containing long filenames and supported ART profile labels, while retaining archive and security checks.
- Skip incompatible installers, missing installers and installed version/signature mismatches before replacing data. The final report lists restored, skipped, failed and not-run apps with reasons.
- Continue after safe compatibility skips; unknown errors, data failures and pending recovery still stop the queue.
- Leave **Open apps after restore** off by default. Explicitly saved choices remain respected.
- Keep legacy multi-app archives staged independently for sequential restore.

Tested on Odin 3 / Android 15 using Odin 2 / Android 13 backups: 30 compatible apps passed content, destination ownership, permission and SELinux-label verification. A mixed six-app queue restored aPS3e and skipped five incompatible 32-bit-only installers, with no failures. All eight host suites and FirmwareCheck integration passed. The final signed rebuild matches the device-tested APK byte for byte.

File verification does not establish game progress, account access or Android Keystore portability. Other untested devices/firmware, Retroid hardware and Enforcing SELinux remain unverified. Raven does not change SELinux enforcement or add generic su/Magisk support.

Install over the existing Raven Backup to retain settings and permissions. After a factory reset or fresh installation, select the backup folder, allow storage access if requested, press **Run checks**, wait for **Checks passed**, then **Continue**. Keep an independent backup before wiping a device. GPL-3.0-only.

[Installation](https://github.com/Rsotbiemrptson/Raven-Backup/blob/main/docs/INSTALL.md) · [Factory-reset recovery](https://github.com/Rsotbiemrptson/Raven-Backup/blob/main/docs/FACTORY-RESET.md) · [Removal](https://github.com/Rsotbiemrptson/Raven-Backup/blob/main/docs/UNINSTALL.md)
