# Changelog

## 1.7.2 — restore performance, compatibility and settings transfer (November 14, 2026)

- Support newline filenames, app-scoped external symbolic links and metadata-only private FIFOs; retain strict path, ownership and device-node checks.
- Use a no-display setup helper to remove test-window transitions.
- Review local/SD restore queues using checked metadata, then validate private/APK payloads per app and validate/write external data in one pass before replacement.
- Preserve staging/rollback safety and fix clearing recovery state after failed pre-replacement staging.
- Export and review/import theme, preferences, backup ordering and schedule choices as a JSON file.
- Keep imported schedules off until reviewed and enabled; preserve the current folder and device approval.
- Reject invalid/oversized settings and never import Android grants, root approvals or recovery state.

## 1.7.1 — reliable large backups and backup controls (November 13, 2026)

- Fix corrupt streamed ZIP entries caused by changing compression levels within an archive. Re-create affected older backups from the original device; installing this release cannot repair their missing/corrupt data.
- Include primary external app files by default for new settings, show their scope at confirmation, and safely omit confirmed absent/empty external directories without omitting private data or installers.
- Show app names and batch position during backup, with one completed app per line. Add Select all and compact landscape controls while retaining selections across searches.
- Add saved Package ID, App name and Size ordering, ascending/descending, with optional Android Usage access during setup or in settings.
- Protect manual operations with a foreground notification and CPU wake lock for screen-off work. Keep progress text-only and leave native AYN burn-in protection in charge.
- Allow eligible system-app data only with strict restore identity checks; refuse running persistent apps with a clear warning. Game Assistant remains unsupported on the tested Odin 3.

## 1.7.0 — streamed external data backups (November 6, 2026)

- Add optional backup and restore of primary external app files with ZIP64 support. Stream new external backups directly into ZIPs and continue to read older archives.
- Move signature and version overrides to Restore preferences. A blocked restore identifies the differences and points to the required settings.
- Use accent-colored toggles for Backup and Restore selections and preferences.
- Align the About and License dialog widths with the shared Raven layout.


## 1.6.9 — issue reporting and signature overrides (November 6, 2026)

- Allow explicit data-only restore to an installed app with the same package ID and version code but a different signing certificate.

- Open a GitHub issue draft from the three-dot menu. The draft includes the Raven Backup version and prompts for test details.

## 1.6.8 — faster setup and failed-backup review (November 4, 2026)

- Guided setup checks root, folder operations and disposable-app backup/restore without scanning existing archives.
- Fully validate selected backups before restore; failed archives can be skipped while other selected backups proceed to review.
- Show failed validations with reasons and a persistent Check failed flag. Select failed files for optional deletion, with the existing file-change and location checks and a separate confirmation.
- Verify backup folder remains available in Advanced for an optional full scan; report all failed archives instead of stopping at the first.


## 1.6.7 — Odin 3 restore fixes and compatibility reports (November 2, 2026)

- Skip incompatible app installers, installed version/signature mismatches and missing installers, then list each app and reason in the final restore report.
- Continue other apps after a safe compatibility skip. Data/recovery failures still stop the queue and mark remaining apps as not run.
- Open apps after restore defaults to off in Settings and both restore confirmation flows; explicitly saved choices remain respected.
- Split legacy multi-app archive plans into independent app staging jobs, so cleanup of one restore cannot remove another app’s pending files.
- Include the long-filename, owner-remapping and ART profile-label fixes tested in 1.6.6 below.

## 1.6.6 — long filename restore fix (in testing, November 2, 2026)

- Accept validated GNU long-filename TAR headers created by ordinary app-data backups, preserving actual file paths for verification. Links, unsupported metadata and unsafe paths remain rejected.
- Include the backup filename when queue import fails.
- Preserve bounded GNU filename metadata during UID remapping and support observed nested ART profile paths under app_pccache.
- Odin 3 / Android 15 live test: 30 compatible backups restored with file/metadata verification; five 32-bit-only installers were incompatible with the device.

## 1.6.5 — Advanced layout and app styling (October 27, 2026)

- Match Gamepad’s surface-colored Advanced buttons, with two actions per row and shorter descriptions.
- Draw the overflow dots at the center of the button background and clear default button tints.
- Retain the shared Raven dark/light sidebar and card palette.
- Keep About focused on Raven Backup by removing the Raven family text.

## 1.6.4 — narrower About popup (October 25, 2026)

- Reduce About from 600dp to 480dp, retaining three description lines and full visibility on the Odin.
- Match the scrollable License popup to the same 480dp width.

## 1.6.3 — About layout (October 25, 2026)

- Keep the System Control-style stacked About layout and size the popup so the full text and License link fit without scrolling on the Odin.

## 1.6.2 — About cleanup (October 25, 2026)

- Remove Source & instructions from About.
- Show License as underlined accent-colored text without a button background.
- Include the solid icon-background correction from 1.6.1.

## 1.6.1 — icon background correction (October 25, 2026)

- Make the app and GitHub icon tile background solid #1F2228, repairing unintended interior transparency.
- Preserve the original rounded outline and the raven, box and green arrow artwork; no backup/restore behavior changes.

## 1.6.0 — final milestone release (October 25, 2026)

- Move Settings and a new Raven-family About dialog into a centered three-dot menu.
- Place equal-width appearance/accent buttons in one row; give settings actions visible accent backgrounds.
- Use one shared backup/restore folder, display it below the storage buttons, and place button descriptions below their controls.
- Match Backup/Restore list cards to the sidebar surface and Restore Details buttons to inactive sidebar tabs in light and dark themes.
- Keep Choose backup folder at half width, reserving the other half for the conditional storage-permission button.
- Show Allow internal-storage only while all-files access is missing or revoked.
- Move Purge all backups and its description into Advanced.
- Make Advanced a card that starts collapsed and expands from its header/chevron.
- Rename Device & recovery to Advanced, place descriptions below each button, rename Rerun Guided Setup and remove the detached live-restore status line and trailing general note.
- Clarify storage, recovery and scheduling controls; rename Purge all backups, pair schedule/disable controls, and remove Select all shown.
- Move Backup refresh to the footer; simplify Restore actions, reorder delete/restore and style Details buttons.
- Remove the Launchable apps filter.
- Add a Show selected / Show all toggle, keep results visible above the keyboard, and ignore trailing search whitespace.
- Preserve selections across searches, fix singular/plural backup counts and show a consistently dimmed inactive backup action.
- Match the System Control rounded overflow menu.
- Match the family color picker with other-app presets plus purple/yellow/white, synchronized RGB sliders, wheel/hex input, and no brightness slider.

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
