# Final build for the current milestone

Selected October 25, 2026: **Raven Backup 1.6.0**, version code **23**, package `dev.raven.backup`.

This is the exact signed APK tested and installed on the connected Odin 2. It updates navigation, settings and backup selection, adds a selected-only view and RGB accent controls, uses one shared backup/restore folder, and places advanced tools and Purge in a collapsed Advanced card. The root, archive, migration and restore engines retain their existing behavior.

- [Final APK](releases/v1.6.0/RavenBackup-1.6.0.apk)
- [Checksum](releases/v1.6.0/SHA256SUMS.txt)
- [Installation](docs/INSTALL.md)
- [Factory-reset recovery](docs/FACTORY-RESET.md)
- [Migration](docs/MIGRATION.md)
- [Recovery backup management](docs/RECOVERY.md)
- [Removal](docs/UNINSTALL.md)

SHA-256: `8a3337c1be6d7a8a6e18147240a02944516c7b87f92343f4f37b76d101d10fe4`

All six host test groups and the FirmwareCheck integration harness passed. A clean source rebuild matches app and setup-helper payloads, and the approved APK matches the installed APK byte for byte. Odin 2 UI checks covered repeated search/select/clear cycles, 1/2/3-app counts, selected-only filtering, deselection, RGB/hex/preset synchronization and persistence, permission recovery, collapsed Advanced, and exact light/dark color matching. Earlier disposable-app restore and migration results remain documented as historical tests; full app-data restore was not repeated for this UI update.

Actual Odin 2-to-3 transfer, Android 15 execution, Retroid hardware and Enforcing SELinux remain unverified. Compatible OEM PServerBinder access is required. A regular release designation does not expand the documented compatibility.
