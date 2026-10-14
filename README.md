# Pocket Pet 96 — Touch Edition (v2.0.0)

A fully fullscreen, monochrome, touchscreen virtual pet inspired by 1990s digital companion toys. Independent project, not an official Tamagotchi/Bandai product. Original pixel graphics.

## Controls
Tap the 4 top and 4 bottom LCD icons directly. No virtual plastic shell, no A/B/C hardware buttons.
Top: STATUS, FOOD, LIGHT, GAME. Bottom: MED, CLEAN, SCOLD, ALERT.
Food: tap MEAL or SNACK. Game: tap LEFT or RIGHT. Status: tap left/right to change pages. Tap the clock header to open the clock; tap the screen to dismiss. Android back closes an opened screen. After the pet dies, tap its ghost to restart.

## Gameplay
Egg hatches after 5 minutes. Real-time progression, hunger, mood, cleanliness, sickness, discipline, light and sleeping, growth paths, weight, death and restarting. Pet state is saved on device and progresses while the game is closed. No ads, permissions, or network connection.

## Build
Minimum Android 4.4 (API 19), target Android 16 (API 36). Android 17/API 37 is expected to run through compatibility but is not device-tested. Java 8 source, Gradle 8.13, AGP 8.13.2, JDK 17, Android SDK 36. Run `gradle :app:assembleRelease`.

GitHub Actions creates a debug-key-signed APK intended for sideload testing. Because the ephemeral signing key changes between runners, uninstall previous versions when an in-place upgrade reports a signature conflict; uninstalling clears saved progress.
