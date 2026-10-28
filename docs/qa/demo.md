# Demo and user guide

## Build and installation

Open the repository in Android Studio. Configure Android SDK 34 and a Gradle 8.7-compatible JDK (17 is recommended). Sync and build normal Debug using gradlew.bat assembleDebug -PisTestBuild=false. Output: app/build/outputs/apk/debug/app-debug.apk. Install on a dedicated test device and grant necessary notification permissions. The offline core does not require server startup.

## Timer-to-pet reward demonstration (rehearsal pending)

1. Record the APK revision and enable airplane mode. Show Home, balance, and the locked dog costing 50 points.
2. Prepare a dedicated profile with 49 points and the dog still locked. Earn the points beforehand through real sessions, or explicitly label any fixture as 49 preloaded points and document its preparation. Do not use a build that unlocks every pet automatically.
3. Complete a real one-minute session. Show balance 49 → 50 and exactly one new one-minute record.
4. Unlock the dog. Show balance 50 → 0, the unlocked collection entry, and selection/interaction on Home.
5. Close normally and reopen. Show that the session and collection state persist.
6. Optionally cancel another session and show no reward. Include key values and offline status in the recording. Disclose any edited waiting time.

Run T1–T8 before presenting and prepare a local recording as fallback. This script is prepared; device setup, rehearsal, and recording have not been completed.

## Quick user guide

Set a task and duration on Focus, then start. After success, inspect the session record. Unlock an affordable pet in Collection and select it on Home. Cancellation earns no successful-session reward. Keep test fixtures in dedicated profiles.
