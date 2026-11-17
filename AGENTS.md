# Railnest TV

Railnest TV is an Android TV launcher based on Aerial TV. The app is in `app/` and uses Kotlin, Jetpack Compose, and Android TV Material components.

## Build and verify

Use JDK 17 and Android SDK 36.

```bash
./gradlew :app:assembleStandardRelease :app:testStandardReleaseUnitTest
```

The standard build declares Android's HOME intent. The guard flavor builds separately without that filter. Read [Railnest TV behavior and device notes](docs/RAILNEST-TV.md) before testing on a television. For historical device-specific context, see [the upstream Aerial TV notes](https://github.com/pengchujin/aerial-tv/tree/b3abf8fb7569f7678c2e60e9afea72539df2b7a1/docs).

- For TV performance tests, use a release build. For Android 9 and later, install with the applicable `.dm` file from the APK alongside it and check the dexopt state.
- Preserve the package identity and signing continuity for upgrades. Store signing files and household-specific settings under the ignored `local/` directory.
- Distinguish a successful build, a successful install, and behavior verified on a physical TV.

## UI and device behavior

- Keep user-facing Android strings in all five existing locales. The active phone wallpaper page also has five translations. Preserve format placeholders.
- Design for the existing 960×540dp Compose canvas. Preserve predictable remote focus and use the existing motion and key handling utilities.
- Detect device capabilities through available packages, providers, or settings before adding model checks. Document the firmware and test device for any platform-specific change.
- Preserve existing accessibility services when changing their settings. Leave factory launchers, system packages, photos, settings, and third-party apps alone unless the device owner authorizes a specific change.
- Installation instructions should only cover Railnest TV. Keep upstream copyright, font notices, and wallpaper attribution with redistributed material.
