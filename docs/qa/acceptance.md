# Offline acceptance tests and execution record

Use a dedicated test device or emulator. Record Android version, model, APK source, Git revision, date, and tester. Use a normal build with IS_TEST_BUILD=false and disconnect networking. Do not clear personal device data.

| Case | Preconditions and actions | Expected result | This run |
| --- | --- | --- | --- |
| T1 | Offline cold start; open Home, Focus, Collection | Screens open without crashing | Not executed |
| T2 | Record balance P and record count N; complete m minutes | Exactly one successful record; balance P+m; correct duration | Not executed |
| T3 | Start and cancel; wait beyond original end time | No successful reward or automatic restart | Not executed |
| T4 | In normal mode, background for over five seconds and return | Failure feedback; no successful reward; a new session can start | Not executed |
| T5 | Locked pet costs C; balance P>=C; unlock and select it | Balance P-C; unlocked collection entry; selectable on Home | Not executed |
| T6 | Attempt insufficient-balance unlock, repeat an unlocked purchase, and tap rapidly | No insufficient-balance charge, duplicate charge, or duplicate collection entry | Not executed |
| T7 | After T2/T5, close normally and reopen | Records, balance, and unlock persist; no repeated settlement | Not executed |
| T8 | Open records during a session, return, and inspect statistics | Internal navigation does not falsely fail/restart timer; chart labels remain readable | Not executed |
| T9 | On hardware, test screen locking, allowlisted apps, return, and a 25-minute session | Mode-specific behavior; usable allowlist; no repeated lock-page launch | Not executed |
| T10 | Test light/dark themes, large fonts, and TalkBack | Essential controls remain readable and usable | Not executed |

For every execution, append actual result, evidence path, and defect ID. Failed cases remain failed until a recorded fix revision is retested.

## Automated checks

Attempted gradlew.bat --offline testDebugUnitTest assembleDebug. The initial sandbox run could not write the Gradle cache. The approved retry needed SDK configuration; only the existing sdk.dir setting was copied into ignored local.properties. See release.md for the final outcome.

The baseline has no tracked app/src/test or app/src/androidTest tests. A NO-SOURCE result is not evidence of test coverage.

## Evidence

Store screenshots and bounded logs under evidence/date/case-ID without credentials. No new device evidence has been produced. T2, T5, and T7 require before/after values so preloaded points cannot conceal failures.
