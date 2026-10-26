# Recovery backups — 1.6.1

Before a user-initiated restore replaces data, Raven creates and validates a temporary transaction snapshot. It also retains a separate, standalone recovery ZIP for each app, containing the app's data immediately before restore. A newly installed app may have blank data; its recovery backup returns it to that state.

Retained files use names such as `Recovery_AppName_10-24-2026_a1b2c3d4.zip`. They appear alongside normal backups in Restore with a **Recovery** tag and **Recovery restore** action. Backup details and restore review identify them as recovery backups. Multiple recovery versions of the same app may be retained. Dates include seconds to help distinguish versions. Recovery snapshots are data-only: the matching signed app version must be installed before they can be restored. Restoring one also saves a recovery copy of the app's current data, allowing another undo if needed.

## Select and delete recovery backups

Open **Settings > Backup storage > Manage recovery backups**. The themed popup lists each recovery backup by app name, date and size. Check individual entries or use **Select all**. **Delete selected** is disabled until you select an entry. Review the second confirmation popup, then press **Delete**. Cancel leaves the files intact. Deletion does not clear installed app data or normal backups.

Deletion is blocked while Raven has an interrupted restore that still needs recovery. Use **Settings > Advanced > Recover pending restore** first. Cleanup validates each selected file's location and reviewed metadata before deletion. It does not traverse links or remove files outside Raven's managed recovery folder. The Restore tab's ordinary Delete selected action can also remove managed recovery entries.

## Storage and limitations

Managed recovery ZIPs are kept in Raven's private app storage, separate from the chosen normal backup folder. They remain available even if that folder is unavailable. They use internal storage and remain until deleted. **Uninstalling Raven, clearing its app data or factory resetting removes them.** Keep normal backups separately for long-term protection.

**Settings > Advanced > Purge all backups** affects the selected normal-backup folder, not managed recovery storage. The recovery manager does not discover or automatically delete legacy exported ZIPs in the old `RavenBackupRollbacks` folder; those can be imported manually if needed.

The recovery archive carries a recovery marker, but the same archive, package-version/signature, ownership, permission and SELinux-label checks apply. It is not a system image or a way to recover Android Keystore secrets. Confirm app data and game progress inside the restored app before deleting its recovery copy.
