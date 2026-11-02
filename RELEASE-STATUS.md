# Final build for the current milestone

Selected November 2, 2026: **Raven Backup 1.6.7**, version code **30**, package `dev.raven.backup`.

Includes Odin 3 long-filename and supported ART profile-label restore fixes, compatibility skips with a per-app completion report, and opening apps after restore off by default. Unknown errors, data-replacement failures and pending recovery still stop the queue.

- [Final APK](releases/v1.6.7/RavenBackup-1.6.7.apk)
- [Checksum](releases/v1.6.7/SHA256SUMS.txt)
- [Installation](docs/INSTALL.md)
- [Factory-reset recovery](docs/FACTORY-RESET.md)
- [Migration](docs/MIGRATION.md)
- [Recovery backup management](docs/RECOVERY.md)
- [Removal](docs/UNINSTALL.md)

SHA-256: `98115556e20e8e9d126a564eca994ac0fd7320fe23691b229ac8d11efff7eeae`

The final signed rebuild matches the APK tested on the Odin 3 byte for byte. Thirty compatible Odin 2 app backups passed file and metadata verification on Android 15. The mixed-queue test restored one app and skipped five incompatible installers with reasons; the restored app remained stopped. All eight host suites and the FirmwareCheck integration harness passed. Full evidence and historical UI/setup checks are in [Testing](docs/TESTING.md).

Game-level progress and account access need manual confirmation. Retroid hardware, other untested firmware and Enforcing SELinux remain unverified. Compatible OEM PServerBinder access is required.
