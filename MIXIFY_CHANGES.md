# Changes in this source candidate

- Mixify labels in Android phone, TV, Wear and shared UI strings; existing UI layouts retained.
- Version name 1.0.7; overridable versionCode 7 (local original 1.0.6 uses 6).
- Production package com.mixify.app; separate dev package com.mixify.app.dev; phone/watch package parity retained.
- Explicit AppCompat launcher theme before activity initialization.
- Embedded watch APKs wired through AGP artifact providers, removing the hard-coded output folder.
- Mixify repository updater, settings website/support links and upstream attribution.
- Original Mixify artwork for default phone launcher, in-app and shared logo resources.
- Existing GPL-3.0 license and third-party notices retained.

Pending: actual build/runtime verification, official artwork throughout alternate icons/TV/Wear, remaining old Mixify data mapping, old Mixify-specific feature porting, owned backend/website service integration, signing and publication.

- Read-only v39 online-library and cached-playlist importer. Host SQL checks passed; Android tests added but not run.
