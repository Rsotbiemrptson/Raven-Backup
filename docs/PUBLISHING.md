# Publishing Raven Backup

Repository: [Rsotbiemrptson/RavenBackup](https://github.com/Rsotbiemrptson/Raven-Backup).

For each release:

1. Update the Android version, changelog, setup instructions and test evidence. Build with the preserved release signing identity and verify the signed APK on a supported device.
2. Put the tested APK, SHA256SUMS.txt, LICENSE, signing fingerprint and release notes under `releases/vVERSION/`. Keep the matching source, resources and build scripts in the same commit. Preserve previous releases in Git history and GitHub Releases.
3. Commit and push to `main`, tag the matching commit `vVERSION`, then create a regular GitHub **release** using that version's release notes. Attach the APK, checksum and LICENSE. GitHub automatically supplies source archives for the tag.
4. Keep setup steps and limitations prominent. The APK remains experimental and device-specific. Copying app files does not recover Android Keystore keys or Play entitlement.
5. Never upload private signing keys, passwords, personal backups, raw device logs or generated fixture APKs. Publish only this curated source tree and approved release assets, not the whole development workspace.

The current version is **1.8.0**. Its installation and factory-reset guides explain selecting the folder, allowing storage access, optionally allowing App sizes access or skipping it, pressing **Run checks**, waiting for **Done**, then pressing **Continue**. Existing verified installations keep their settings when updated with the same signing identity.

GitHub upload requires repository write access. Configure Git authentication on the publishing computer; credentials must not be embedded in source or scripts. Retain the original release keystore privately for compatible future updates.

For 1.8.0, keep the pinned Tink/Gson libraries and notices in every source archive. Publish the tested version-code-38 APK, not a later rebuild. Include the encryption guide and retain default-off encryption/Remember password. Standard su is experimental and must not be represented as tested on other devices. Verify the uploaded APK hash and compare the custom source ZIP, automatic ZIP and automatic tar.gz by file contents against the tagged commit. Keep 1.7.4 as the local known-good rollback.
