# Place discovery (enrichment built, discovery surfaces planned)

Beyond "where is it": photos, summaries, opening facets, related places at the
same address, and editorial "what to see/eat/do" collections for a
destination.

- **Built:** shared-core clients for `/places/enrich` (Wikidata/Wikipedia
  seeded, attribution-carrying `Place`), `/places/image` (proxied Commons
  thumbnails), `/places/related`, `/places/collections`.
- **In progress:** place-detail enrichment — photo header with license
  attribution, description/summary, facets.
- **Planned:** collections browser per destination (see/do/eat/… kinds, the
  one-hop `wikidata` → enrich → image flow), share-alike attribution rendered
  exactly as the contract requires.
- **Planned:** related-places section on civic addresses ("same address"
  matches).
- **Note:** attribution is not optional — CC-BY-SA content carries its source
  link, and provenance fields exist on every displayed datum.

Decision: ADR 0004, 0005.
