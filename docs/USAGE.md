# Usage and limits

## Navigation and folders

The top-right three-dot menu contains **Settings** and **About**. About includes the version, build date, creator, license and source link.

In Settings > Backup storage, **Choose backup folder** sets the shared folder used for new backups and the Restore list. The selected folder is shown below the buttons and survives app restart. **Allow internal-storage** appears only when all-files access is missing or revoked. It opens Android’s permission page needed for root scripts, and hides again when permission is granted. Settings refreshes when you return to Raven.

The accent picker combines a hue/saturation wheel, hexadecimal input, and Red/Green/Blue sliders (0–255). All controls update the preview together. Presets are System Control orange (`#FF8000`), App Control red (`#FB0B24`), Gamepad blue (`#0070CC`), purple (`#8000FF`), yellow (`#FFFF00`), and white (`#FFFFFF`). **Default** restores Raven Backup green. **Cancel** discards picker edits; **Apply** saves them. There is no separate brightness slider.

## Backup

In **Backup**, search/filter installed user apps, select one or several, then choose **Back up 1 app** or **Back up [count] apps**. The confirmation offers APK/split inclusion. Apps are force-stopped while private credential/device-protected data is copied. Leave them stopped until completion. Each app gets a package-qualified subfolder and its own dated ZIP. New versions never intentionally replace older ones; same-day versions receive `_2`, `_3`, etc. Existing older filenames stay valid.

**Refresh** reloads installed apps and backup lists. Select an app in search results, clear the search, then search and select more apps; selections are retained as the displayed results change. **Back up apps** is inactive with no selection, then becomes **Back up 1 app**, **Back up 2 apps**, and so on. **Show selected** filters the list to checked apps and changes to **Show all** to return to the full list. Search and sort/filter still apply. Unchecking an app in this view removes it from the list. **Clear** clears all selected apps.

## Restore on an already verified device

After a fresh install or reset, complete the guided setup before entering the main app; see [Installation](INSTALL.md). Once verification passes, choose **Review & restore** for a single backup, or select multiple backups and **Restore selected**. Use only one version per app in a queue. Queue order is selection order; Select all follows displayed catalog order. A legacy multi-app ZIP restores every app listed in that queue's review, so check the names carefully.

**Import ZIP** opens one Raven ZIP from another location for review and restore. It does not copy it into the selected folder. The file picker alone does not start restoring; review and confirmation still apply.

A queue validates the selected archives before confirmation, then installs missing apps from full backups and restores archives one at a time. Installed app versions/signatures must match. An error stops the remaining queue; previously completed restores stay restored. Temporary recovery protects each individual data replacement; it is not an atomic rollback of the whole queue and does not undo APK installation. Open-after runs after the complete queue succeeds. Generic launch success does not prove every account, save or app feature works.

Raven keeps a separate data-only recovery ZIP for each app, capturing its state immediately before restore, which may be blank. These are marked Recovery in Restore and remain until removed in Settings > Backup storage > Manage recovery backups. The popup supports individual selection, Select all and confirmation. They are stored privately and are lost if Raven is uninstalled or its data is cleared. Never clear/uninstall Raven with pending recovery.

## Delete and Purge

**Delete selected** permanently deletes only reviewed ZIPs selected in Restore. **Purge all backups**, in Settings > Advanced, reviews all descendants of the selected folder and then asks to empty it. It includes ZIPs, non-ZIP files, hidden files, unfinished files and nested/empty subfolders. The selected folder itself is kept. Changed contents require review again; a storage failure after deletion begins may leave a partial purge. Purge targets the shared backup folder. Delete selected targets the reviewed ZIPs in that folder or managed recovery storage. Neither operation deletes installed app data. There is no recycle bin in this workflow.

## Store attribution

New full backups record the installing package and its signing-certificate hashes where available. Missing-app restore reuses that attribution only when the installed store matches those hashes. Old archives lack this information. **Repair Google Play link** reinstalls the exact currently installed signed APKs, retaining data; use it only for apps originally acquired through Google Play. It does not supply a license or subscription. The store still checks the real account's entitlement.

## Current limits

Device and firmware differences require migration confirmation. See [Migration](MIGRATION.md) and [Recovery backups](RECOVERY.md).

- Only the observed Odin OEM root protocol is implemented; no generic su/Magisk backend.
- User 0 and eligible non-system/non-shared-UID apps only; migration requires explicit confirmation.
- Live metadata checks were demonstrated with SELinux Permissive. Enforcing/cross-firmware restore is unverified; Raven does not change enforcement.
- No Android Keystore export, account recovery, runtime-permission restoration, store-entitlement transfer, external game-download backup, or general ACL/capability preservation.
- ZIPs are unencrypted and unauthenticated. Imported data and scripts require trust.
- Backups force-stop apps; schedules can interrupt an app in use unless charging/idle conditions are chosen. Android may delay jobs. First unlock after reboot is required.
- Local storage and a portable SD provider with read/write/rename/sync support were tested; cloud-document providers are not established as compatible.
- Clearing data/uninstalling/resetting loses approval. Complete the guided setup again. Keystore-bound data, accounts, runtime grants and store entitlement still require their own recovery.

## Permissions

The manifest requests installed-package visibility, all-files access, reboot reception and battery-optimization exemption. These support app selection, root-script launchers and scheduling. The manifest does not request Internet permission. Android system pickers/keyboards and other apps keep their own appearance.

## Fingerprint spoofing and migration

New backups record framework and vendor fingerprints separately from the advertised device fingerprint. Matching base identities, model and Android API recognize spoof-only identity changes. Older backups retain their model/API information; a recognized stock-318 spoof pair can match automatically. Other identity changes now trigger migration review. Malformed or incomplete metadata still fails validation. Confirmation does not bypass checksums, APK signatures, versions, ownership, modes or supported security-label rules.

## Advanced tools

The Advanced card in Settings starts collapsed. Tap its header or chevron to expand or hide the tools. Each filled accent button has a description directly below it.

- **Rerun Guided Setup:** opens setup again to check storage, root access and backup/restore with the disposable helper app. Checks start when you press Run checks.
- **Verify backup folder:** checks root access, validates archives, and checks read/write/rename/delete using its own temporary file. It does not restore app data.
- **Test root access:** checks Raven can run commands through the OEM privileged service; it does not replace guided setup approval.
- **Recover pending restore:** returns affected apps to their state before an interrupted restore, leaving them stopped. Use only when recovery is pending.
- **Purge all backups:** reviews all contents of the selected backup folder before asking to permanently empty it.
- **Rehearse a restore:** validates and extracts a chosen ZIP in a separate test area without replacing installed app data. It does not prove the app will work after a real restore.

**Disable schedule** stops future scheduled backups. **Allow background backups** requests exemption from Android battery optimization; it does not create a schedule or guarantee exact run times.
