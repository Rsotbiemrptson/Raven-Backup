# Final build for the current milestone

Selected October 24, 2026: **Raven Backup 1.5.2**, version code **22**, package `dev.raven.backup`.

This is the tested signed APK installed on the connected Odin 2, unchanged from its testing designation. It adds concise migration warnings and retained per-app recovery backups with tags, recovery restore and selective management in Settings. The Manage recovery backups button uses the filled accent style.

- [Final APK](releases/v1.5.2/RavenBackup-1.5.2.apk)
- [Checksum](releases/v1.5.2/SHA256SUMS.txt)
- [Installation](docs/INSTALL.md)
- [Factory-reset recovery](docs/FACTORY-RESET.md)
- [Migration](docs/MIGRATION.md)
- [Recovery backup management](docs/RECOVERY.md)
- [Removal](docs/UNINSTALL.md)

SHA-256: `ef3211fdb62f5b38e2c0d1bf3173343d519bdb40408138c4e4db6db9006f8032`

Six host test groups, the FirmwareCheck integration harness, disposable-app migration/recovery tests and device UI checks passed. Actual Odin 2-to-3 transfer, Android 15 execution and Enforcing SELinux remain unverified. Root access still requires a compatible OEM PServerBinder service; no generic su backend was added. The release is regular rather than a pre-release; that designation does not expand the documented test coverage.
