# Changelog

## 1.5.2 — final milestone release (October 24, 2026)

- Give Manage recovery backups the same filled accent button style as Choose backup folder and Purge. Visually verified on the connected Odin 2.

## 1.5.1 — development

- Shorten the migration warning and use Cancel / Continue.
- Keep standalone per-app recovery ZIPs with recognizable names, Recovery tags and a Recovery restore action.
- Add Settings > Manage recovery backups with per-backup selection, Select all, dates/sizes and deletion confirmation.
- Protect pending restores, normal backups, unrelated paths, links and changed recovery files during cleanup.
- Remove the old optional rollback-retention checkbox; user restores now retain recovery copies until managed deletion.

## 1.5.0 — development

- Record and display backup source manufacturer/model, device name and Android version.
- Replace device/Android/firmware mismatch rejection with explicit migration review and consent before installation or data replacement.
- Require acknowledgment for mixed-source restore queues; recheck consent in backend execution.
- Preserve source metadata, archive and app identity checks, destination ownership/SELinux adaptation and transactional recovery.
- Older Raven backups retain source display using their existing model/API fields. Real Odin 3 migration is not yet verified.

## 1.4.1

- Start setup tests only when Run checks is pressed.
- Show themed Testing and Done popups in dark and light modes.
- Hide helper diagnostic screens while retaining actual app-read and launch verification.
- Keep completion pending until Continue is pressed, including after relaunch.

## 1.4.0

- Guide fresh installations through backup-folder selection and storage approval before entering the app.
- Run root, folder/archive and live-restore checks automatically, with progress, retries and interrupted-restore recovery.
- Preserve configured installations across updates and offer Guided setup in Settings.
- Identify invalid backups in setup errors without exposing private working paths or deleting files.


## 1.3.2

- Add a self-service live-restore setup check after fresh install/reset, using a signed disposable helper with verified cleanup.
- Restore buttons explain missing setup instead of silently remaining disabled.
- Save framework/vendor firmware identities in new backups; recognize the documented Odin 2 stock-318 legacy fingerprint spoof while rejecting unrelated firmware mismatches.


## 1.3.1 — 2026-10-15

- Purge now empties the selected backup folder completely, including all file types and nested/empty subfolders; keeps the selected root.
- Review snapshot, containment and empty-directory checks guard deletion.
- Purge uses the same filled accent-button style as Choose backup folder.
- Icon moved to the upper left beside the app title.

## 1.3 — 2026-10-15

- App/date ZIP names with same-day version suffixes.
- Restore multi-selection and sequential queue; duplicate app-version rejection.
- Confirmed selected-ZIP deletion and initial purge workflow.
- Header icon and removal of the lower-left caption.
- Progress/completion dialogs retained across configuration changes.

## 1.2–1.2.2 — 2026-10-15

- Combined app selection and backup into Backup.
- Added light/dark/system modes, color wheel and custom accents.
- Emerald default accent and copper-raven/green-arrow launcher icon.

## 1.1 — 2026-10-15

- Themed progress, completion and restore-confirmation dialogs.
- Stored app names for uninstalled-app backup listings.
- Captured installer provenance and provided confirmed Play-link repair.

## 1.0 and earlier

- Initial device-specific backup/restore and recovery implementation, APK/split support, version history and approximate daily/weekly schedules.
