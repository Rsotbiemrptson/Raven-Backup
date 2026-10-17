# Usage and limits

## Backup

In **Backup**, search/filter installed user apps, select one or several, then choose **Back up selected**. The confirmation offers APK/split inclusion. Apps are force-stopped while private credential/device-protected data is copied. Leave them stopped until completion. Each app gets a package-qualified subfolder and its own dated ZIP. New versions never intentionally replace older ones; same-day versions receive `_2`, `_3`, etc. Existing older filenames stay valid.

## Restore on an already verified device

After a fresh install or reset, complete the guided setup before entering the main app; see [Installation](INSTALL.md). Once verification passes, choose **Review & restore** for a single backup, or select multiple backups and **Restore selected**. Use only one version per app in a queue. Queue order is selection order; Select all follows displayed catalog order. A legacy multi-app ZIP restores every app listed in that queue's review, so check the names carefully.

A queue validates the selected archives before confirmation, then installs missing apps from full backups and restores archives one at a time. Installed app versions/signatures must match. An error stops the remaining queue; previously completed restores stay restored. Temporary recovery protects each individual data replacement; it is not an atomic rollback of the whole queue and does not undo APK installation. Open-after runs after the complete queue succeeds. Generic launch success does not prove every account, save or app feature works.

Retaining rollback ZIPs is optional; temporary recovery copies are removed after verified success when retention is off. A rollback captures the state immediately before restore, which may be blank. Never clear/uninstall Raven with pending recovery.

## Delete and Purge

**Delete selected** permanently deletes only reviewed ZIPs selected in Restore. **Purge**, beside Choose backup folder, reviews all descendants of the selected folder and then asks to empty it. It includes ZIPs, non-ZIP files, hidden files, unfinished files and nested/empty subfolders. The selected folder itself is kept. Changed contents require review again; a storage failure after deletion begins may leave a partial purge. Neither operation deletes installed app data. There is no recycle bin in this workflow.

## Store attribution

New full backups record the installing package and its signing-certificate hashes where available. Missing-app restore reuses that attribution only when the installed store matches those hashes. Old archives lack this information. **Repair Google Play link** reinstalls the exact currently installed signed APKs, retaining data; use it only for apps originally acquired through Google Play. It does not supply a license or subscription. The store still checks the real account's entitlement.

## Current limits

- Only the observed Odin OEM root protocol is implemented; no generic su/Magisk backend.
- User 0 and eligible non-system/non-shared-UID apps only; same-firmware restore.
- Live metadata checks were demonstrated with SELinux Permissive. Enforcing/cross-firmware restore is unverified; Raven does not change enforcement.
- No Android Keystore export, account recovery, runtime-permission restoration, store-entitlement transfer, external game-download backup, or general ACL/capability preservation.
- ZIPs are unencrypted and unauthenticated. Imported data and scripts require trust.
- Backups force-stop apps; schedules can interrupt an app in use unless charging/idle conditions are chosen. Android may delay jobs. First unlock after reboot is required.
- Local storage and a portable SD provider with read/write/rename/sync support were tested; cloud-document providers are not established as compatible.
- Clearing data/uninstalling/resetting loses approval. Complete the guided setup again. Keystore-bound data, accounts, runtime grants and store entitlement still require their own recovery.

## Permissions

The manifest requests installed-package visibility, all-files access, reboot reception and battery-optimization exemption. These support app selection, root-script launchers and scheduling. The manifest does not request Internet permission. Android system pickers/keyboards and other apps keep their own appearance.

## Fingerprint spoofing

New backups record framework and vendor fingerprints separately from the advertised device fingerprint. Matching real base identities, model and Android API allow a spoof-only identity change. Older archives lack that metadata: they require an exact fingerprint, except for the documented Odin 2 stock-318 / Pixel 10 Pro spoof pair, checked against the target stock-318 system/vendor identities and Android 13/API 33. This exception does not allow arbitrary cross-firmware restores. Checksums, APK signatures, versions, ownership, modes and security-label validation still apply.
