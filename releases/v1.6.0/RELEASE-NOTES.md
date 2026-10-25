# Raven Backup 1.6.0

Final version for the current milestone, October 25, 2026. This is the same signed APK tested and installed on the Odin 2.

- Search for apps one at a time while retaining selections. Show selected / Show all reviews checked apps; the backup action shows correct app counts and a dimmed inactive state.
- Keep app rows visible above the landscape keyboard and ignore trailing search whitespace.
- Use one shared backup/restore folder. The folder button keeps half width, and Allow internal-storage appears only when permission is missing.
- Open Settings and About from a rounded three-dot menu. Advanced starts collapsed and contains Rerun Guided Setup, diagnostics, restore recovery/rehearsal and Purge all backups, with descriptions under each button.
- Put storage/schedule descriptions below their controls and place schedule/disable actions in one row.
- Customize light/dark/system themes with a wheel, synchronized RGB sliders, hex input and six color presets. List cards match the sidebar, and Details matches inactive tabs in both themes.

## Installation and updates

Install over the existing Raven Backup to keep settings and permissions. Fresh installs and reset devices still choose a folder, allow storage access, press Run checks, wait for Done / Checks passed, and press Continue. Settings > Advanced > Rerun Guided Setup opens the checks again. Use the owning Play Store account for apps requiring a license.

## Verification and scope

Six host test groups and the FirmwareCheck integration harness passed. A clean source rebuild matched the app and helper payloads; the approved APK matches the installed Odin 2 APK byte for byte. Device checks covered selection/search, counts, selected-only filtering, color controls/persistence, permission recovery, collapsed Advanced, and light/dark layout/color matching. This UI release does not introduce a new root or restore backend; historical restore/migration tests are recorded separately. No personal app was restored or personal backup deleted in this release’s UI testing.

Actual Odin 3 transfer, Android 15 execution, Retroid hardware and Enforcing SELinux remain unverified. Compatible OEM PServerBinder access is required. Android Keystore secrets, store entitlement and external game downloads are not reproduced by copying private files/APKs. Verify restored apps before removing recovery copies; private recovery copies are lost on uninstall, clear-data or factory reset.

GPL-3.0-only. Matching source is available in this release’s GitHub source archives. Private signing keys and personal backups are excluded.
