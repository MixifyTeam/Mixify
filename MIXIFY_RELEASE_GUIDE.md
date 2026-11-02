# Mixify 1.0.7 — source release candidate

This is a rebrand of the supplied Mixify 1.9 source, not a merge of the old Mixify codebase. Existing screen layouts and playback features are retained. Existing Mixify-only features and data migrations have not been ported or verified.

## Open and test in Android Studio
1. Extract this ZIP into a NEW folder. Open the folder containing settings.gradle.kts. Do not copy these files over the old Mixify project.
2. Use Android Studio with SDK 37, NDK/CMake versions requested by the project and JDK 17. Allow Gradle sync. The supplied wrapper is retained at Gradle 8.11.1.
3. Choose app / devDebug. Test on an emulator and a real phone.
4. In PowerShell, run each command separately:

```powershell
.\gradlew.bat :wearos:assembleDevDebug
.\gradlew.bat :app:assembleDevDebug
```

The development application ID is com.mixify.app.dev. It installs beside production Mixify for testing. Production ID is com.mixify.app. Source namespaces intentionally remain com.mixify.music; they are not the installed app identity.

The launcher explicitly selects the AppCompat XML theme before super.onCreate. The supplied ZIP already contained compatible day/night themes. Reproduce launch, rotation, day/night and cold start on your device to verify the reported crash is gone.

## Before any public release
- Get the exact versionCode of the last published Mixify APK. The default here is 7, based on code 6 in the supplied local 1.0.6 source; verify the published APK too. It MUST exceed every published Mixify versionCode. Override with -Pmixify.versionCode=YOUR_HIGHER_INTEGER if needed.
- Sign with the SAME old Mixify key. Never create a new keystore for an update. Use Android Studio Generate Signed Bundle / APK, select prodRelease, and choose the old key locally. The watch APK also needs the same key: configure keystore.properties locally, using keystore.properties.example, before building it.
- The read-only v39 importer copies online liked songs, library songs and cached playlists. Complete old Mixify data compatibility remains UNVERIFIED. Back up a test device, install the actual 1.0.6 release, populate library/downloads/playlists/settings, then install signed prodRelease over it. Verify launch, database migrations and all retained data. Do not uninstall the old app as an update workaround. The old Mixify source/database schema is needed to implement migration properly.
- Original Mixify artwork is used for the default phone launcher and in-app/shared logo. Alternate styles, TV/Wear artwork, Logo.png and Banner.png still require review.
- Listen Together still defaults to the original Mixify backend and invite web origin. A Mixify server and website integration have NOT been deployed. Do not advertise these as your own server. Set LISTEN_TOGETHER_SERVER locally to a compatible tested backend and verify create/join/sync/invite behavior before release. The custom scheme is mixify://party/CODE; the old Mixify /listen?code= website flow is not implemented by this source.
- Discord application ID and other third-party service identifiers remain upstream configuration. Configure your own service applications/credentials where applicable before release.
- No Firebase config or private keys were supplied. Optional Last.fm credentials remain local configuration.
- Original LICENSE, copyright notices, contributor information and dependencies are retained. The in-app footer credits BitChord. Publish corresponding modified source alongside the APK and preserve these notices.

## Release build
Configure the old signing key in keystore.properties locally (never upload that file or key). Then:

```powershell
.\gradlew.bat :app:assembleProdRelease
```

Output: app/build/outputs/apk/prod/release/. Confirm the APK is signed with the old key. For GitHub updater discovery, rename the universal APK Mixify-v1.0.7.apk and split APKs Mixify-v1.0.7_arm64-v8a.apk, Mixify-v1.0.7_armeabi-v7a.apk, Mixify-v1.0.7_x86_64.apk. Publish only after update/data and feature testing. The updater uses MixifyTeam/Mixify. Change it if the repository moves to the new organization.

## Validation performed here
XML resources were parsed and app IDs, theme parents, version names, watch/phone ID parity and updater destinations were checked against the edited source. A Gradle build was attempted, but the environment could not download the Gradle distribution (network unreachable) and has no Android SDK. No APK was built, signed, installed, or published here. Build and runtime success remain unverified.

## Revision 2 verification and limits
Read MIXIFY_ORIGINAL_COMPARISON.md. Host SQLite queries were checked against a fixture created from the original v39 schema: online projection, missing artists, local/podcast exclusion and playlist order passed. Isolated Android instrumentation tests cover idempotence, unchanged source, malformed destination retry, unknown schema and destination conflicts; these tests have NOT run here.

Run on a disposable emulator:
```powershell
.\gradlew.bat :app:connectedDevDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.mixify.music.data.migration.LegacyMixifyLibraryImportTest"
```
The development app cannot see the production app's old song.db. Actual upgrade testing needs the old signing key and a backed-up test install of 1.0.6.

Old lock-screen, Cinema Mode, lyrics cards and Music DNA have NOT been adapted to this new project's APIs. This is the supplied new UI/data engine plus legacy online-playlist import, not full old Mixify feature parity.
