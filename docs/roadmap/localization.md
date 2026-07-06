# Localization (Italian + English only)

The served regions are Italian today; the product intent is Europe-wide, and
the string layer should not need a rewrite when that happens.

- **Built:** Android resources and iOS string catalogs exist per-platform with
  Italian and English.
- **Planned:** a shared glossary (transit vocabulary is the hard part:
  "binario"/"platform"/"Gleis") so the two shells translate consistently;
  locale-aware time/distance formatting helpers in the shared core.
- **Planned:** contribution workflow for translators (Weblate or plain PRs —
  decide when there is a second language community).
- **Note:** geocoding and collections already accept a `lang` param — plumb
  the app locale through instead of hardcoding `it`.

Decision: ADR lands with the framework choice.
