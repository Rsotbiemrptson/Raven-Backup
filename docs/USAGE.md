# Usage and limits

## Navigation and folders

The top-right three-dot menu contains **Settings** and **About**. About includes the version, build date, creator and an accent-colored License text link. Its stacked layout fits on the Odin without scrolling.

In Settings > Backup preferences, **Choose backup folder** sets the shared folder used for new backups and the Restore list. The selected folder is shown below Choose backup folder and survives app restart. **Allow internal-storage** appears only when all-files access is missing or revoked. It opens Android’s permission page needed for root scripts, and hides again when permission is granted. Settings refreshes when you return to Raven.

The accent picker combines a hue/saturation wheel, hexadecimal input, and Red/Green/Blue sliders (0–255). All controls update the preview together. Presets are System Control orange (`#FF8000`), App Control red (`#FB0B24`), Gamepad blue (`#0070CC`), purple (`#8000FF`), yellow (`#FFFF00`), and white (`#FFFFFF`). **Default** restores Raven Backup green. **Cancel** discards picker edits; **Apply** saves them. There is no separate brightness slider.

## Backup

In **Backup**, search/filter installed user apps, select one or several, then choose **Back up 1 app** or **Back up [count] apps**. The confirmation offers APK/split inclusion. Apps are force-stopped while private credential/device-protected data is copied. Leave them stopped until completion. Each app gets a package-qualified subfolder and its own dated ZIP. New versions never intentionally replace older ones; same-day versions receive `_2`, `_3`, etc. Existing older filenames stay valid.

**Refresh** reloads installed apps and backup lists. Select an app in search results, clear the search, then search and select more apps; selections are retained as the displayed results change. **Back up apps** is inactive with no selection, then becomes **Back up 1 app**, **Back up 2 apps**, and so on. **Show selected** filters the list to checked apps and changes to **Show all** to return to the full list. Search and sort/filter still apply. Unchecking an app in this view removes it from the list. **Clear** clears all selected apps.

## Restore on an already verified device

After a fresh install or reset, complete the guided setup before entering the main app; see [Installation](INSTALL.md). Once verification passes, choose **Review & restore** for a single backup, or select multiple backups and **Restore selected**. Use only one version per app in a queue. Queue order is selection order; Select all follows displayed catalog order. A legacy multi-app ZIP restores every app listed in that queue's review, so check the names carefully.

**Import ZIP** opens one Raven ZIP from another location for review and restore. It does not copy it into the selected folder. The file picker alone does not start restoring; review and confirmation still apply.

For seekable local/SD archives, a queue checks metadata before confirmation, then fully validates private data/installers at each app’s turn. External files are checked while writing temporary staging, before replacement. Non-seekable inputs and older multi-app ZIPs keep full up-front validation. Metadata failures appear in a review popup; private payload/installer failures found later are skipped with a reason. External staging/data failures stop the queue. Failed archives are flagged and require a separate confirmation for deletion. Failed checks remain visible until the file changes or a new full validation passes. The queue installs missing apps from full backups and restores archives one at a time. Installed package IDs must match. Restore preferences can allow different signatures, different versions, or both. These switches default to off. Raven restores data into the installed copy and keeps its APK when the required settings are enabled. Known installer incompatibilities, missing installers and blocked identity mismatches are skipped before data replacement; the final report names each restored, skipped, failed or not-run app and gives reasons. Unknown errors, data-replacement failures and pending recovery stop the remaining queue; previously completed restores stay restored. Temporary recovery protects each individual data replacement; it is not an atomic rollback of the whole queue and does not undo APK installation. Open-after defaults to off. If explicitly enabled, it opens only successfully restored apps after a queue finishes without a safety stop. Generic launch success does not prove every account, save or app feature works.

Raven keeps a separate data-only recovery ZIP for each app, capturing its state immediately before restore, which may be blank. These are marked Recovery in Restore and remain until removed in Settings > Backup preferences > Manage recovery backups. The popup supports individual selection, Select all and confirmation. They are stored privately and are lost if Raven is uninstalled or its data is cleared. Never clear/uninstall Raven with pending recovery.

## External app files

Enable **Settings > Backup preferences > Include external app data** before making the backup. This adds the app’s primary shared-storage `Android/data/<package>/files` directory to its ZIP. It supports external payloads up to 512 GiB with ZIP64. The private archive limit remains 16 GiB. Files stored in arbitrary shared folders, Android/obb, or a removable volume’s app directory are outside this option.

Use a local backup drive that supports files larger than 4 GiB. Raven streams external files into the ZIP and records their checksum as it finishes. Allow room for one finished ZIP, including private data and any APKs. This build reads earlier Raven ZIPs, including format 3 external backups. New external backups use format 4 and require a Raven build with streaming support. Restore streams validated files into staging on primary storage before replacing the app’s external files. Allow space there for a full new copy even if the app already has external files. During replacement Raven keeps the old directory for interrupted-restore recovery. After a completed restore, the retained recovery ZIP covers private data only.

## Delete and Purge

**Delete selected** permanently deletes only reviewed ZIPs selected in Restore. **Purge all backups**, in Settings > Advanced, reviews all descendants of the selected folder and then asks to empty it. It includes ZIPs, non-ZIP files, hidden files, unfinished files and nested/empty subfolders. The selected folder itself is kept. Changed contents require review again; a storage failure after deletion begins may leave a partial purge. Purge targets the shared backup folder. Delete selected targets the reviewed ZIPs in that folder or managed recovery storage. Neither operation deletes installed app data. There is no recycle bin in this workflow.

## Store attribution

New full backups record the installing package and its signing-certificate hashes where available. Missing-app restore reuses that attribution only when the installed store matches those hashes. Old archives lack this information. **Repair Google Play link** reinstalls the exact currently installed signed APKs, retaining data; use it only for apps originally acquired through Google Play. It does not supply a license or subscription. The store still checks the real account's entitlement.

## Current limits

Device and firmware differences require migration confirmation. See [Migration](MIGRATION.md) and [Recovery backups](RECOVERY.md).

- Only the observed Odin OEM root protocol is implemented; no generic su/Magisk backend.
- User 0 and eligible app identities only; user-app migration requires explicit confirmation. Eligible system apps are data-only and require strict same-identity/device/firmware restore.
- Live metadata checks were demonstrated with SELinux Permissive. Enforcing/cross-firmware restore is unverified; Raven does not change enforcement.
- No Android Keystore export, account recovery, runtime-permission restoration, store-entitlement transfer, game downloads outside the supported external app files directory, or general ACL/capability preservation.
- ZIPs are unencrypted and unauthenticated. Imported data and scripts require trust.
- Backups force-stop apps; schedules can interrupt an app in use unless charging/idle conditions are chosen. Android may delay jobs. First unlock after reboot is required.
- Local storage and a portable SD provider with read/write/rename/sync support were tested; cloud-document providers are not established as compatible.
- Clearing data/uninstalling/resetting loses approval. Complete the guided setup again. Keystore-bound data, accounts, runtime grants and store entitlement still require their own recovery.

## Permissions

The manifest requests installed-package visibility, all-files access, reboot reception, optional Usage access, foreground data-sync operation, CPU wake lock, notification access and battery-optimization exemption. These support app selection, root-script launchers and scheduling. The manifest does not request Internet permission. Android system pickers/keyboards and other apps keep their own appearance.

## Fingerprint spoofing and migration

New backups record framework and vendor fingerprints separately from the advertised device fingerprint. Matching base identities, model and Android API recognize spoof-only identity changes. Older backups retain their model/API information; a recognized stock-318 spoof pair can match automatically. Other identity changes now trigger migration review. Malformed or incomplete metadata still fails validation. Confirmation does not bypass checksums, APK signatures, versions, ownership, modes or supported security-label rules.

## Advanced tools

The Advanced card in Settings starts collapsed. Tap its header or chevron to expand or hide the tools. Each filled accent button has a description directly below it.

- **Rerun Guided Setup:** opens setup again to check storage, root access and backup/restore with the disposable helper app. Checks start when you press Run checks.
- **Verify backup folder:** checks root access, validates all archives on demand and reports failures without stopping at the first, and checks read/write/rename/delete using its own temporary file. It does not restore app data.
- **Test root access:** checks Raven can run commands through the OEM privileged service; it does not replace guided setup approval.
- **Recover pending restore:** returns affected apps to their state before an interrupted restore, leaving them stopped. Use only when recovery is pending.
- **Purge all backups:** reviews all contents of the selected backup folder before asking to permanently empty it.
- **Rehearse a restore:** validates and extracts a chosen ZIP in a separate test area without replacing installed app data. It does not prove the app will work after a real restore.

**Disable schedule** stops future scheduled backups. **Allow background backups** requests exemption from Android battery optimization; it does not create a schedule or guarantee exact run times.

## Backup behavior

### Backup contents and selection

External app data is included by default for new settings; an existing explicit choice is preserved. Confirmation states whether primary external files are included. This covers internal shared storage's Android/data/<package>/files, not arbitrary game libraries, ROM folders, or physical SD-card app directories. System-app backups always omit installers, even when installer backup is enabled for a mixed selection.

Backup has Refresh, Show selected, Select all and Clear. Selections survive searching and filtering. In landscape these four equal compact buttons share one row with the backup action, which receives the remaining width. Portrait uses a separate backup row.

Android Developer Verifier, Android System Key Verifier and Android System SafetyCore appear only under **System apps & services**, even when installed as user packages. This grouping does not change Android's system flag or backup/restore policy.

### Supported system data

System-app data is limited to supported identities. Ordinary per-app UIDs without a shared identity are allowed; the special shared system identity is restricted to com.odin.gameassistant. Other shared/core identities remain unsupported. System restores require the same installed system identity, UID, signature, version, model, Android version and underlying firmware. General migration or signature/version preferences do not bypass these checks. A system app that restarts after being stopped is refused. Game Assistant on the tested Odin 3 cannot stay stopped and is refused; its backup/restore is unsupported. Files outside supported private/primary-external directories are not captured.

### Long operations and native burn-in protection

Manual operations use a data-sync foreground service with an ongoing notification and a partial CPU wake lock. Notification access is optional; Android may show its own permission dialog. The service releases its wake lock when work finishes and does not automatically retry an interrupted restore. Android 15 limits data-sync foreground services to six background hours per 24-hour period; reopening the app resets that allowance. Battery optimization exemption may still be needed for unattended use. Device testing covered screen-off operation and a completed long batch; Android or firmware can still interrupt work.

Progress dialogs contain text only. Raven does not implement its own pixel shift or change AYN's burn-in settings. The Odin firmware's native static-frame detector can offset the whole display, including dialogs; whole-display offsets were observed with the progress popup open on Odin 3.

### Preparation and backup order

Set **Settings → Backup preferences → Backup order** to Package ID, App name or Size, then choose **↑ Ascending** or **↓ Descending**. Names and package IDs use A–Z or Z–A; size uses smallest or largest first. Earlier smallest/largest-first settings are preserved. Manual and scheduled runs use the saved choice. Preparation estimates the selected apps before backup; ZIP sizes differ because of compression and exclusions.

Optional **Allow app size access** opens Android's Usage access settings. With permission, external data enabled and an app on internal storage, preparation uses per-package Android storage statistics, subtracts cache and adds selected installer sizes. Private-only backups, shared system identities and unavailable statistics use checked root directory counts. Android's totals may include app media outside Raven's scope, so sizes remain estimates. No Usage access is required to back up.

External app files are omitted only when a checked root job confirms that the files directory is absent or contains no files. The installer and private data still back up. Empty files count as content. Unreadable directories, incomplete checks and invalid sizes fail clearly instead of silently omitting data. Root-job results are read from a checked log rather than treating an empty OEM reply as a size. Existing backup formats and the restore reader are unchanged.

Completed-app reports list one app per line, including partial-batch failures. Backup streaming keeps one compression level throughout each ZIP to avoid the Android ZIP entry corruption seen when changing levels mid-archive.

Guided setup offers optional **App sizes** access after required storage access and before device checks. **Allow app size access (optional)** opens Android’s Usage access page; **Skip for now** continues with file counting. Returning from Android refreshes the status without starting checks. Required folder/storage/live-restore approval is unchanged, and app-size access can be enabled later in Backup preferences.

System apps that keep running show **App can’t be backed up** with a Close button. There is no force-close retry or temporary disable/resume option. Persistent system apps are checked before a normal stop is attempted, so an unsupported running app is left alone. Missing or failed process enumeration also refuses the backup. No completed backup is published for the rejected app.


## Settings export and import

Settings > Settings & configuration has Export settings and Import settings. Export saves a small JSON file through Android's file picker: light/dark/system theme, accent, installer/external-data preferences, backup ordering, restore switches and scheduled app/interval/charging/idle choices. Keep the file on storage that survives a reset. It contains app package names, so share it only intentionally. It is separate from app backup ZIPs and contains no app data.

Import validates the file and shows the exact settings before confirmation. Cancelling changes nothing. Imported schedules are kept OFF; open Choose apps & schedule to review available installed apps, frequency and conditions, then enable using this device's current backup folder. A file without a schedule disables and clears the previous schedule. Signature/version override switches are shown explicitly in the review; import only settings you trust.

The current backup-folder grant and device verification remain unchanged. Folder permissions, root/live-restore approval, Android permissions, recovery state, backup ZIPs and history are never imported. On a fresh device or after reset, first complete guided setup and choose the backup folder. Enable optional Usage access/background allowance separately. Export/import is blocked while a manual or scheduled operation runs; applying import is also refused with pending restore recovery. Files over 64 KiB, unsupported schemas, unknown preference fields and invalid values are rejected before applying settings.

Settings transfer passed parser and isolated device preference-application tests. Imported schedules remain disabled and the actual device folder/approval are preserved. Do not install any update during an ongoing backup or restore.


## Faster restore review

For seekable local/SD ZIPs, queue review reads and checks the ZIP index, manifest and small external checksum footer. It does not scan all payload contents up front. Each app’s private payloads and installers are fully CRC/hash/path/identity checked at its turn before installation or data replacement; invalid private archives/installers are skipped with a reason. Large external files are validated and written into temporary staging together in one read pass, including ZIP CRC, TAR safety and SHA-256. Only a successful stage can replace the original files. Staging becomes the final app files through a rename, avoiding a second full copy. Retained recovery ZIPs still cover private data only. External staging/data failures stop the queue; earlier successful restores remain restored.

Backup changes after review are refused using file identity plus pinned metadata; payload checks remain mandatory. Non-seekable inputs and old multi-app ZIPs retain the full-validation review path. Advanced Verify backup folder and rehearsal still perform full scans. Cancelling a review does not restore data. An interrupted/failed pre-replacement external stage now clears its recovery flag after safe cleanup, while a cleanup failure preserves the recovery journal.


## Special private-data files

Legitimate newline/carriage-return filenames are preserved using NUL-delimited path inventories and filename-independent SHA checks. Absolute paths, NUL bytes, traversal components and backslashes remain rejected. GNU long filename and link-target metadata is bounded and UTF-8 checked.

Private TAR validation accepts symbolic links only with an explicit app identity, targeting `/storage/emulated/0/Android/data/<same-package>/files/<safe-relative-path>`. Links cannot be parents of archived files; hard links, cross-app targets and targets outside that folder remain blocked. A restore including external data verifies that each link target is a regular file in the successfully validated external stage before replacing private data. Private-only recovery can preserve these links while keeping existing external files. Link ownership, modes, labels and the exact target are checked without following it during private extraction/verification.

Private named pipes (FIFOs) are recreated as empty IPC endpoints with checked owner/mode/label metadata. Raven never reads or hashes a FIFO as file contents. Payload-bearing FIFOs, device nodes and unsupported special files remain refused. Connections and in-flight pipe data cannot be backed up. External TARs continue to accept only ordinary files/directories.

The bundled setup helper uses a no-display activity and finishes without creating a visible window. This removes its window transitions; it does not establish the cause of any separate display artifact.
