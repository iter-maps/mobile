# Release hardening (not started)

Everything between "builds green" and "installable by strangers".

- **Planned:** Android release signing + reproducible-build settings, Play
  and F-Droid packaging lanes (F-Droid viability is a standing constraint:
  no proprietary deps — ADR 0003, 0011).
- **Planned:** iOS signing/TestFlight lane on the macOS CI runner.
- **Planned:** data migration policy — once local persistence lands, every
  schema change ships with a tested migration
  ([`saved-places-history.md`](saved-places-history.md)).
- **Planned:** versioned release checklist: changelog, ADR audit, REUSE lint,
  store metadata, screenshot refresh.
- **Note:** the debug default gateway origin must never survive into a release
  build; release config points only at documented public origins.

Decision: ADR lands with the first release candidate.
