# Security and sensitive data

Raven executes root scripts and handles private app data. Use only trusted builds and archives. Do not attach backup ZIPs, saved-game data, account files, device serials or unredacted root logs to public issues.

The current backend is an observed OEM Binder protocol, not a portable Android API. No generic root support or cross-firmware restore is claimed. The app does not change SELinux enforcement. The existing release signing key is private and not included.

Before reporting a problem, provide the app version, device model, Android version, relevant feature and a minimal synthetic reproduction. Use GitHub private vulnerability reporting if the repository maintainer has enabled it; otherwise ask the maintainer for a private reporting route before sharing sensitive details.

Version 1.6.4 permits ordinary app-data migration only after per-plan acknowledgment of device/firmware differences. This does not establish compatibility on new hardware. Destination setup, app identities, archive validation, supported label mapping and rollback remain required. See [Migration](docs/MIGRATION.md).
