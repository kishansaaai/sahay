# Content

Two bundled domains (PRD C2/C3): `dsa` (DSA for placements) and `mleng` (ML engineering), 35 skills each,
6 misconception-tagged questions per skill.

- `src/*.txt` - compact authoring source (format documented in `scripts/build_content.py`)
- `skillgraphs/*.json`, `questions/*.json` - generated, versioned data the app and simulator both load
- Rebuild: `python scripts/build_content.py` ; validate: `python scripts/validate_content.py` (runs in CI)

Resources list titles of well-known books and courses without URLs (to avoid dead or invented links).
Status: drafted by the team lead with AI assistance and not yet reviewed by a subject expert (issue C17).
