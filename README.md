# Pocket Pet 96 — Touch Edition

Full-screen monochrome pixel virtual pet inspired by 1990s digital companions. Independent fan-inspired game, not an official Tamagotchi/Bandai release. All sprites and icons were created for this project.

## Touch controls
- No toy shell, plastic body, virtual A/B/C buttons, or external control panel.
- Tap the four icons at the **top** and four icons at the **bottom** directly.
- Top: STATUS, FOOD, LIGHT, GAME. Bottom: MED, CLEAN, SCOLD, ALERT.
- FOOD: directly tap MEAL or SNACK.
- GAME: tap the left or right half.
- STATUS: tap left/right to switch pages.
- Clock: tap top header; tap center to return.
- Android system Back closes sub-screens.
- After death: tap the pet in the center to start a new egg.

## Gameplay
5 minute egg incubation. Realtime growth through baby, child, teen, adult, 1000 unique procedurally drawn pets (10 ear shapes × 10 faces × 10 body markings). A new random ID is selected for each egg, without repeating the immediately previous ID. Stages preserve each pet’s identity, and care influences adult development. Hunger, happiness, hygiene, sickness, bedtime and light, discipline, attention alerts, minigame, life and rebirth. The game advances by real elapsed time even with the app closed, and stores the pet locally. No ads, registration, or Internet connection.

## Android compatibility and build
Android 4.4 (API 19) minimum, target API 36 (Android 16). Android 17/API 37 is expected through API backward compatibility but requires device testing. Java 8 source, AGP 8.13.2, JDK 17, Gradle 8.13, Android SDK 36. `gradle :app:assembleRelease`. Output: `app/build/outputs/apk/release/app-release.apk`.

The GitHub workflow signs the sideloaded test release APK with the runner's Android debug key. This key changes on different runners; uninstall previous releases if an in-place update reports a signature conflict (uninstalling deletes local pet progress). For production, use a persistent release keystore.

## New egg / variety
Each egg is assigned a random pet ID from 0000 to 0999. The egg sprite itself is intentionally the same: after hatching the generated pet has visible genetic differences. To start a new pet without waiting for death, open STATUS, go to PAGE 3 and tap NEW EGG twice within five seconds. This deliberately deletes the current pet’s progress. Previous pet saves still load.
