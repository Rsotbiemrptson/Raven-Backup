# Final build for the current milestone

Selected October 25, 2026: **Raven Backup 1.6.4**, version code **27**, package `dev.raven.backup`.

The app and GitHub icon now have a solid #1F2228 tile interior, with the original rounded outline, raven, box and green arrow retained. About no longer has Source & instructions; License is an underlined accent-colored text link. About and License share a 480dp width. About retains its stacked layout, three description lines and full visibility without scrolling on the Odin; License keeps its full text scrollable. These changes do not alter the backup, restore or setup engines.

- [Final APK](releases/v1.6.4/RavenBackup-1.6.4.apk)
- [Checksum](releases/v1.6.4/SHA256SUMS.txt)
- [Installation](docs/INSTALL.md)
- [Factory-reset recovery](docs/FACTORY-RESET.md)
- [Migration](docs/MIGRATION.md)
- [Recovery backup management](docs/RECOVERY.md)
- [Removal](docs/UNINSTALL.md)

SHA-256: `feecf0d5287c321a37ff84cb63c6894e8039f98ee8bab2cdfd8b1fec36f34f2a`

The signed update was installed on the Odin 2. Pixel comparison verified the solid icon background, original artwork RGB values and original exterior contour. The About source button is absent, and License is a clickable TextView that successfully opens the license text. All compiled classes outside MainActivity and the bundled setup helper match 1.6.1 byte for byte. Prior backup/restore and host-suite validation remains documented; no personal app-data operations were performed for this patch.

Actual Odin 3 transfer, Android 15 execution, Retroid hardware and Enforcing SELinux remain unverified. Compatible OEM PServerBinder access is required.
