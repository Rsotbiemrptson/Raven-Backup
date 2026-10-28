# Final build for the current milestone

Selected October 27, 2026: **Raven Backup 1.6.5**, version code **28**, package `dev.raven.backup`.

Advanced uses Gamepad-style surface-colored buttons in two columns with shorter descriptions. The overflow dots are centered within their button, and sidebar/card backgrounds retain the shared Raven palette. About now describes Backup only. About and License retain their shared 480dp width. These changes do not alter the backup, restore or setup engines.

- [Final APK](releases/v1.6.5/RavenBackup-1.6.5.apk)
- [Checksum](releases/v1.6.5/SHA256SUMS.txt)
- [Installation](docs/INSTALL.md)
- [Factory-reset recovery](docs/FACTORY-RESET.md)
- [Migration](docs/MIGRATION.md)
- [Recovery backup management](docs/RECOVERY.md)
- [Removal](docs/UNINSTALL.md)

SHA-256: `cc5d057247f0df3c288820741383884f17b43706bfb359e9cd2522ebe5d6df58`

The final APK matches the signed update tested on the Odin 2 byte for byte. Dark/light layouts, exact surface colors, centered dots and About content passed on-device checks. All six host suites and the FirmwareCheck integration harness passed. No personal app-data operations were performed for this patch. Earlier functional validation remains documented in [Testing](docs/TESTING.md).

Actual Odin 3 transfer, Android 15 execution, Retroid hardware and Enforcing SELinux remain unverified. Compatible OEM PServerBinder access is required.
