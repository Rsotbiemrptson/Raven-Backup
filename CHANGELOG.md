# Changelog

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
