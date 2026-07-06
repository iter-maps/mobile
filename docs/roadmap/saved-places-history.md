# Saved places & planning history (not started)

Home/work/school shortcuts, arbitrary saved places, and a ranked "recents"
memory that makes the search page useful before the first keystroke.

- **Planned:** local persistence in the shared core (SQLDelight is the natural
  candidate — its ADR lands with the schema) for saved places, planning
  history with frequency-and-recency ranking, and the active-trip snapshot.
- **Planned:** home-sheet chips (Casa/Lavoro/Scuola + recents) and a saved
  places CRUD page in both shells.
- **Planned:** migration policy from day one — schema changes must never brick
  an installed database ([`release-hardening.md`](release-hardening.md)).
- **Note:** this data is the primary payload for private sync
  ([`sync-e2ee.md`](sync-e2ee.md)); model it E2EE-serializable from the start.

Decision: ADR lands with the first implementation change.
