# Streamarr Architecture Decisions

This repository is the canonical home for architecture decisions and diagrams shared across the Streamarr codebases.

## Decisions

Architecture Decision Records live in [`adr/`](adr/). Start new decisions from [`adr/template.adoc`](adr/template.adoc) and assign the next available repository-wide number.

Diagrams belong beside the decisions they explain. Commit both the editable source and a rendered image when both are available.

## Working Proposals

Documents in `proposals/` are discussion drafts. They are not accepted architecture decisions and do not supersede ADRs.

None at present.

## Spikes

Throwaway prototypes whose conclusion an ADR depends on live in [`spikes/`](spikes/). They are evidence, not product code, and are kept only while the decision they support is in force.

- [`spikes/rebac-engine-comparison`](spikes/rebac-engine-comparison/) — Cedar versus OpenFGA, the basis for ADR 0025.

## Catalog

- [ADR 0001: PostgreSQL Over Embedded Database](adr/0001-postgresql-over-embedded-db.adoc)
- [ADR 0002: GraphQL Over REST](adr/0002-graphql-over-rest.adoc)
- [ADR 0003: Keyset Pagination Over Offset](adr/0003-keyset-pagination.adoc)
- [ADR 0004: ProcessBuilder + FFmpeg Over JNI Bindings](adr/0004-processbuilder-over-jni.adoc)
- [ADR 0005: TDD and Behavior Testing](adr/0005-tdd-and-behavior-testing.adoc)
- [ADR 0006: TMDB Metadata Provider Abstraction](adr/0006-tmdb-metadata-abstraction.adoc)
- [ADR 0007: Transcode-First With Smart Remux](adr/0007-transcode-first-with-smart-remux.adoc)
- [ADR 0008: Server-Generated HLS Playlists](adr/0008-server-generated-hls-playlists.adoc)
- [ADR 0009: H.264 + AV1 Codec Strategy](adr/0009-h264-plus-av1-codec-strategy.adoc)
- [ADR 0010: Spring Application Events for Service Decoupling](adr/0010-spring-application-events.adoc)
- [ADR 0011: Remove MapStruct](adr/0011-remove-mapstruct.adoc)
- [ADR 0012: Filepath URI Encoding](adr/0012-filepath-uri-encoding.adoc)
- [ADR 0013: Stream Decision Architecture](adr/0013-stream-decision-architecture.adoc)
- [ADR 0014: Protocol-Agnostic Pagination](adr/0014-protocol-agnostic-pagination.adoc)
- [ADR 0015: Authorization, Profiles, and Content Controls](adr/0015-authorization-profiles-and-content-controls.adoc)
- [ADR 0016: Authentication Mechanisms and Session Security](adr/0016-authentication-mechanisms-and-session-security.adoc)
- [ADR 0017: Persisted Playback-Enforcement Lifecycle](adr/0017-playback-enforcement-lifecycle.adoc)
- [ADR 0018: Live Playback Authority and Outbound Transcode Workers](adr/0018-live-playback-authority-and-outbound-workers.adoc)
- [ADR 0019: Recoverable Just-In-Time HLS Segment Delivery](adr/0019-recoverable-jit-hls-segment-delivery.adoc)
- [ADR 0020: TMDB Reachability Does Not Gate Aggregate Health](adr/0020-tmdb-reachability-does-not-gate-aggregate-health.adoc)
- [ADR 0021: Device Pairing over Streamarr Transport](adr/0021-device-pairing-over-streamarr-transport.adoc)
- [ADR 0022: Accounts Have One Home Household and Profiles Are Portable](adr/0022-single-home-accounts-and-portable-profile-sharing.adoc) — superseded by ADR 0024
- [ADR 0023: `startLetter` Is a Seek Anchor under TITLE Sort](adr/0023-start-letter-seek-pagination.adoc)
- [ADR 0024: Identity Authority: One Household per Account, One Personal Profile per Account, Portable Profiles Facilitate Sharing by Relationship](adr/0024-identity-authority-by-relationship.adoc)
- [ADR 0025: Cedar Decides Authorization; PostgreSQL Keeps the Relationships](adr/0025-cedar-decides-authorization.adoc)
- [ADR 0026: Mutations Return Payloads Whose Expected Errors Are Typed Unions; Top-Level Errors Carry Failure](adr/0026-mutation-payloads-and-error-channels.adoc)
- [ADR 0027: Library Scan and Refresh Are Claimed Atomically in the Database and Run on a Single Instance](adr/0027-library-work-claimed-in-database-single-instance.adoc)
