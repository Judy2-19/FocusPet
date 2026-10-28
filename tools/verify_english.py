"""Check English source coverage, renamed artwork, and catalog upgrade invariants."""
from pathlib import Path
import re
import sqlite3
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / 'app/src/main'
han = re.compile(r'[\u3400-\u9fff]')
for path in MAIN.rglob('*'):
    if path.is_file() and path.suffix in {'.kt', '.xml', '.html'} and 'values-zh' not in path.parts:
        assert not han.search(path.read_text(encoding='utf-8')), path
for path in (ROOT/'artwork').rglob('*'):
    assert not han.search(path.name), path
for path in (MAIN/'res/values').glob('*strings.xml'):
    default = {e.attrib['name']: e.text for e in ET.parse(path).getroot()}
    english = {e.attrib['name']: e.text for e in ET.parse(MAIN/'res/values-en'/path.name).getroot()}
    assert default == english, path

# Reproduce the migration's bound SQL against an existing catalog with linked progress.
source = (MAIN/'java/com/example/focuspets/db/InitialDataProvider.kt').read_text(encoding='utf-8')
pets = re.findall(r'PetEntity\((\d+), "([^"]+)", "[^"]+", Rarity.\w+, \d+, "([^"]+)"\)', source)
assert len(pets) == 9
db = sqlite3.connect(':memory:')
db.execute('PRAGMA foreign_keys=ON')
db.execute('CREATE TABLE pets(id INTEGER PRIMARY KEY, name TEXT, description TEXT)')
db.execute('CREATE TABLE user_collection(pet_id INTEGER REFERENCES pets(id) ON DELETE CASCADE)')
db.execute('CREATE TABLE focus_records(id INTEGER PRIMARY KEY, content TEXT, points INTEGER)')
for pet_id, _, _ in pets:
    db.execute('INSERT INTO pets VALUES(?,?,?)', (int(pet_id), '\u5c0f\u732b\u54aa', 'legacy description'))
db.executemany('INSERT INTO user_collection VALUES(?)', [(1,), (2,), (4,)])
db.execute('INSERT INTO focus_records VALUES(1,?,25)', ('user activity',))
before = list(db.execute('SELECT * FROM user_collection')), list(db.execute('SELECT * FROM focus_records'))
migration = (MAIN/'java/com/example/focuspets/db/AppDatabase.kt').read_text(encoding='utf-8')
sql = re.search(r'"(UPDATE pets SET name=\?, description=\? WHERE id=\?)"', migration).group(1)
for pet_id, name, description in pets:
    db.execute(sql, (name, description, int(pet_id)))
after = list(db.execute('SELECT * FROM user_collection')), list(db.execute('SELECT * FROM focus_records'))
assert before == after
assert list(db.execute('SELECT name FROM pets ORDER BY id')) == [(name,) for _,name,_ in pets]

# All currently available original cat artwork references still resolve after renaming.
script = (ROOT/'process_cat_assets.py').read_text(encoding='utf-8')
sources = re.findall(r'\("([^"]+\.jpg)", "[^"]+"\)', script)
assert sources
for relative in sources:
    assert (ROOT/'artwork/starter-cat'/relative).exists(), relative
print('PASS: English source/resources, artwork references, catalog migration and retained progress')
