# English application migration

English is the fallback resource language and the default app language. Optional Chinese UI translations live only in `app/src/main/res/values-zh/`.

The built-in pet catalog, wardrobe labels, statistics, notifications, achievements, card viewer, diagnostics, accessibility labels and backend-generated nicknames use English. User-written activities and nicknames are retained. Legacy generated nicknames are normalized for display.

Room schema version 7 updates built-in pet names and descriptions by ID. It does not replace pet rows or modify collection, wardrobe, points, focus history or sync data.

Source artwork paths were renamed to `artwork/starter-cat/` and `artwork/dog/`. Cat originals use `coat-colors/`, `dresses/` and `crowns/`, with English filenames. Asset-processing scripts reference these paths; packaged asset names were already English. Original images are unchanged.

Run checks from the project root:

```text
python tools/verify_english.py
node tools/smoke_card.js app/src/main/assets/card3d/card.html
gradlew.bat :app:assembleDebug :app:lintDebug --offline
```

The Python check validates English source coverage, default resources, original artwork references and the catalog UPDATE statement's preservation of linked progress in SQLite. It does not substitute for a Room migration test on a device.

Historical Chinese project documentation and user-entered data are not translated by the application migration. Do not clear application data to update pet names: install the new APK over the previous version to run the migration.
