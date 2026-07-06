# Design tokens

The semantic vocabulary both shells implement (ADR 0009). Names are shared;
values are platform-resolved. Material/Liquid-Glass surface colors come from
each platform's system; only the tokens below are ours.

## Brand

| Token | Value | Use |
|---|---|---|
| `brand.seed` | `#888FFA` | Seed for the Android fallback tonal palette; iOS accent. |
| `brand.ink` | `#4248C9` | Wordmark, badges, marketing surfaces. |

## Transit line identity

Line colors are **data-driven first**: a GTFS `route_color` always wins. The
tokens below are fallbacks and category colors, resolved per line outside the
Material scheme so no wallpaper palette can re-tint a metro line.

| Token | Use |
|---|---|
| `line.metro.<letter>` | Official metro line colors as published by the operator. |
| `line.tram` / `line.bus` / `line.rail` / `line.night` | Softened category fallbacks. |
| `line.onBadge` | Text on a line badge — chosen by luminance contrast, not by theme. |

## Status & severity

| Token | Meaning |
|---|---|
| `status.ok` / `status.degraded` / `status.down` | Service status strip (health vocabulary mirrors the wire `Status`). |
| `severity.info` / `severity.warning` / `severity.severe` | Alert severities, each with a `.container` background variant per theme. |
| `delay.early` / `delay.onTime` / `delay.late` | Board and leg delay coloring (early = signed negative minutes). |

## Sheet metrics

| Token | Android | iOS |
|---|---|---|
| `sheet.anchor.peek` | measured content + handle | small custom detent |
| `sheet.anchor.half` | 0.57 × screen | `.medium`-equivalent custom detent |
| `sheet.anchor.full` | 0.88 × screen | large detent below status bar |
| `sheet.corner` | 28 dp top radius | system sheet radius |

## Type & shape

Stock platform type ramps (M3 `Typography()`, SF defaults) — deliberately
uncustomized pre-1.0. Android shape scale: 8/12/16/20/28 dp.
