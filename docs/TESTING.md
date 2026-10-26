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

## Version 1.5.0 migration development — October 24, 2026

Tested on the original Odin 2, Android 13/API 33, using an isolated Raven test package and two disposable fixture apps. The normal Raven installation and personal apps were not used as restore targets. Synthetic backups declared a different model on Android 15/API 35 and the same model on Android 12/API 31 with different firmware. This exercises migration decisions on one OS, not real Android 12/15 behavior or physical device transfer.

Passed: expanded source metadata capture; source model/Android/firmware warnings; unconfirmed data restore and missing-APK installation blocked; cancellation leaves a missing app absent; confirmed data restore; injected failure with verified rollback and independent app reads; mixed-source sequential queue with explicit acknowledgment; full saved-APK reinstall/data restore to a fresh installation; destination UID/ownership, modes, SELinux labels, exact file paths and CE/DE data verification; independent app reads after restore; simulated interruption followed by recovery using a fresh restore engine; wrong app version and signing identity remain rejected despite migration consent. No pending recovery journal remained.

UI checks passed for the individual warning/cancel flow, warning before missing-app installation, separate install and data confirmations, the queue button disabled until its acknowledgment is checked, successful two-backup UI queue, and dark/light warning themes. Source details are shown in backup details and restore review; legacy metadata display is covered by host tests. The isolated test package and disposable apps were removed. The 1.5.0 signed testing APK was installed over Raven 1.4.1 with settings retained, and normal launch and restore approval were checked. The published 1.4.1 release remains unchanged.

All six host check groups and the production FirmwareCheck integration harness passed. The latter uses simulated Android/root identity adapters and tests same/legacy identities, upgrade/downgrade consent, changed source/destination invalidation, malformed metadata and source preservation. Test hooks and adapters are excluded from the production APK.

Pending: actual Odin 3 root-service access and setup, real Odin 2-to-3 transfer, Android 15 app/installer behavior, different SELinux policies, and game-level progress checks. An ordinary file/metadata pass does not establish application compatibility or recover Keystore-bound secrets. The root backend remains OEM PServerBinder; generic rooted-device support was not added.

## Version 1.5.1 recovery management — October 24, 2026

Tested in a separate installation on the connected Odin 2 with two disposable apps. Full guided setup passed. A two-app restore created two independently valid, specially named data-only recovery ZIPs. Restoring a recovery ZIP returned its app to the independently verified pre-restore state and created another recovery copy. Catalog entries remained visible with no chosen folder. Selective deletion removed only the reviewed file; normal backup and unrelated-file hashes/presence were retained. Changed metadata, out-of-folder entries and symlinks were rejected. Simulating interruption produced a durable journal, blocked recovery deletion and preserved every recovery ZIP; recovery completed and unlocked cleanup. Select-all deletion cleared managed recovery ZIPs while preserving normal/unrelated files.

UI verification covered empty selection disabling deletion, Select all/Clear selection, cancellation returning to the complete available list, deletion confirmation, successful one-file deletion with another retained, the Recovery tag/action and Recovery restore review title. Dark and light manager themes and the concise migration warning with Cancel/Continue were checked. Host archive/metadata/identity suites and the production FirmwareCheck integration harness passed. Test hooks are excluded from the signed production APK. No personal app was restored or personal backup deleted. Actual Odin 3 migration remains pending.


## Version 1.6.0 final UI release — October 25, 2026

All six host suites and the production FirmwareCheck integration harness passed. A clean rebuild matched the approved APK’s app and bundled helper payloads; the installed Odin 2 APK matched the approved artifact byte for byte. No test hooks are present in the distributed APK.

On the Odin 2, three repeated search/select/clear cycles retained selections, with correct singular/plural counts through 1/2/3 and 2/1/0. Show selected displayed exactly the checked apps; unchecking removed its row, Show all retained the other selections, and the empty selected view safely returned to the full list. Search ignores trailing whitespace and keeps results visible above the large landscape keyboard. The inactive backup action remained consistently dimmed.

All six color presets, RGB slider changes, hex input/readouts, color persistence after restart and Default reset passed. Screenshot pixels matched sidebar/list-card backgrounds and inactive-tab/Details backgrounds in light and dark themes. The folder button retained the same half-width bounds as the earlier visible-permission layout.

The storage-permission button was hidden with access granted. Revoking all-files access restarted Raven and its existing setup gate displayed Allow storage access; original permission was restored and normal launch/hidden-button state verified. The live missing-permission branch in an existing MainActivity was not directly observed because Android restarted the app. Advanced expansion/collapse, descriptions, Purge location, shared-folder catalog and revised scheduling/storage control placement were checked without starting Purge, scheduling jobs, restoring personal apps or deleting personal backups.

A short-lived independent restore-folder UI was tested during development and removed before this final build; the release uses one backup/restore folder. Prior app-data restore, migration, scheduled-job and reboot results above remain historical rather than newly repeated for 1.6.0. Odin 3, Retroid hardware and Enforcing SELinux remain pending.


## Version 1.6.1 icon correction — October 25, 2026

A direct pixel edit replaced the connected dark backdrop with exact #1F2228, filled the tile’s unintended interior alpha holes, and preserved the original rounded outer contour/exterior pixels. All 474,247 non-background artwork RGB pixels remained unchanged. The 1254×1254 app and GitHub PNGs are identical. The packaged PNG preserves visible pixels and alpha exactly; Android’s PNG optimizer normalized only two invisible RGB values under zero alpha.

The APK signature matches the retained release identity. Only AndroidManifest.xml and the colored icon PNG differ from 1.6.0 outside signing metadata. classes.dex and the bundled setup-helper APK are byte-identical. The update installed successfully on the Odin 2, and About displayed version 1.6.1 with the corrected icon. A white-background preview confirmed the original rounded shape and opaque tile interior. No app-data operations were performed; earlier backup/restore and host-suite results remain historical.


## Version 1.6.2 About cleanup — October 25, 2026

The signed update installed on the Odin 2 and displayed version 1.6.2 in About. Source & instructions is absent. License is a clickable TextView, rendered as underlined readable accent text without a background, and opens the GNU GPL text. The corrected icon is unchanged from 1.6.1. All compiled classes outside MainActivity and the bundled setup helper are byte-identical to 1.6.1. No backup/restore operations were run for this small About change.
