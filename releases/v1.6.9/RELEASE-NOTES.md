# Raven Backup 1.6.9

- **Report an issue** in the three-dot menu opens an editable GitHub form with the app version and prompts for test details.
- **Data-only restore across signing certificates** is available with explicit approval when the installed app has the same package ID and version code. Raven keeps the installed APK and creates a recovery backup before replacing data. Queues skip differently signed apps unless approved.

Tested on Odin 3 with two disposable apps. The single restore passed after confirmation. The queue skipped a differently signed app by default, then restored both fixtures with approval. The fixtures independently read their original files and preferences. The destination APK hash stayed identical. The GitHub form opened with version 1.6.9 and was cancelled. All eight host suites and FirmwareCheck integration passed.

Install the signed APK over Raven Backup to keep settings and permissions. After a fresh install or factory reset, choose your backup folder and run guided setup.

[Installation](https://github.com/Rsotbiemrptson/Raven-Backup/blob/main/docs/INSTALL.md) · [Factory-reset recovery](https://github.com/Rsotbiemrptson/Raven-Backup/blob/main/docs/FACTORY-RESET.md) · [Removal](https://github.com/Rsotbiemrptson/Raven-Backup/blob/main/docs/UNINSTALL.md)
