Raven Backup 1.7.3 is the latest regular release for the tested Odin OEM root backend.

- Automatically remove Raven's temporary `raven-backup-job-<UUID>.sh` launchers from the root of shared internal storage after their root job finishes, including jobs that report an error.
- Check for older completed-job launchers when Raven opens or starts another root job. Only unchanged Raven launchers with a matching private job and a valid completion marker are removed. Unfinished, modified, unrecognized or orphaned scripts are retained.
- Backup ZIPs, recovery copies and private job/recovery records are unaffected. Existing backup formats remain supported.

Tested on Odin 3 with the exact release APK: seven old launchers removed on opening Raven; a new disposable-app backup created a launcher and removed it after completion; its ZIP reopened and passed validation. An injected root-job failure also removed its launcher. Unfinished/modified-script and symlink guards passed on device. Eleven host suites passed. The disposable app, test backup and test driver were removed afterward.

Install over the existing signed app to preserve settings and folder grants. Wait for any active backup or restore to finish before updating. Open Raven once after updating to clean up verifiably completed old launchers. Internal-storage access must still be allowed for cleanup.

After a factory reset: keep backup ZIPs and exported settings on independent storage; install Raven, choose the backup folder, allow internal-storage access, optionally allow App sizes access, then press Run checks and wait for Checks passed before Continue. Imported schedules remain off until reviewed and enabled.

[Installation](https://github.com/Rsotbiemrptson/Raven-Backup/blob/v1.7.3/docs/INSTALL.md) · [Factory-reset recovery](https://github.com/Rsotbiemrptson/Raven-Backup/blob/v1.7.3/docs/FACTORY-RESET.md) · [Testing](https://github.com/Rsotbiemrptson/Raven-Backup/blob/v1.7.3/docs/TESTING.md)

Limits remain: a compatible tested OEM root service is required; ZIPs are unencrypted/unauthenticated. External scope is the app's primary Android/data/<package>/files, not arbitrary SD libraries. Android Keystore secrets and store entitlement are not transferred. Other manufacturers and enforcing SELinux remain unverified. Retained recovery ZIPs cover private data only. A full 153 GB Eden restore has not been tested. Backups corrupted by the older 1.7.0 ZIP writer must be recreated from the original data.
