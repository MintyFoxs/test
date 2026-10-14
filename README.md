# Карманный питомец 96 — v4.0

Independent, original touchscreen virtual pet inspired by 1990s electronic companions (not affiliated with Tamagotchi or Bandai). Original pixel art, no toy shell, no physical-style A/B/C controls. Russian interface by default.

## New features

* Android **local system notifications** about hatch/naming, illness, dirty environment, hunger, low mood and bedtime. Uses `AlarmManager` with an inexact, battery-friendly check roughly every 30 minutes, reschedules after boot, avoids repeating the same need more often than every two hours. Android Doze and battery restrictions may delay notices. Android 13+ requires POST_NOTIFICATIONS permission; none of this requires Internet, background services, or exact-alarm access.
* Pet's name is entered after egg hatches (the next time the app is open). Rename in **СТАТУС → ПЕРЕИМЕНОВАТЬ**. If you choose "Позже", the name defaults to «Питомец»; you can rename later. Name and game wins persist with existing save data.
* 10 **interactive** minigames accessed by touching **ИГРЫ**. Two pages of five: **Найди цель, Пары, Запомни, Реакция, Кроты, Больше/меньше, Лови, Лабиринт, Ритм, Считай**. They require taps on the board, correct decisions, memory, timing, or arithmetic. Success increases happiness and records the win; simply opening a minigame doesn't grant rewards. Failed games don't lower happiness.
* 1000 distinct generated pet sprites, growth, hunger, hygiene, illness, sleep, attention and real-world-time progression retained from v3. Unique appearance chosen randomly for each egg; eggs have the same visual until they hatch.

## Source and build

Android Java, no third-party runtime libraries. Minimum Android 4.4 / API 19. `compileSdk` and `targetSdk` 36. Compatible with newer Android under Android's backward compatibility rules, but real device validation for Android 17 is not yet available. Build on GitHub Actions with Java 17, Gradle 8.13, Android Gradle Plugin 8.13.2 and Android SDK 36. The GitHub Actions workflow runs pure-Java tests for all 10 minigames and 1000 sprite variants before building.

```bash
gradle :app:assembleRelease --no-daemon
```

The `release` APK is signed using GitHub runner's ephemeral **debug signing key** solely for sideload testing. **Builds with different signing keys cannot update one another.** Before uninstalling, understand that Android may delete existing locally stored pet data. Persistent production signing is not yet configured.

## Privacy / limitations

All state remains on device. No internet or analytics permissions. Notifications are inexact; force-stopped apps usually cannot receive scheduled checks until reopened. If Android 13+ notification permission is denied, no notifications appear until granted in system settings.

To start a new egg, go to **СТАТУС**, page 3, press **НОВОЕ ЯЙЦО** twice within five seconds. This resets the current pet. Name entry will appear once the new egg hatches.
