# Ownership and integration

The following maps the supplied roles to repository responsibilities. It does not identify historical authors or rewrite attribution. Members must confirm names and actual contributions.

| Member           | Responsibility | Handoff |
|------------------| --- | --- |
| Liu Ziming (A)   | UI, presentation ViewModels, layouts, assets, pet interactions | Working screens; fixes for display and interaction defects |
| Zhu Jingyi (B)   | Gradle, Room, FocusService, reward/unlock/purchase rules, foundational tests | Settlement rules, migrations, and failure behavior |
| Dong Qiutongc(C) | Requirements, research, acceptance, defect tracking, demo, delivery report | Versioned evidence, coordinated fixes, and retest results |

Integration contract: FocusService emits timer/completion state and persists records; repositories expose points and unlocking; screens observe and display the resulting data. C verifies the full chain.

Workflow: report reproduction steps and device → propose A/B ownership → implement and reference the fix commit → C reruns the original case → record evidence and status. Proposed owners are not confirmed assignments.

Contribution register: each member supplies their name, commit hash, change summary, and reviewer. No team meeting or ownership agreement is claimed by these documents.
