# Remaining Chinese text

This replaces the obsolete comment-only translation inventory.

Application Kotlin, HTML, manifest and default/English string resources contain no literal Chinese text. Chinese UI translations are explicitly kept in `app/src/main/res/values-zh/` to support the language selector. Source artwork directories and filenames use English.

Historical Chinese documentation (including README, cloud plans and QA notes) and user-created database content are retained. Legacy generated nickname matching uses Unicode escapes for compatibility with old installations.

See [English application migration](english-migration.md) for upgrade behavior and verification commands.
