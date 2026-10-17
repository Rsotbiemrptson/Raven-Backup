# Contributing

Contributions are provided under GPL-3.0-only, the project's license. Preserve copyright/license notices and document meaningful changes.

Run the host checks and build before submitting. Root backend and restore changes require independent, isolated-device testing. Do not infer compatibility from a device's Run as root label. Never use a personal app's data or personal backup library for destructive tests. Document firmware/SELinux conditions and limits without including device identifiers or account data.

Do not commit signing keys, passwords, app backups, private logs, credentials or generated fixture APKs. Keep the public APK accompanied by its corresponding source, build scripts, license and checksum. Test fresh installs separately from upgrades; the current restore-approval limitation is intentional and must remain visible until safely addressed.
