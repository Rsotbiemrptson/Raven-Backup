# Raven Backup 1.5.2

Final version for the current milestone, October 24, 2026. This is the same signed APK tested and installed on the Odin 2.

- Different model, Android version or firmware now prompts a concise migration warning with source device/version details and Cancel / Continue. Archive, app-version/signature and security-label checks remain required.
- Each user restore retains a separately named per-app recovery ZIP with a Recovery tag and Recovery restore action.
- Settings > Backup storage > Manage recovery backups lists apps, dates and sizes with individual selection, Select all and deletion confirmation. Pending-restore copies remain protected; normal backups are separate.
- Manage recovery backups now has a visible filled accent button matching the other storage actions.

## Setup and updates

Update an existing installation with this APK to retain settings. Fresh installs and factory-reset devices must choose a backup folder, allow storage access, press Run checks, wait for Done / Checks passed, then press Continue. Backups with installers can reinstall missing apps before data restore. Sign in to the owning Google Play account where licensing requires it.

Verify restored data inside each app before deleting recovery copies. Managed recovery copies are private app data and are lost on uninstall, clear-data or factory reset. Keep independent normal backups.

## Validation and limits

Six host check groups and the FirmwareCheck integration harness passed. Disposable-app tests on the Odin 2 covered synthetic model/Android differences, restore queues, APK reinstall, ownership/modes/SELinux labels, app reads, rollback, interruption recovery and protected selective recovery deletion. The warning, manager and Recovery tag/actions were checked on-device, including dark/light dialogs. No personal app data was used as a restore target.

Actual Odin 2-to-Odin 3 transfer and Android 15 behavior remain pending hardware. Compatible OEM PServerBinder access is required; generic su, other devices and Enforcing SELinux are not established as supported. Copying app files does not transfer Android Keystore secrets or store entitlement.

[Installation](../../docs/INSTALL.md) · [Factory-reset setup](../../docs/FACTORY-RESET.md) · [Migration](../../docs/MIGRATION.md) · [Recovery backups](../../docs/RECOVERY.md) · [Removal](../../docs/UNINSTALL.md)

GPL-3.0-only. Matching source is included; private keys and personal backups are excluded.
