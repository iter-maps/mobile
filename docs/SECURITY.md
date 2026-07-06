# Security policy

## Reporting a vulnerability

Report security or privacy issues **privately** via
[GitHub Security Advisories](https://github.com/iter-maps/mobile/security/advisories/new)
— not in a public issue. You'll get an acknowledgement within a week.

## Scope notes

- The app authenticates to no one by design: it holds no credentials, tokens,
  or account state beyond the optional E2EE sync secret, which never leaves
  the device unencrypted.
- The gateway origin is user-configurable; a hostile server is inside the
  threat model — the client must never grant one more than map data and the
  user's explicit queries.
- Server-side issues belong to
  [`iter-maps/server`](https://github.com/iter-maps/server/security).
