# Product outline and requirements

FocusPets targets Android users completing study tasks. Its core loop connects offline focus sessions, pet companionship, and collection rewards. The project aims to demonstrate a usable workflow; no user study establishes improved learning outcomes.

## Core scope

| ID | Requirement | Acceptance cases |
| --- | --- | --- |
| R1 | Start an offline timer and display remaining time | T1, T2 |
| R2 | Persist successful sessions and award one point per completed minute | T2, T7 |
| R3 | Do not award successful-session points for cancellation or failure | T3, T4 |
| R4 | Unlock affordable pets, deduct costs, and prevent duplicate charges | T5, T6 |
| R5 | Select unlocked pets on the home screen and show interaction feedback | T5, T7 |
| R6 | Display session records and statistics | T8 |

Wardrobes, locking, allowlists, and leaderboards are existing extensions: regress them when included in the demonstration. Cloud weekly reports, model evaluation, and study rooms are outside this baseline's core scope. Record team decisions before expanding scope.

## Related-app research

Official product descriptions consulted on 2026-10-28. This is desk research, not hands-on testing, a pricing comparison, or an effectiveness study.

| Product | Mechanism described by its official source | Design implication (our analysis) |
| --- | --- | --- |
| [Forest](https://www.forestapp.cc/) | Connects focus with growing trees and provides focus statistics | Make individual sessions contribute to visible long-term progress |
| [Habitica](https://habitica.com/static/features) | Connects habits and tasks with game rewards and social accountability | Keep rewards understandable and basic actions brief |

FocusPets uses local Room storage for session and collection data so the core flow can be tested without a network. This does not establish superiority over either product.
