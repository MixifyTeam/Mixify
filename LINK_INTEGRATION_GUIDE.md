# Mixify song / Listen Together link patch

## Android Studio
Merge the patch's app folder into the CURRENT Mixify-1.0.7 project root, replacing matching files. Stop the running app and rebuild with `.\gradlew.bat :app:assembleDevDebug`, then Run. Do not paste this patch into the old 1.0.6 project.

Remote YouTube songs now share https://mixify.co.in/open?song=VIDEO_ID. Local/non-YouTube identifiers share title/artist text rather than an unplayable link. Rooms share https://mixify.co.in/listen?code=CODE&v=107&server=ENCODED_SERVER. The actual active room backend is preserved. Incoming invitations retain the existing preview/join and server-switch confirmation UI; this patch does NOT silently join or switch a server.

The development build adds app=dev to shared URLs so the updated website explicitly opens com.mixify.app.dev. Production shares use com.mixify.app. This avoids trying to open the old production app while testing the new dev build.

## Website
The separate Mixify-Website-Links.zip contains the updated website source. Its existing design is preserved. Deploy it using the same existing hosting process/project; no deployment was made here. Install dependencies afresh on the deployment machine; OS-specific node_modules and generated output are excluded.

Website contracts now retain server, version and app variant. The Open Mixify action uses an explicit Android intent. If no matching app handles it, the browser fallback is /download/latest, which redirects to the selected production phone APK from MixifyTeam/Mixify's latest stable release. Download buttons use the same route. Directly opening a web link without the app still displays the existing landing page and download button; the page does not automatically start a download on load.

The supplied .well-known/assetlinks.json is preserved unchanged. It contains the existing production certificate fingerprint; verify it matches the key used for the 1.0.7 APK. Debug/dev builds are not added to production association. Until domain verification succeeds, the landing page's explicit Open action can be used. Do not replace the production fingerprint with the debug fingerprint.

Until a signed 1.0.7 is published, latest stable download still returns the current 1.0.6 release. New room invitations require both people to use the same new 1.0.7 client/backend. This patch does not bridge the old and new Listen Together protocols. A fallback download cannot manufacture a 1.0.7 release that has not yet been published.

## Test after deployment
- With the new dev app installed, share a song, open its URL in Chrome on Android, tap Open and verify the same song plays. The link includes app=dev.
- Create a room in the new build; share the link to a second phone with that same build. Confirm the server, preview and join. Verify actual playback synchronization.
- Cold-start and warm-start both song and room links. Rotate/reopen and ensure the invite/song is not handled repeatedly.
- Without a matching app installed, verify the landing page and Download route. Browser intent fallback handling can differ in in-app browsers; Chrome is the initial test target.
- Check invalid, duplicated and malformed code/server inputs show a readable error.

## Validation
Nine website contract tests and the website TypeScript check passed. Exact manifest paths were inspected. Existing auxiliary-worker config field names and release-counter typing errors were corrected so TypeScript passes. Android unit/instrumentation tests were updated/added, but an Android build and actual device/browser end-to-end tests could not run here (Android SDK and Gradle distribution unavailable). No backend was changed, and no GitHub release or website deployment was performed.
