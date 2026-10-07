# Changelog

## Unreleased

### Changed
- Toolchain: AGP 9.4.1 with built-in Kotlin (the `kotlin-android` plugin is no longer applied), Gradle 9.8.0, compileSdk 37.
- core-ktx 1.19.1 (needed compileSdk 37 and AGP 9.1+).
- Release APK is ~1.2 MB (was ~1.7 MB in 1.3.0).

## 1.3.0

> **Upgrading from an earlier release APK:** releases are now signed with a
> release key instead of the debug key. Uninstall the old version once before
> installing 1.3.0; later updates install normally.

### Fixed
- Pop counter showed "0 / 0" until the first pop.
- Challenge Mode times included the 400 ms celebration delay.
- Changing theme mid-game left the counter stale; theme changes now keep popped bubbles.
- Changing grid size mid-challenge kept the previous run's clock going.
- Resetting during the celebration could wipe out the next game.
- A best time could be saved from a broken run after the activity was recreated.
- On Android 15 the toolbar drew under the status bar and the FAB under the navigation bar.
- The overflow menu icon could be invisible when the system was in light mode.
- A second finger tapping without moving didn't pop a bubble.
- An inflated bubble drawn right after a popped one could appear faded.
- Possible crash on devices without a vibrator.
- Sound loading could race with shutdown, and an interrupted write could leave a broken cached sound.

### Changed
- Bubbles are rendered from cached sprites with hardware acceleration instead of a software layer.
- Release builds use R8 with resource shrinking (~5.9 MB → ~1.5 MB).
- Toolchain: AGP 8.13, Kotlin 2.4, Gradle 8.14, Java 17, compileSdk 36.
- Libraries: core-ktx 1.18, AppCompat 1.8, Material 1.14, ConstraintLayout 2.2, plus newer AndroidX Test.
- Releases are signed with a release key when signing secrets are configured, and the tag must match `versionName`.

### Added
- Espresso tests run in CI on an emulator; Gradle wrapper validation.
- `gradlew.bat` for Windows builds.

## 1.2.0

### Fixed
- Thread-safety issue in `SoundManager` between the sound-loading callback and playback.

### Added
- Release workflow: pushing a `v*` tag builds the APK and creates a GitHub Release.
- Dependabot for Gradle and GitHub Actions updates.
- README banner, issue and pull request templates.

## 1.1.0

### Added
- ⚡ Neon theme — electric magenta, cyan, matrix green, orange, yellow, blue.
- 🍬 Candy theme — bubblegum pink, tangerine, lemon, lime, sky blue, grape.
- ⏱️ Challenge Mode — timed runs with a personal best stored in SharedPreferences.
- ⚙️ Settings screen — sound and haptic toggles that persist across launches.
- 🧪 Unit tests (`GridMathTest`) and Espresso tests (`BubblePopTest`).
- 🏗️ CI — GitHub Actions builds the APK and runs lint on every push.

### Fixed
- Preferences not persisting between launches.
