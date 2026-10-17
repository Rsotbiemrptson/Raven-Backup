# Validation and development testing

## What has been demonstrated

On the tested Odin 2 environment: root/library access, per-app version history, APK/split backup and reinstall, fresh-install UID adaptation, CE/DE data/preferences, file content/ownership/mode/SELinux verification, recovery after injected failure/interruption, selective legacy restore, sequential queues, duplicate-version rejection, deletion/purge cancellation and confirmation, changed-folder refusal, light/dark/system appearance and configuration-change dialog retention.

A naturally scheduled backup ran while the app stayed closed after a reboot and first unlock, using a temporary 15-minute developer interval. That does not establish exact timing for daily/weekly schedules. The short test interval is absent from the release UI.

Version 1.3.1's purge test used an isolated folder: Cancel preserved five files and three directories; changing contents after review refused deletion; a fresh confirmation removed six files and three directories, keeping the root and a sibling sentinel. Personal backups were not used as deletion targets. Their hashes remained unchanged. A separate, already-wiped stock-318 Odin 2 was tested on October 16: the built-in setup passed controlled rollback, full APK/data reinstall, changed UID adaptation, ownership/modes/labels and independent app reads; the helper was removed afterward. We did not perform the factory reset ourselves. Enforcing SELinux and unrelated firmware/devices remain unverified.

These are historical observations, not tests automatically reproduced by the host script. No personal archives, accounts, device serials, signing keys or raw device logs are included here.

## Disposable app testing

Use a dedicated test device or isolated apps/folders. Build fixture targets as described in BUILD.md, install only the intended target and explicitly seed synthetic data. For example:

```powershell
adb -s YOUR_SERIAL install .\dist\fixture-one.apk
adb -s YOUR_SERIAL install .\dist\fixture-two.apk
adb -s YOUR_SERIAL shell am start -n dev.raven.backupfixture/.Seed --es mode seed
adb -s YOUR_SERIAL shell am start -n dev.raven.backupfixture.two/dev.raven.backupfixture.Seed --es mode seed
```

A normal fixture launch reads restored values and writes `cache/launch-report.json`; check its `success` field using `adb shell run-as` for that fixture. Do not pass seed/mutate extras to a real app or clear real app data for a test.

Cleanup exact fixture packages, only if you installed them:

```powershell
adb -s YOUR_SERIAL uninstall dev.raven.backupfixture
adb -s YOUR_SERIAL uninstall dev.raven.backupfixture.two
```

Other possible fixture packages are `dev.raven.backupfixture.generic` and `dev.raven.backupfixture.reserve`. These are different from Raven Backup itself (`dev.raven.backup`). Delete only the synthetic backup folder you created; do not use a personal backup folder for purge tests.

## Publication-package checks

The prepared source was rebuilt using the documented flat-toolchain option and a separate local development key. Every non-signature ZIP entry in that rebuilt APK matched the supplied 1.3.1 release APK byte for byte. The supplied release signature and SHA-256 checksum were verified. All four host checks passed. No device installation or data operation was performed as part of this publication-package validation.


## Version 1.3.2

The fresh-install UI now explains the missing verification and offers a setup check. On stock 318, the non-debuggable bundled helper passed backup, injected-failure rollback, successful restore and APK reinstall with UID adaptation. Host firmware-policy tests accept exact legacy identities, the documented legacy spoof pair and matching new real-base metadata, and reject changed/partial base identities, unknown legacy fingerprints and Android/model mismatches.

On the already-wiped stock-318 Odin 2, the legacy spoofed Super Meat Boy Forever backup restored all five saved APKs (version 151) and private data successfully. Raven verified file content, owners, modes and SELinux labels; installer remained Google Play, and the source ZIP hash was unchanged. Launch redirected to Google Play sign-in; the owner subsequently confirmed the game worked after Play Store sign-in. In-game progress was not explicitly confirmed.

A separate read-only post-launch check confirmed all three game save files matched the backup byte for byte. Ownership/modes/labels and the 30-path set matched. One account/keychain preference file differed after launch; the entire live-data snapshot was therefore not claimed identical after opening the game. The temporary checker was removed. No Play sign-in was used for the read-only check. The owner subsequently confirmed the game worked after Play Store sign-in; in-game progress was not explicitly confirmed.

The 1.3.2 publication build was also rebuilt from the clean public source with a separate local signing key: all app payloads matched the distributed APK, and the rebuilt bundled helper matched apart from signing. All five host checks passed. The current package rechecks the official APK hash/signature, production source/resource equality, documentation links and public-file contents.

## Version 1.4.0 guided setup

Tested with an isolated fresh installation under a separate test package on the connected Odin 2; production Raven settings were not cleared. First launch routed to setup. Folder cancellation stayed in setup, storage denial showed a recovery action, and restarting the app retained the folder selection. Granting storage automatically began checks. A deliberately invalid disposable ZIP stopped setup before restore was enabled, named the ZIP and left it intact. After removing that synthetic file, Retry completed the full rollback/reinstall/data/metadata/app-read check, removed the helper and opened the main screen. A cold launch then skipped setup. The temporary installation and empty test folder were removed. No personal app restore or backup deletion was performed in this test.

All five host checks passed. The release's public source was rebuilt cleanly and compared with the signed APK (helper payload compared separately from its different local signing). The connected production installation was updated in place and checked for version and guided-setup availability.

## Version 1.4.1 setup popups

Updated the existing connected Odin installation without clearing its settings. Selecting the existing folder returned to Run checks without starting tests or installing the helper. Pressing Run checks showed a themed progress popup, completed backup/rollback/reinstall/changed-UID/data/mode/SELinux/app-read checks and removed the helper. Screens sampled through the dark-theme run showed no white diagnostic screens. Repeated the checks in light mode and visually verified both popups. Relaunching with Done pending displayed it again; Continue entered the main app. Restored the original dark theme. Five host checks passed. Fresh permission denial/cancellation behavior was not repeated in this version; earlier 1.4.0 evidence above remains historical. The other Odin remains untested for this flow.
