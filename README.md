# Pocket Pet 96

Original pixel-art virtual pet, inspired by 1990s keychain toys. Not an official Bandai/Tamagotchi product. No original copyrighted sprites or audio are included.

## Controls
A: select item / choose left or right. B: confirm. C: back. Tap physical on-screen buttons. Menus: food (meal or snack), light, game, medicine, toilet, status, discipline. Pet starts as an egg, hatches after 5 minutes; its needs and development depend on real-world time, including while the app is closed. Save data is stored locally.

## Compatibility
Android 4.4 (API 19) minimum; target API 36. Designed for 32-bit and 64-bit phones without native libraries. Android 17 (API 37) is expected to launch through backward compatibility but requires actual testing. Portrait phone design scales to available screen space.

## Build
`gradle :app:assembleRelease` using JDK 17, Android SDK 36 and Gradle 8.13. The `release` APK is signed using the Android debug keystore as a convenience for sideloaded testing, **not** suitable for Play Store publishing. Generated APK is in `app/build/outputs/apk/release/app-release.apk`.

## Gameplay
Real-world progression; care misses influence evolution and lifespan. Hunger, happiness, hygiene, disease, sleep/light, discipline, left/right guessing game, age/weight statistics, permanent death and rebirth. No network access, accounts, ads, trackers or permissions.
