# Mixify Web

The web player and its companion browser extension. The plan, and the decisions behind it, are in [`docs/web-app-build-prompt.md`](../docs/web-app-build-prompt.md).

| Package | What it is |
|---|---|
| `app/` | The site: Vite, React 19, TypeScript, React Router, TanStack Query, Zustand, Tailwind CSS v4 |
| `extension/` | Manifest V3 extension for Chromium and Firefox, built with WXT |
| `shared/` | The page ↔ extension bridge contract and client, used by both |
| `docs/` | `apple-music-dom.md` (the design measurements), `kira-notes.md` (how the reference YouTube web client plays), `youtube-playback.md`, `youtube-signin.md`, `queue-and-player.md`, `lyrics.md`, `spotify-and-discord.md` |
| `fixtures/` | Real captured responses for parser tests (see its README) |
| `scripts/` | `capture-fixtures.mjs` |

## Setup

Node 22 or newer. pnpm comes through corepack, so nothing needs a global install:

```bash
corepack pnpm --dir web install
```

## Run the site

```bash
corepack pnpm --dir web/app dev
```

It serves on `http://localhost:5173`, and only there (`strictPort`): the extension trusts that exact origin.

## Run the extension

Build both targets:

```bash
corepack pnpm --dir web/extension build
```

- **Chrome, Edge, Brave:** `chrome://extensions` → Developer mode → *Load unpacked* → `web/extension/.output/chrome-mv3`. The id is always `glfaomlndpmlkbinbaadcjcjgihbebmp`, pinned by the manifest `key` (`extension/dev-key/`), so the site finds it without configuration.
- **Firefox:** `about:debugging#/runtime/this-firefox` → *Load Temporary Add-on* → `web/extension/.output/firefox-mv3/manifest.json`.

To offer the extension as a download from the site's Extension page, package it:

```bash
corepack pnpm --dir web/extension package
```

This builds both targets and writes three files to `app/public/downloads/` (git-ignored):

- `mixify-extension-chrome.zip` and `mixify-extension-firefox.zip`. Each holds a single folder (`mixify-extension-<browser>/`), so extracting it in place leaves one folder to load unpacked.
- `extension.json`, with the version. The site compares it with the installed extension's version and shows **Update** in the sidebar and an update row on the Extension page when the zip is newer. Bump `version` in `extension/package.json` to publish an update.

`VITE_EXTENSION_CHROME_URL`, `VITE_EXTENSION_FIREFOX_URL` and `VITE_EXTENSION_MANIFEST_URL` override those links, for example to point at store listings.

The toolbar icons (`extension/public/icon/`) are rendered from `app/public/favicon.svg` by `corepack pnpm --dir web/extension icons`.

Then open `http://localhost:5173/extension`. It shows "Installed", with the version and the transport: **Direct** on Chromium (`externally_connectable`), **Page relay** on Firefox (content script).

## Configuration

| Variable | Where | Default |
|---|---|---|
| `MIXIFY_SITE_ORIGINS` | extension build | `https://mixify.co.in,http://localhost:5173` (store builds leave out localhost). Comma-separated origins the extension serves; replaces the default |
| `MIXIFY_STORE_BUILD=1` | extension build | Leaves out the dev `key` for a Chrome Web Store upload |
| `VITE_EXTENSION_ID_CHROMIUM` | app (`app/.env.local`) | The pinned dev id. Set it for a store-built extension |
| `VITE_ANDROID_APP_URL` | app | The GitHub releases page |
| `VITE_PARTY_SERVER_URL` | app (`app/.env.local`, Pages env) | None. The Listen Together server; `vite build` fails without it |

## Deploy (Cloudflare Pages)

| Setting | Value |
| --- | --- |
| Root directory | `web` |
| Build command | `pnpm build:site` |
| Output directory | `app/dist` |
| Environment | `NODE_VERSION=22`, `VITE_PARTY_SERVER_URL` |

`build:site` packages the extension first, so each deploy publishes the zips and `downloads/extension.json` with the version from `web/extension/package.json`. The Extension page polls that file and offers the update when it's newer than the installed one. Bump that version to make an update show.

## Checks

```bash
corepack pnpm --dir web -r typecheck
corepack pnpm --dir web/app build
```

## Mixify branding and extension identity

- The extension is independently identified as **Mixify** (Chromium development ID `glfaomlndpmlkbinbaadcjcjgihbebmp`, Firefox add-on ID `extension@mixify.co.in`). Older BitChord extensions are *not* interchangeable with the Mixify extension.
- Production website origin defaults to `https://mixify.co.in`; override with `MIXIFY_SITE_ORIGINS` during extension compilation if hosting the web player on a different URL. Include every exact allowed origin. Only use localhost in development builds.
- Keep `web/extension/dev-key/public-key.txt` and `extension-id.txt` together and unchanged between releases. They identify the unpacked Chromium extension. The Chrome Web Store assigns its own extension ID: set `VITE_EXTENSION_ID_CHROMIUM` in the production web app to that real published ID and rebuild.
- Use `corepack pnpm --dir web/extension icons` after changing the Mixify SVG icon. Then run `corepack pnpm --dir web/extension package` to generate the versioned `mixify-extension-*.zip` packages and `extension.json` consumed by `/extension`.
- `MIXIFY_STORE_BUILD=1` excludes localhost and the pinned development key from store submissions. Review host permissions and whether the optional Discord integration is appropriate for the distribution channel.
- The original BitChord project is GPL-3.0 licensed. Keep attribution, notices and corresponding-source availability when redistributing modified versions.
