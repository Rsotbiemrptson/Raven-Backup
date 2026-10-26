# Raven Backup

<img src="docs/images/icon.png" alt="Raven Backup icon" width="96" />

Raven Backup is an experimental Android app for backing up user-app private data and, optionally, the original APKs and installed splits. Its root backend was tested on an **AYN Odin 2** using the OEM `PServerBinder` service.

**Final build for the current milestone.** Adds [device and Android migration warnings](docs/MIGRATION.md), plus [tagged recovery backups and selective cleanup](docs/RECOVERY.md). Simulated migration and recovery tests passed on the Odin 2. Actual Odin 2-to-Odin 3 transfer remains unverified until that hardware is available.

## What it does

- Select multiple installed user apps for backup, with a ZIP per app and a history of versions. Search for apps one at a time without losing checked selections, then use **Show selected** to review them together.
- Include the original signed base APK and installed splits, or back up private data only.
- Choose one local/internal-storage or portable SD folder for both backups and the Restore list. Storage permission is requested only when missing.
- Name new archives `AppName_MM-DD-YYYY.zip`, with `_2`, `_3`, etc. for additional same-day versions.
- On verified devices, review and restore a backup or queue different apps in selection order. Each restore checks content, ownership, permissions and SELinux labels. Queues stop on error; earlier successful restores stay restored.
- Create temporary transaction recovery protection and retain separately tagged per-app recovery ZIPs until deleted through **Settings > Backup storage > Manage recovery backups**.
- Delete selected ZIPs after confirmation. **Settings > Advanced > Purge all backups** can empty the chosen backup folder, including non-ZIP files and all subfolders, after confirmation; the selected root folder is kept.
- Schedule approximate daily/weekly backups with charging/idle options and optional Android battery-optimization exemption.
- Choose light, dark or system appearance and customize the accent using a color wheel, RGB sliders, hex input or Raven-family presets.
- Open **Settings** and **About** from the three-dot menu. The collapsed **Advanced** card contains **Rerun Guided Setup**, backup verification, root-access testing, interrupted-restore recovery and restore rehearsal. First-launch checks start only when **Run checks** is pressed.

## Supported scope

Android 11/API 30 is the minimum declared version. The demonstrated root backend and live restore were tested on two Odin 2 devices (stock 318 and a stock-318-based fingerprint-spoofed build) with **SELinux Permissive**. Enforcing SELinux, other devices/firmware, secondary users, system apps and shared-UID apps are not established as supported. A similarly named Run as root option on another device is not evidence of compatibility.

Only use backups you trust: ZIPs are unencrypted and not cryptographically authenticated. Android Keystore keys, store entitlement, accounts, runtime grants, external game downloads, ACLs and arbitrary filesystem capabilities are not reproduced by copying private files/APKs. Device, Android-version or firmware differences require a migration warning and explicit confirmation. Archive integrity, app version/signatures, destination ownership and supported security-label checks remain required. Real cross-device/OS behavior is app-dependent and not yet verified on the Odin 3. Google Play still checks real account entitlement. See [Usage and limits](docs/USAGE.md).

## Source and building

The app uses Java and Android framework APIs without a Gradle dependency graph. [Build instructions](docs/BUILD.md) describe the Windows PowerShell/JDK/Android SDK build, local signing and host tests. The release signing key and SDK tools are deliberately not included.

- `src/`, `res/`, `AndroidManifest.xml`: app source and resources.
- `build.ps1`: portable app/fixture build script.
- `tests/`: host checks, Android test helper and disposable fixture source.
- `releases/v1.6.4/`: the current signed APK and checksum.
- `docs/`: installation, removal, usage, build and validation notes.

## License and contributions

Copyright (C) 2026 Raven Backup contributors. This project, including its app source and project artwork, is licensed under **GNU GPL version 3 only**; see [LICENSE](LICENSE). Distributed modified versions must comply with that license and provide corresponding source. Private modifications do not have to be published. This does not impose a license on the user's backed-up apps or data. See the [GNU licensing FAQ](https://www.gnu.org/licenses/gpl-faq.en.html#GPLRequireSourcePostedPublic).

See [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md), [CHANGELOG.md](CHANGELOG.md), and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
