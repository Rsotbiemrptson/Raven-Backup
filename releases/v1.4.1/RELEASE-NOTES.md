# Raven Backup 1.4.1

Setup now waits for **Run checks** after you choose the folder and allow storage access. A themed **Testing…** popup shows progress. The helper diagnostic screen stays hidden while real backup, rollback, reinstall and restored-data checks run. Success shows **Done — Checks passed**; **Continue** opens Raven.

Verified on the connected Odin 2 in dark and light themes: manual start, both popups, complete restore checks, helper removal, completion retained after relaunch and normal entry after Continue. Five host checks passed. No personal app restore or backup deletion was performed. Testing on the other Odin remains deferred.

Existing installations retain settings. See [Installation](../../docs/INSTALL.md), [Factory-reset recovery](../../docs/FACTORY-RESET.md) and [Removal](../../docs/UNINSTALL.md). Root backend and firmware limits are unchanged. Google Play entitlement and Android Keystore secrets are not restored from app files. Generic su, other devices, Enforcing SELinux and arbitrary cross-firmware restore remain unverified.

GPL-3.0-only. Matching source is included; private keys and personal backups are excluded.
