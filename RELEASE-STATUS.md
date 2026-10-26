# Final build for the current milestone

Selected October 25, 2026: **Raven Backup 1.6.2**, version code **25**, package `dev.raven.backup`.

The app and GitHub icon now have a solid #1F2228 tile interior, with the original rounded outline, raven, box and green arrow retained. About no longer has Source & instructions; License is an underlined accent-colored text link. These changes do not alter the backup, restore or setup engines.

- [Final APK](releases/v1.6.2/RavenBackup-1.6.2.apk)
- [Checksum](releases/v1.6.2/SHA256SUMS.txt)
- [Installation](docs/INSTALL.md)
- [Factory-reset recovery](docs/FACTORY-RESET.md)
- [Migration](docs/MIGRATION.md)
- [Recovery backup management](docs/RECOVERY.md)
- [Removal](docs/UNINSTALL.md)

SHA-256: `0c42d4a0aefa6f4fc4ec77b8a916181f2ac6559e66f524a9955f3503c6b0ce08`

The signed update was installed on the Odin 2. Pixel comparison verified the solid icon background, original artwork RGB values and original exterior contour. The About source button is absent, and License is a clickable TextView that successfully opens the license text. All compiled classes outside MainActivity and the bundled setup helper match 1.6.1 byte for byte. Prior backup/restore and host-suite validation remains documented; no personal app-data operations were performed for this patch.

Actual Odin 3 transfer, Android 15 execution, Retroid hardware and Enforcing SELinux remain unverified. Compatible OEM PServerBinder access is required.
