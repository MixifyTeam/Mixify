# Mixify branding update

Project source packages now use `com.mixify.music`; application IDs and version remain unchanged. Kotlin class filenames, themes, JNI exports, generated-resource imports and embedded watch APK names were updated together. Phone alternate icons retain their backgrounds and use the Mixify glyph. Wear OS launcher and notification/in-app marks use Mixify artwork.

Legacy storage keys, protocol fields, working service endpoints, and original license/attribution remain intentionally compatible. Do not replace these blindly.

Extract this complete ZIP into a NEW folder, open it in Android Studio and sync. Do not overlay it on an older source folder: package directories moved. Run `./gradlew :app:assembleDevDebug` (Windows: `.\gradlew.bat :app:assembleDevDebug`), then test icon selection, startup, playback, sharing and watch installation. This environment has no Android SDK; Android compilation and device verification remain pending.
