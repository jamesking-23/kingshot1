# KingShot Camera Lab

An early Android Camera2 prototype exploring capability-aware, minimal-processing capture controls.

## Prototype scope
- Rear-camera discovery and preview
- Camera capability inspection (including RAW support and available processing modes)
- Requests noise reduction, edge enhancement and shading Off only when the device reports support and the toggle is enabled
- JPEG capture saved to Pictures/KingShot
- GitHub Actions debug APK build

## Build
Open this repository in Android Studio with JDK 17 and Android SDK 35, or use the repository Actions workflow. Open **Actions → Build KingShot APK**, then download the **KingShot-debug-apk** artifact from a completed run.

This is an early prototype and has not yet been verified on physical Pixel hardware. Public Android APIs cannot guarantee that every ISP/vendor processing stage is bypassed. The current branch captures JPEG only; RAW/DNG, timestamp-matched capture metadata, lifecycle hardening, renderer, comparison UI and production signing remain future work.

See `KINGSHOT_BLUEPRINT.md` for product and engineering direction.
