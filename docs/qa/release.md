# QA release assessment

Baseline: main 6452d3f. Date: 2026-10-28. Documentation is prepared; device acceptance remains pending. There is no basis to claim full acceptance.

## Historical defect retest queue

Source: developer/bug.md. These issues were not reproduced in this run. Priorities and owners are proposals.

| ID | Issue | Proposed priority / owner | Status |
| --- | --- | --- | --- |
| B1 | Cat tail disappears on the card | P2 / A | Reproduction pending |
| B2 | Lock screen launches twice | P1 / A+B | Historically marked fixed; retest pending |
| B3 | Opening records during focus causes failure | P1 / A+B | Reproduction pending; T8 |
| B4 | Weekday label disappears in statistics | P2 / A | Reproduction pending |
| B5 | Allowlisted applications cannot be used normally | P1 / A+B | Hardware reproduction pending |
| B6 | Card model appears too small | P2 / A | Expected size needs clarification |
| B7 | Pie chart overlaps its text | P2 / A | Reproduction pending |

P1 blocks core use or data correctness. P2 covers display/interaction defects with workarounds. Record the fix commit, device, and retest outcome. Feature requests in the original list are not automatically confirmed defects or release blockers.

## Release criteria

Execute T1–T8 on the same delivery revision; prevent duplicate settlement; resolve core P1 issues. Pass T9 if locking is demonstrated. Save the demo recording and build identity. C collects results; A/B confirm interface fixes. Record actual member approval only after it happens.

## Checks performed

Markdown links within the QA pack: passed. git diff --check: passed. Build outcome will be appended after the current Gradle run. No new device, usability, or long-term effectiveness evidence.

Final build result: gradlew.bat --offline testDebugUnitTest assembleDebug succeeded (42 tasks). assembleDebug produced the APK. testDebugUnitTest reported NO-SOURCE: no unit tests executed. Existing deprecation warnings remain. Runtime acceptance is still pending.

