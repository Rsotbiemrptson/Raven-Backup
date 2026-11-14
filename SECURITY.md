# Security and sensitive data

Raven executes root scripts and handles private app data. Use only trusted builds and archives. Do not attach backup ZIPs, saved-game data, account files, device serials or unredacted root logs to public issues.

The current backend is an observed OEM Binder protocol, not a portable Android API. No generic root support or cross-firmware restore is claimed. The app does not change SELinux enforcement. The existing release signing key is private and not included.

Before reporting a problem, provide the app version, device model, Android version, relevant feature and a minimal synthetic reproduction. Use GitHub private vulnerability reporting if the repository maintainer has enabled it; otherwise ask the maintainer for a private reporting route before sharing sensitive details.

Version 1.6.9 permits ordinary app-data migration only after per-plan acknowledgment of device/firmware differences. This does not establish compatibility on new hardware. Destination setup, app identities, archive validation, supported label mapping and rollback remain required. See [Migration](docs/MIGRATION.md).


Private-data TARs can contain ordinary files/directories, metadata-only FIFOs and app-scoped symbolic links to `/storage/emulated/0/Android/data/<same-package>/files/`. Link targets are validated, links cannot be extraction parents, and an external restore checks target files in validated staging before private replacement. Hard links, cross-app/outside targets, device nodes and unsupported special entries remain rejected. Filename handling is NUL-delimited and content hashes do not interpret filenames. Metadata review is not a full payload-integrity check: each app’s private data/installers are validated at its turn, and external data while staging. ZIPs remain unauthenticated; only restore backups from a trusted source.
