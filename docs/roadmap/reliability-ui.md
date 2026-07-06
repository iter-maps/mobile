# Reliability & rerank explanation UI (data plumbed, surfaces partial)

The server's historical-reliability tier can reorder itineraries
(`?rerank=<profile>`), annotate RT-less legs with typical delays
(`?predict=historical`), and serve per-stop delay distributions. The client's
job is to make that legible without drowning the user in statistics.

- **Built:** shared-core support for every wire field — rerank profiles on the
  plan call, `reliabilityScore`/`rerankScore`/`rerankFactors` per itinerary,
  `predictedDelay`/`predictedDelaySummary`, and the
  `/reliability/{route}/{direction}/{stop}` read endpoint.
- **In progress:** surfacing predicted delay on itinerary legs ("usually
  +3 min here") with sample-count gating so low-confidence cells stay quiet.
- **Planned:** a "why ranked here" affordance decomposing `rerankFactors`
  into the seven weighted contributions; profile picker (reliability /
  balanced / eco / comfort) in planning filters.
- **Planned:** per-stop reliability detail (tod-bucket × day-type grid) on
  stop pages.
- **Note:** all added fields are additive and optional by contract — every
  surface must render sensibly when they are absent.

Decision: ADR 0005.
