# Building from source

The tested build is Windows PowerShell with a JDK and Android SDK platform 35/build-tools. No Gradle wrapper, SDK binaries or release signing key are included. The app declares minimum API 30 and targets API 35.

## Standard Android SDK layout

Install a JDK with `javac`, `jar` and `keytool`, Android SDK platform `android-35`, and Android build-tools `35.0.0` (or provide the installed version). Then run from the repository root:

```powershell
$env:JAVA_HOME = 'C:\Java\jdk'
$env:ANDROID_SDK_ROOT = 'C:\Android\Sdk'
.\build.ps1 -BuildToolsVersion '35.0.0'
```

The script creates a locally signed APK at `dist/RavenBackup-local.apk`. It generates a development key under ignored `.local/` on the first local build. This local key/password is for development only; do not use it for public release signing.

A flat toolchain layout is also supported through `-Toolchain`: `Java/`, `Android-Build-Tools/`, and `Android-SDK/platforms/android-35/android.jar` under one directory.

```powershell
.\build.ps1 -Toolchain 'C:\AndroidToolchain'
```

## Signing an update with an existing private key

Keep the keystore outside the repository and set passwords in your process environment:

```powershell
$env:RAVEN_STORE_PASS = '<your keystore password>'
$env:RAVEN_KEY_PASS = '<your key password>'
.\build.ps1 -AndroidSdk $env:ANDROID_SDK_ROOT -JavaHome $env:JAVA_HOME `
  -SigningKey 'D:\PrivateKeys\release.jks' -KeyAlias '<your alias>'
Remove-Item Env:RAVEN_STORE_PASS, Env:RAVEN_KEY_PASS
```

The distributed APK uses the existing Raven release certificate. A newly created key cannot produce an update Android will accept over it. Developers using a different key need a separate test device/install; uninstalling the public APK loses Raven's private state and live-restore approval. Do not change builds while recovery is pending.

## Host checks

```powershell
.\scripts\test.ps1 -JavaHome $env:JAVA_HOME
```

This runs archive-path/metadata guards, SELinux-label mapping, accent contrast and filename/versioning checks without connecting to a device. Build/test intermediates are ignored under `.build/`. Optional `-BuildDirectory` and `-OutputDirectory` keep intermediates elsewhere.

## Disposable fixtures

```powershell
.\build.ps1 -Target one
.\build.ps1 -Target two
```

Fixture source and manifests are under `tests/device/fixtures/`. Targets also include `generic`, `reserve`, and `generic-split`. These APKs are development tools; they are not included in the public download. They are signed with the same local development key across runs. See [Testing](TESTING.md).

The Android helper in `tests/android/ApkBackupTest.java` is historical developer test source. It is excluded from the app build. There are no exported development intent test hooks in the release app. The production **guided setup** flow uses a separate bundled helper built from `tests/device/fixtures/Seed.java` and `setup/AndroidManifest.xml`. The build automatically compiles/signs that helper with the same key as Raven and embeds it in the app; no private signing key is shipped. Local builds also build their own matching helper.

For isolated validation, `-LocalKeyDirectory` can keep the local development key outside this checkout. All signing material stays private and must not be uploaded.

## Publishing a final APK

Pass `-PublishFinal` when building the app to verify and publish it to the Workbench's `Final apks` folder. Use `-FinalApkDirectory` for a different destination. This option is rejected for fixture targets. The publisher checks package/version/signing identity, refuses downgrades or certificate changes, and archives superseded APKs for this app without overwriting existing archives or touching unrelated apps. Building without this switch leaves the final folder unchanged.

The publisher can also be called directly with `scripts/publish-apk.ps1 -Apk PATH`. For non-Workbench environments, supply `-BuildToolsDirectory`, `-JavaHome` and `-FinalDirectory`. Keep private keystores outside the public source and release archives.

## Migration identity integration checks

For development 1.5.0, an additional harness compiles the production FirmwareCheck with simulated Android and root-property adapters. It verifies warning/consent behavior without installing an app. The adapters are under `tests/firmware`, outside app sources.

Download the [org.json 20240303 JAR from Maven Central](https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar) outside the public source tree. The script verifies SHA-256 `3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed` before using it. This dependency is only for host testing and is not bundled in the APK.

```powershell
.\scripts\test-firmware.ps1 -JavaHome $env:JAVA_HOME -JsonJar C:\test-dependencies\json-20240303.jar
```

The ordinary host suite now includes migration policy tests as its sixth group. Neither host suite establishes actual cross-device app compatibility.
