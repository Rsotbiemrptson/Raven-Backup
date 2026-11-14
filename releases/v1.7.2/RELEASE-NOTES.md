Raven Backup 1.7.2 is the latest regular release for the tested Odin OEM root backend.

- Faster restore review: local/SD queues read checked metadata first. Private data/installers are validated at each app’s turn; large external files are validated and written into temporary staging in one pass before replacement. A 114-backup library reviewed in 2.7 seconds during device testing; actual restore time still depends on size and storage speed.
- Restore legitimate newline filenames, app-scoped external symbolic links and private named pipes. N.O.V.A. 3 and Blood Strike restored successfully and launched on Odin 3, with file contents, ownership, modes and SELinux labels verified. Other links and device nodes remain blocked.
- Export/import theme, accent, backup/restore preferences, backup order and schedule choices as a JSON file. Review before import. Imported schedules remain off until enabled; Android folder permissions and device approvals are not transferred.
- Fix clearing the recovery flag after safe cleanup of failed pre-replacement external staging. Corrupt payloads still receive a Check failed tag.
- Use a no-display setup helper, removing its test-window transitions. This does not claim to diagnose or repair unrelated moving display lines.

Install over the existing signed app to preserve settings and folder grants. Existing backups remain supported. Use 1.7.2 to restore backups with newline filenames, supported links or FIFOs. Backups already corrupted by the older 1.7.0 ZIP writer must be recreated from the original app/device.

After a factory reset: preserve backup ZIPs and any exported settings JSON on independent storage; install Raven; choose the backup folder and allow storage access; optionally allow App sizes access or skip it; press Run checks and wait for Checks passed; then Continue. Import settings afterward if wanted and review/enable schedules separately. The setup helper checks only disposable data and does not scan your whole library.

[Installation](https://github.com/Rsotbiemrptson/Raven-Backup/blob/v1.7.2/docs/INSTALL.md) · [Factory-reset recovery](https://github.com/Rsotbiemrptson/Raven-Backup/blob/v1.7.2/docs/FACTORY-RESET.md) · [Testing](https://github.com/Rsotbiemrptson/Raven-Backup/blob/v1.7.2/docs/TESTING.md)

Limits remain: tested OEM root service required; ZIPs are unencrypted/unauthenticated; primary external scope is Android/data/<package>/files, not arbitrary SD libraries or game folders. Keystore secrets and store entitlement are not transferred. Enforcing SELinux and other manufacturers remain unverified. The retained recovery ZIP covers private data only. A full 153 GB Eden restore has not been tested.
