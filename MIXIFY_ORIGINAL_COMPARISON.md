# Original local source comparison

The supplied Mixify-Original-Source.zip is the authoritative local source for 1.0.6: application ID com.mixify.app, versionCode 6, namespace com.mixify.music, Room database version 39 (song.db).

The GitHub main snapshot inspected earlier still declared version 1.0.5. The local ZIP contains additional lock-screen, lyrics sharing, Cinema Mode and Music DNA implementation files. The new project has a different UI/data/DI/playback structure; these files cannot simply be copied across and compiled.

## Integrated in this candidate
- Official Mixify adaptive launcher artwork, density icons and in-app/shared vector logo.
- Canonical repository MixifyTeam/Mixify, including updater.
- Production version 1.0.7 / code 7 (overridable with mixify.versionCode).
- Wear bundling uses the matching AGP APK artifact provider and its build dependency.
- A read-only legacy v39 library importer copies online songs, liked-song collections and local playlist contents into the new local-playlist store. It never opens Room on song.db, changes the old database, or deletes it. Import uses deterministic IDs and a single synchronous preference commit to avoid duplicating playlists after interruption. Existing destination playlists win on ID collisions.

## Not integrated / not claimed
- Old lock-screen player, Cinema Mode, lyrics cards and Music DNA logic have not been adapted to the new playback and settings APIs.
- Old authentication, settings, downloaded audio, local-file access, album/artist bookmarks, lyrics, playback statistics and party sessions are left intact on disk but are NOT mapped to the new UI. Users may need to log in again. The old library importer intentionally skips local tracks and podcasts rather than create broken YouTube IDs.
- Old Listen Together uses a different client/protocol; its server URL has not been substituted into the Mixify client.
- App Links on mixify.co.in have not been deployed or verified.
- Original signing key is not included. In-place upgrade and imported data still require testing against the released signed 1.0.6 APK.

This remains a development candidate, not a public production release. Preserve both original projects. Do not advertise feature parity or guaranteed data migration.
