# Restore after a factory reset

These steps apply to Raven Backup 1.6.5 on a compatible Odin 2. A factory reset removes Raven's settings, folder permissions and live-restore approval. Install Raven and run setup again before restoring.

## Before resetting

1. Back up the apps you want. Enable **Include app APKs and installed splits** if you want to reinstall them from the backups as well as restore their private data.
2. Copy the backup folder and Raven APK to a computer or other storage that will not be erased. Internal-storage backups can be erased by the reset. Keep this separate copy until recovery is complete.
3. Resolve any pending restore before resetting. Account passwords, store purchases, Android Keystore keys and extra game downloads are not included in the ZIPs.

## Set up Raven on the wiped device

1. Complete Android setup and unlock the primary device user. Copy your backup folder back to the device, or connect the storage containing it. Keep the separate backup copy until recovery is complete.
2. Install **RavenBackup-1.6.5.apk** and open Raven. First launch shows **Raven Backup setup** before the main app. Allow APK installation from your file manager if Android asks. ADB is optional.
3. Tap **Choose backup folder**. Select your existing backups or a dedicated folder and approve Android's folder-access confirmation. The picker starts in Documents; use its navigation to select an SD card or another location if needed.
4. Tap **Allow storage access** if needed. Enable access for Raven Backup on Android's all-files access page, then go back. Android requires the user to approve these prompts; Raven cannot grant them automatically.
5. Press **Run checks**. A themed **Testing…** popup shows progress while Raven tests root, folder read/write/rename/delete access and existing ZIPs, then installs its bundled **Raven Restore Check** helper to verify backup, controlled failure/rollback, saved-APK reinstall, changed app ownership, permission modes, security labels and app reads. The helper runs with its diagnostic screen hidden. Keep the device unlocked; your other apps and backup ZIPs are not changed by this check.
6. After success, Raven removes the helper and shows a themed **Done** popup saying **Checks passed**. Tap **Continue** to open the main app. Restore is enabled on this installation and firmware. You do not need to press separate root, folder or live-restore test buttons.

If a permission is declined or folder selection is cancelled, setup keeps the current progress and explains the next step. If a check fails, fix the stated issue and tap **Retry checks**, or choose a different folder. An invalid ZIP is named in the error and is not automatically deleted. Raven does not enter the main app before the required setup passes.

If setup is interrupted and a restore needs recovery, the wizard offers **Recover interrupted restore**. Recover first. If the disposable helper remains afterward, use **Remove leftover test app** and confirm Android's uninstall prompt, then retry. Do not clear Raven's data or uninstall it while recovery is pending.

Already-configured installations skip the wizard on normal launches and updates. To review the selected folder or repeat checks, use **Settings > Advanced > Rerun Guided Setup**. A factory reset, revoked access or loss of firmware approval requires setup again.

## Restore your apps

1. Open **Restore** and choose a backup version with **Review & restore**, or select different apps and use **Restore selected**. Choose only one backup version per app in a queue.
2. If an app is absent and its backup includes APKs, review and confirm **Install missing apps from backup**. For a data-only backup, first install the matching app version and signing identity yourself. Use only backups you trust.
3. Review the apps and confirm **Restore**. Raven stops affected apps, keeps a temporary recovery copy and checks the restored files and metadata. Queues stop on error; earlier successful restores remain restored.
4. Wait for the completion popup, then open the apps and check their data. Keep your original backup ZIPs. Raven retains a data-only recovery ZIP for each app. After confirming restored data, use **Settings > Backup storage > Manage recovery backups** to select and delete those copies.
5. Sign back into Google Play with the account that owns any paid/licensed apps. On the wiped Odin test, Super Meat Boy Forever's save files restored correctly before sign-in, and the owner confirmed the game worked after signing into Play Store. A backup does not transfer store entitlement or replace sign-in. Some apps also need permissions granted again, account login or additional downloads.

## Firmware and other limits

The tested environments were stock Odin 2 firmware 1.0.0.318 and its documented fingerprint-spoofed variant, both Android 13/API 33 with SELinux Permissive. Raven does not change firmware or SELinux mode. New archives record source device and Android details plus framework/vendor identities separately from the advertised fingerprint. Different model, Android version or firmware prompts a migration warning instead of automatic rejection; confirmation does not guarantee that an app will accept the data. Real Odin 3 transfer and other firmware/SELinux policies remain unverified. See [Migration](MIGRATION.md).


Android Keystore-bound secrets, accounts, runtime permission grants and external game downloads are not restored by copying app files. Some apps will therefore require their own recovery or sign-in even when their saved files are present. See [Usage and limits](USAGE.md) and [Removal and cleanup](UNINSTALL.md).
