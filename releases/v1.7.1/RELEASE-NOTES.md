Raven Backup 1.7.1 is the latest regular release for the tested Odin 2/3 OEM root backend.

- Fix corrupt streamed ZIP entries caused by changing compression levels within an archive. Re-create affected older backups from the original device; installing this release cannot repair their missing/corrupt data.
- Include primary external app files by default for new settings, show their scope at confirmation, and safely omit confirmed absent/empty external directories without omitting private data or installers.
- Show app names and batch position during backup, with one completed app per line. Add Select all and compact landscape controls while retaining selections across searches.
- Add saved Package ID, App name and Size ordering, ascending/descending, with optional Android Usage access during setup or in settings.
- Protect manual operations with a foreground notification and CPU wake lock for screen-off work. Keep progress text-only and leave native AYN burn-in protection in charge.
- Allow eligible system-app data only with strict restore identity checks; refuse running persistent apps with a clear warning. Game Assistant remains unsupported on the tested Odin 3.

Update over the existing signed app; settings and backups are preserved. New streamed external backups require Raven 1.7.0 or newer. Prefer 1.7.1 for creating new backups. Backups already corrupted by an older writer must be recreated from the original app/device.

For a fresh install or factory reset: choose the backup folder, allow storage access, optionally allow App sizes access or skip it, then press Run checks and wait for Checks passed before continuing. Setup uses only a disposable helper and does not scan your backup library. Keep an independent backup copy before wiping. See [installation](https://github.com/Rsotbiemrptson/Raven-Backup/blob/v1.7.1/docs/INSTALL.md) and [factory-reset instructions](https://github.com/Rsotbiemrptson/Raven-Backup/blob/v1.7.1/docs/FACTORY-RESET.md).

External scope is primary internal shared storage Android/data/<package>/files, not arbitrary game libraries or physical SD-card app directories. ZIPs are unencrypted. Android Keystore, account entitlement and app-level cross-device compatibility remain limitations. Android 15 background time limits can interrupt exceptionally long operations. A 152.1 GiB Eden backup completed during development testing; a full Eden restore at that size has not been tested.
