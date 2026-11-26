import json
from pathlib import Path
from collections import Counter
root=Path("app/src/main/assets")
course=json.loads((root/"course2026.json").read_text(encoding="utf-8"))
assert len(course)==205
assert len({item["id"] for item in course})==205
assert Counter(item["type"] for item in course)=={"Теория":150,"Практика":55}
assert {item["id"] for item in course if item["type"]=="Теория"}=={f"Q-{260000+i}" for i in range(1,151)}
assert {item["id"] for item in course if item["type"]=="Практика"}=={f"Q-{261000+i}" for i in range(1,56)}
assert all(item["question"].strip() and item["answer"].strip() and item["topic"].startswith("2026 · ") for item in course)
assert len({item["topic"] for item in course})==14
print("Validated 150 theory questions and 55 practical tasks")
