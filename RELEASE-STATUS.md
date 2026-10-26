# Final build for the current milestone

Selected October 25, 2026: **Raven Backup 1.6.1**, version code **24**, package `dev.raven.backup`.

This icon-only patch makes the tile background solid #1F2228 while preserving the original rounded outline, copper raven, off-white box and green arrow. The same corrected PNG is used in the app and on the GitHub page. Non-background artwork RGB values and the original exterior pixels are preserved; unintended interior transparency is repaired. The app classes and bundled setup helper are byte-identical to 1.6.0.

- [Final APK](releases/v1.6.1/RavenBackup-1.6.1.apk)
- [Checksum](releases/v1.6.1/SHA256SUMS.txt)
- [Installation](docs/INSTALL.md)
- [Factory-reset recovery](docs/FACTORY-RESET.md)
- [Migration](docs/MIGRATION.md)
- [Recovery backup management](docs/RECOVERY.md)
- [Removal](docs/UNINSTALL.md)

SHA-256: `f50838d5820e3fa74fc97b05c899ff640792a09d7218916917cb0957f803de20`

The signed update was installed on the Odin 2 and checked in About. Pixel comparison verified the background color, artwork colors, rounded outline and packaged image. The only non-signature APK payload changes are the icon PNG and Android manifest/version. Existing 1.6.0 backup/restore and UI validation remains applicable; no personal backup or restore was run for this icon correction.

Actual Odin 3 transfer, Android 15 execution, Retroid hardware and Enforcing SELinux remain unverified. Compatible OEM PServerBinder access is required. A regular release designation does not expand the documented compatibility.
