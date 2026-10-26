# Device and Android migration — 1.6.3

Version 1.6.3 allows user-confirmed migration attempts. It has been tested on the Odin 2 with synthetic source-device/Android metadata, not on the Odin 3 or every Android version.

## Restore behavior

New backups record manufacturer, model, hardware device name, Android release/API, advertised fingerprint and separate framework/vendor fingerprints. Existing Raven ZIPs remain readable; their recorded model and API are displayed, with the Android release inferred from known API levels when absent. Device serial numbers and account identities are not collected.

When the model, Android API or firmware differs, restore review shows a warning, for example:

> This backup was made on a different device model and may not restore correctly.

The backup's source model, Android version and firmware appear underneath, followed by the current model and Android version. Migration uses a concise warning; downgrading Android versions can still fail.

For an individual backup, acknowledge the migration warning before APK installation or data restore. You still review APK installation and data replacement separately. Cancelling the warning leaves installed apps untouched. Queues show each backup's source and require an unchecked acknowledgment before Restore queue becomes available. Approval is held only for that restore plan and is checked again by the backend against source and destination identities. It is not stored as a permanent permission or read from a ZIP flag.

A firmware/model difference is a warning; malformed or incomplete identity metadata remains an error. Matching real framework/vendor identities still recognize fingerprint spoofing, including the narrowly documented legacy Odin 2 stock-318 pair. Legacy backups from other firmware can now be reviewed with migration confirmation.

## What remains checked

The destination installation must first pass guided setup on its own firmware. Only user-0 user apps are supported. APKs must pass Android installation, and installed app versions and signing identities must match the backup. Archives, paths, CRCs, hashes, regular-file types, ownership and modes are checked. Ordinary app-owned files are mapped to the destination app UID and ordinary app-data SELinux context. Unrecognized labels, shared UIDs, system apps and special ownership are rejected. Raven does not relax SELinux enforcement.

App root directories and their encryption policy are retained. Each restore still creates and verifies a temporary recovery snapshot before replacing data. Recovery restores a snapshot taken on the destination firmware; migration consent does not bypass that check. A separate recovery ZIP is retained for each app until deleted through Settings. See [Recovery backups](RECOVERY.md).

These checks verify copied files and metadata, not app-level compatibility. Android Keystore keys, device-bound encrypted content, licenses and accounts do not transfer. Apps can reject data, request sign-in or crash after an otherwise successful file restore. Confirm game progress inside each game and keep an independent source backup.

## Device support

The root backend remains the OEM PServerBinder protocol. Other devices must expose that compatible service and pass setup; this does not add generic su/Magisk support. Android 11/API 30 is still the minimum installation version. Passing tests on an Odin 2 does not establish Odin 3 firmware or real cross-version behavior.

## Testing

Host checks cover source display, older metadata, model/firmware differences, Android upgrade/downgrade warnings and consent invalidation. The FirmwareCheck integration harness uses simulated platform identities with the actual production identity/consent class. Live tests use only disposable apps and synthetic source-device metadata; results are documented in TESTING.md. Real Odin 2-to-3 migration remains pending hardware availability.

Neo Backup is a useful comparison, but no Neo Backup implementation was copied. Its [migration FAQ](https://github.com/NeoApplications/Neo-Backup/blob/main/FAQ.md#can-i-use-nb-to-switch-to-a-new-device--new-os--new-custom-rom--new-major-release-of-my-rom) describes device/ROM migration and its limitations, including Keystore-bound apps. Raven uses its own archive validation, ownership mapping and transactional recovery.
