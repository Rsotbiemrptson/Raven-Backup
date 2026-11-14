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


## Version 1.6.3 About layout — October 25, 2026

Compared Raven Backup directly with the installed Raven System Control About popup on the Odin 2. Retained the stacked branding/text layout, unchanged icon and text sizes, and adjusted the popup width and bottom spacing. The entire description, metadata, full 44dp License touch target and Close button were visible simultaneously, with no scrollable content nodes. The License link still opened the GPL text. Only MainActivity.about changed in Java source; the icon and setup-helper APK are byte-identical to 1.6.2. No app-data operations were performed.


## Version 1.6.4 matching popup widths — October 25, 2026

Measured the paragraph on the Odin using the app’s actual theme and TextView at its current density/font scale: 469dp was the minimum outer popup width for three lines. Set About to 480dp for a small margin, 20% narrower than 1.6.3. The description still occupies three lines, the full License touch target and Close button are visible, and no content scrolling is needed. The License popup uses the same shared width constant; device title bounds confirmed identical left/right edges. The full GPL text remains scrollable and its link opens successfully. No app-data operations were performed.

## Version 1.6.5 Advanced layout and shared styling — October 27, 2026

Installed the signed update on the Odin 2 without clearing settings. Verified all six Advanced actions in two columns and three rows, with complete short descriptions in dark and light themes. Screenshot pixel checks confirmed Advanced buttons and sidebar use #1F2228 dark / #E9EAF0 light, and cards use #272B32 dark / #FFFFFF light, matching the other Raven apps. The three-dot glyph’s visual center matches the button center. About contains only the Backup description, with no Raven family text, and remains fully visible. Restored the original dark theme and left Advanced open. No backup, restore, purge or recovery actions were executed for this UI patch.

A fresh signed build is byte-for-byte identical to the device-tested APK. All six host suites and the production FirmwareCheck integration harness passed. These host checks use simulated platform identities and do not establish additional device compatibility.

## Version 1.6.6 Odin 3 development test — November 2, 2026

Tested the OEM root service and real Odin 2 Android 13 backups on an Odin 3 running Android 15/API 35. The 35-backup queue initially stopped during validation because three app archives used GNU long-filename headers. Added bounded GNU L parsing with strict path/termination/UTF-8 checks, effective-path verification, and owner remapping that preserves filename metadata headers. Links, PAX and other unsupported metadata remain rejected. Added the observed app_pccache/version/40-hex-id/oat/*.prof category-free ART profile path; unrelated paths/types/categories remain rejected.

Thirty compatible apps restored in sequential runs of 19, 1, 2 and 8. Every completed restore passed the engine’s content, destination ownership, mode and SELinux-label checks. Epic Games, X-plore and The Legend of Zelda 3D exercised the long-name fix. Daijishō included 13,265 regular private files and took longer to verify. Gravity Guy’s install failed with INSTALL_FAILED_NO_MATCHING_ABIS; it and the saved COD: Black Ops Zombies, AM2R, Modern Combat 3 and N.O.V.A. 3 installers contain only 32-bit native libraries, whereas this Odin 3 reports arm64-v8a only. The latter four incompatible installs were excluded after inspecting their APK contents. No firmware or SELinux enforcement settings were changed.

All six host suites and FirmwareCheck integration passed, including malformed/unsafe GNU header, metadata owner-remap and nested profile-label regression cases. Recovery copies were retained, the original sleep setting restored, and saved shared-storage archives preserved. Most apps were left stopped for manual confirmation of settings and game progress; installation/file verification alone does not prove gameplay, account access or save compatibility. AM2R and COD: Black Ops Zombies archives had no private data files. Gravity Guy had 314, MC3 one, and N.O.V.A. 3 had 36.

## Version 1.6.7 restore queue development test — November 2, 2026

All eight host suites and the production FirmwareCheck integration harness passed. New tests cover mixed/all-skipped queues, known installer incompatibilities, native-library ABI matching across APK splits, pending recovery, interruption, and stopping on unknown or data-replacement failures. A compatibility skip is allowed only before data replacement and with no pending recovery.

Installed the signed test update on the Odin 3 without clearing settings. The live review listed Gravity Guy, COD:BOZ, AM2R, MC3 and N.O.V.A. 3 as incompatible with this arm64-only device. A six-app queue skipped these five saved installers, continued to aPS3e, and displayed Restored 1 / Skipped 5 / Failed 0 / Not run 0 with each app and reason. aPS3e passed content, destination ownership, mode and SELinux-label verification. Its recovery backup was retained; the five skipped apps were not restored. Open apps after restore was visibly unchecked without toggling it, and aPS3e remained stopped after completion. The original device sleep setting was restored. The tested build was finalized as version 1.6.7. These checks do not establish additional game-progress verification.


## Version 1.6.8 faster setup development test — November 4, 2026

All eight host suites passed and the signed APK built successfully. Installed on the Odin 3 without clearing settings. With 41 personal backups and a deliberately invalid synthetic ZIP in the selected SD folder, Rerun Guided Setup passed its root/folder checks and disposable-helper backup, rollback, restore and fresh-UID reinstall checks. The existing archive scan was not invoked; the helper was removed. Testing and Done/Checks passed popups were observed. This rerun used the existing folder/storage grants, rather than clearing the user's settings to repeat Android's permission pickers.

Single-backup validation displayed the failed filename and exact reason before data replacement. The Check failed tag and reason survived force-stop/relaunch. A mixed queue containing that synthetic ZIP and an existing valid aPS3e backup fully checked the valid archive and continued to the restore review with the invalid item marked skipped. The queue was cancelled before installing/restoring aPS3e. Selecting the synthetic failed ZIP opened a separate deletion confirmation; confirming deleted only that ZIP. A complete path inventory matched all 41 remaining personal backups. The synthetic test folder was removed afterward. No personal app-data restore was performed. The optional all-library scan's new aggregation path was built but was not repeated across the full personal library for this test. The tested build was finalized as 1.6.8; a fresh signed rebuild matches the device-tested APK byte for byte. The production FirmwareCheck integration harness also passed.

## Version 1.6.9 signature override device test — November 6, 2026

On Odin 3, installed 1.6.9 and backed up a disposable app with its APK under the first signing key. Reinstalled the same package/version signed with a second key and seeded different data. Cancelling the signature warning preserved the destination data. Approving it restored the original data, with contents, ownership, permissions and SELinux labels verified. The destination APK matched its pre-restore SHA-256 and the second signed build. A manual launch independently verified credential-protected and device-protected files, preferences and removal of the post-backup extra file. Removed the fixture and its two test ZIPs and restored the original sleep setting. This test exercised the single-backup restore flow.

The same signed APK passed two queued restores on Odin 3. Without signature approval, the differently signed first fixture was skipped and retained its modified data. The matching second fixture restored successfully. With explicit approval, both fixtures restored and independently read the original files and preferences. The differently signed APK hash was identical before and after. The Report an issue menu opened the correct GitHub form after sign-in and prefilled version 1.6.9 and the test prompts. The form was cancelled without submission. Removed both fixtures and all five test ZIPs and restored the original sleep setting.


## Version 1.7.0 external storage development test — November 6, 2026

On Odin 3, backed up a disposable app with private data, APK and an external marker to SD. Restored the original marker and removed a file created after the backup. A separate ZIP64 fixture restored a 17 GiB external file into the same app. The app independently opened it and read the first and last bytes. That fixture used both a mismatched signature and version code. The combined warning appeared with the switches off, and enabling both switches led directly to normal restore review.

After adding the external swap marker and destination spool cap, rebuilt and repeated the small SD backup and external restore successfully. The app independently read the restored marker. All eight existing host suites and FirmwareCheck integration passed. An external parser check rejected traversal, links, checksum mismatch and truncation. No 153 GiB Eden backup or restore was performed. The final test APK is installed on both connected Odin devices.

## Version 1.7.0 streaming release checks — November 6, 2026

On Odin 3, the signed selection-toggle build streamed a 4.125 GiB disposable external file to an SD-card ZIP. After mutation, the restore queue ran only the selected ZIP. The restored file matched the original SHA-256, the external marker returned, and the added external file was removed. The Restore list showed the external-data label and the Recovery badge. The Backup selection survived clearing search.

Force-stopping Raven during a second write left an unfinished file and no new finished backup. The unfinished file was removed manually. The same build restored an earlier format 3 external fixture and verified its original marker. A format 2 fixture derived from that archive exercised APK installation and private data restore with the app absent. The app independently verified its restored data on launch. This did not use a ZIP generated by the published 1.6.9 build. Removed the fixture and its test archives and returned the external-data preference to off.

The published 1.6.9 APK then created a fresh app-and-data ZIP on Odin 2 (Android 13). On Odin 3 (Android 15), the 1.7.0 selection-toggle build showed the migration warning, installed the missing fixture from the ZIP, restored its private data, and verified contents, ownership, permissions, and SELinux labels. A manual app launch confirmed the original private files and preferences. Removed the disposable app and both test ZIPs from the Odin 3.


## Version 1.7.1 release acceptance — November 13, 2026

Final APK SHA-256: `2e0992e8e454835eced906e8cbc3c42f81d7be242c6f205611106e084edef9b0` (version code 34). All ten host suites passed. On Odin 3, Android 15/API 35, SELinux Permissive, the exact signed APK passed guided setup's disposable-app controlled failure/rollback, private content/ownership/mode/security-label/root-inode/path-set checks, APK reinstall with a fresh UID and independent CE/DE app reads. The helper was removed and no pending restore remained.

A fresh disposable backup used the production SAF writer on SD with a 4.125 GiB external file (8 MiB random data, sparse zeros and an end marker). The format-4 ZIP64 backup restored with identical SHA-256 `602d13a1a8e1caa210da125f2a9de94ff51e5c4246039b929ebd7ca3a04d4252`. A zero-byte file survived, a post-backup external file was removed, and the helper independently verified original private files/preferences. This compressible fixture verifies size/streaming correctness, not real-world large-library throughput. Confirmed absent/empty external sources were omitted; zero-byte content was included. A format-2 backup and the setup reinstall exercised the older private-data format. All uniquely created test ZIPs/folders and the helper were removed.

The data-sync foreground service and held partial wake lock were observed with the screen off during that successful backup. Backup/Restore switches, retaining a selection after clearing search, text-only progress and Settings also passed. Process-guard regressions rejected the actual running Game Assistant, a 20,000-row process list, a subprocess, and failed/empty enumeration, while accepting an unrelated prefix. The live Game Assistant PID and enabled state were unchanged. Its backup remains unsupported; the app offers only a warning and Close.

Earlier development builds of the same backend completed the remaining 28 apps of a 114-app personal backup batch without changing the first 86 ZIPs. Eden's finished ZIP was 163,267,666,064 bytes (152.1 GiB); the streamed TAR footer and ZIP64 size were checked. No full Eden restore at that size has been tested. Native Odin display offsets were observed with the text-only progress popup open. Optional size-access setup, saved ascending/descending ordering and UI layout were exercised during development. These results supplement the exact-APK acceptance above; they do not establish other manufacturers, Enforcing SELinux or arbitrary system-app compatibility.

The earlier published 1.7.0 writer could corrupt external entries by changing compression level mid-ZIP. Version 1.7.1 holds the level fixed. Affected archives must be recreated from the source app; upgrading does not repair them. The known-good local rollback remains 1.6.9 and cannot read new streamed external-data archives.


## Version 1.7.2 special-file device checks — November 14, 2026

Tested candidate SHA-256 `6231f399a6c3a7d1a70e682081cbb4bdcf80d27184b59aad400bad846d6e1377` (code 35), installed and hash-checked on Odin 3, Android 15/API 35, SELinux Permissive. Ten host suites passed, including GNU long link targets/names, app-scoped link UID remapping, real newline filenames, private FIFO metadata and rejection of cross-app/escaping targets, writes through link parents, hard links, device nodes and FIFO payloads.

The exact candidate passed full disposable setup/rollback/reinstall checks using a no-display helper. A fixture with a GNU-long-target symbolic link, a newline preferences filename and an audio FIFO restored with exact paths, metadata and app reads. A controlled failure preserved the pre-restore special files; a backup missing its linked external file was refused before private replacement. The helper independently followed the restored link, read the newline file and checked the FIFO type/mode. Disposable helpers and test ZIPs/folders were removed.

Blood Strike’s original backup was restored using the preceding newline/symlink candidate, then its special filename’s SHA, owner/mode and SELinux label were verified under this final candidate. N.O.V.A. 3’s original backup restored under the exact final candidate, including its GNU link targets, main/patch OBB references and private `audio.pcm` FIFO. The main OBB target was 1,980,258,754 bytes. Both successful restores passed contents, destination ownership, permission modes and security labels, and retained private recovery copies. Original ZIPs were opened read-only and kept. No restore remained pending.

N.O.V.A. launched through its restored settings screen to the game title/main menu. Blood Strike launched into the existing logged-in level-18 lobby. No match/campaign was started; gameplay completeness and every saved progression field were not independently established. This test does not establish general symlink/device support, Enforcing SELinux compatibility, or the cause of the separately reported moving display lines.
