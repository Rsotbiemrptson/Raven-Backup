# Final build for the current milestone

Selected November 4, 2026: **Raven Backup 1.6.8**, version code **31**, package `dev.raven.backup`.

Guided setup checks root access, folder operations and disposable-app backup/restore without scanning existing archives. Selected archives are fully validated before restore. Failed backups are flagged with reasons and can be skipped or selected for a separate deletion confirmation. Advanced retains an optional full-library scan.

- [Final APK](releases/v1.6.8/RavenBackup-1.6.8.apk)
- [Checksum](releases/v1.6.8/SHA256SUMS.txt)
- [Installation](docs/INSTALL.md)
- [Factory-reset recovery](docs/FACTORY-RESET.md)
- [Migration](docs/MIGRATION.md)
- [Recovery backup management](docs/RECOVERY.md)
- [Removal](docs/UNINSTALL.md)

SHA-256: `365cc3496c9a16501175aee7ef856ba4fa3b41efc1dec106c146ce8d23ed5e7f`

The final signed rebuild matches the APK tested on the Odin 3 byte for byte. Guided setup passed with an invalid test ZIP present; single-backup failure reporting, persistent flags, mixed-queue continuation to review and confirmed deletion of only the synthetic ZIP passed. All 41 remaining personal backup paths were preserved. All eight host suites and FirmwareCheck integration passed. Detailed evidence and historical restore testing are in [Testing](docs/TESTING.md).

The complete personal library was not rescanned for this release, and no personal app-data restore was performed for this change. Game-level progress and account access need manual confirmation. Retroid hardware, other untested firmware and Enforcing SELinux remain unverified. Compatible OEM PServerBinder access is required.
