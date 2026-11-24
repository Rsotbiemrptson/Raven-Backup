# Storage and compatibility — Raven Backup 1.8.0

Version 1.8.0 retains the existing signing identity, preferences and folder grants when installed as an update. Android 11/API 30 remains the minimum. The tested root transport remains the Odin PServerBinder backend. Standard su is now an explicit option on already-rooted devices; that option and Retroid/other manufacturers still lack real-device acceptance.

## Backup contents

Four choices appear in Backup preferences and step 4 of guided setup:

- Internal storage: Android/data/<package>/files.
- Internal storage: the whole Android/obb/<package> folder, including files directly inside it and a nested files folder if present.
- Mounted read-write portable removable storage: Android/data/<package>/files.
- Mounted read-write portable removable storage: the whole Android/obb/<package> folder.

New choices default on; the existing internal-data preference is preserved. Missing and empty directories are omitted; zero-byte files count as content. Arbitrary game libraries, Android/media and other directories outside these app-specific scopes are not included. The choices affect new backups; restoring an archive restores its included scopes.

New scoped archives use format 5. Each source TAR is validated and rewritten into a named scope while streaming directly into one per-app ZIP. A checksum footer identifies each original volume and scope. No full external TAR is staged during backup. Total external TAR size is limited to 512 GiB. Older Raven versions cannot restore format-5 archives; this release keeps readers for formats 1–4.

On restore, each scope is validated and extracted in one pass to Raven-owned staging on its destination volume. All scopes finish validation before any live app data is replaced. Existing external directories are retained until commit and restored if application fails; retained recovery ZIPs still cover private data only. Native private ownership/mode/labels remain checked. Removable filesystem permissions are managed by Android/the filesystem; unsupported POSIX metadata is not forced onto the card.

A matching mounted portable-card UUID is preferred. If it is missing and exactly one writable portable card is available, that card is used. Ambiguous destinations or attempts to merge multiple source cards are refused. Read-only cards, adopted storage and nonstandard volume layouts remain unsupported. Keep independent backups before testing real migrations.

## App-data security labels

Raven reads app-data type declarations from the destination firmware's seapp_contexts files. It accepts declared destination root types, verifies app ownership and maps the saved root label/categories to that installed destination's context. It does not blindly apply a foreign root label or change SELinux enforcement. Special per-file labels outside the known ordinary-root/ART-profile cases remain rejected for inspection rather than guessed. This is broader app-root recognition, not universal vendor SELinux compatibility.

## System-app migration

An off-by-default Allow system-app migration switch permits app-version or firmware differences after explicit restore warning/confirmation. Same model, package, UID/shared identity and signing certificate remain required. APK capture/replacement is still forbidden for system apps. Persistent apps that cannot remain stopped are refused. A different Android/app schema may be incompatible even when file checks pass; app/device problems are possible. No real system app was modified during release tests; migration-policy branches were exercised with synthetic identities.

## Google Play / original store attribution

Installer metadata is captured for data-only as well as app-and-data backups. Android installer attribution is restored after data verification only when the original store is present with its recorded signing identity (or verified signing lineage) and the installed app matches the recorded app signing identity. Missing-APK installs also use that verified installer. Odin 3 rejects direct pm set-installer calls from UID 0, so repairing an installed app uses a same-version, same-signed APK/split reinstall with data retained and identity checked afterward. Failed attribution is a warning in the restore report, including queued restores; it does not claim that already-verified data failed to restore.

Backups without installer metadata cannot establish the original store; existing attribution is retained and reported. Use the manual Repair Google Play link action only for an app originally obtained from Play. Android attribution does not transfer an account, entitlement, license, Play Integrity result or Keystore keys. Play still determines listing visibility, ownership and update eligibility.

## Validation and limits

- Eleven dependency-free host suites; settings parser/export/import tests; legacy external-TAR validation; four-scope TAR rewriting/routing with long/newline names and traversal/unknown-scope/hash rejection.
- On Odin 3, an actual four-location backup and restore using the disposable app. All four payload hashes matched and the app independently read them through Android external-files/OBB APIs. Disabled/empty scopes were omitted. An injected failure after two external swaps restored the previous external contents.
- Legacy format-4 restore; malformed external hash/CRC/path and changed-source refusal; format-3/private-data compatibility and late queue validation are covered by the device regression tests.
- Synthetic system version/firmware allowance and strict signer/UID/model refusal. Destination app-root policy loading and source-root mapping; invalid categories, undeclared destination types and privileged child labels refused.
- Play attribution restored for data-only and APK-plus-data restores; restored private data independently read. Changed app/store signers and missing legacy metadata do not receive guessed Play attribution. The disposable app has no Play listing, so actual update delivery is not claimed.
- A preceding candidate of the same scoped streaming backend passed a 4.125 GiB streamed backup/restore with SHA-256 verification, zero-byte preservation and extra-file removal, plus guided setup rollback/reinstall/fresh-UID checks. Subsequent changes adjusted review text, metadata warnings and missing-system-app handling; focused tests were rerun on the final candidate. Large compressible synthetic data is not a real-world throughput benchmark.

Raw logs, device identifiers, test APKs and acceptance hashes stay outside public source under local Outputs staging. They must be reviewed before any stable release is promoted.

encrypted containers use `.rbk` and require a seekable local/internal or portable-card provider and their original password. Plain ZIP compatibility is retained. An explicit `su` backend is available only on already-rooted devices and is not hardware validation. Guided checks probe required tools, labels, ownership, rename and disposable restore behavior. Restore preview names destination folders/cards and conservative staging/recovery budgets; mapping and space are rechecked before replacement.
