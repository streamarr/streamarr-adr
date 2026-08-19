# Prototype conclusion

Both Cedar 4.10.0 and OpenFGA 1.18.3 express the baseline Streamarr hierarchy and relationships:

- Member + active share: view yes, manage no.
- Admin + active share: view yes, edit yes.
- Admin after unshare: view no, edit no.
- Direct ProfileManager without a share: view no, manage yes.
- ServerAdmin without a share: view no, manage yes.

The later Cedar refinement separates editing from authority-creating actions. Executable contract
tests prove that a derived HouseholdAdmin may edit an actively shared Profile but cannot offer it
again, invite a direct ProfileManager, initiate a Profile claim, or migrate its lifecycle. Those
actions require direct ProfileManager or ServerAdmin authority. A Cedar `forbid` guardrail protects
this boundary even if a later policy adds a broader `permit`. The guarded operations belong to one
schema-defined action group, eliminating duplicated permit/forbid action lists, and policies are
validated against that schema when the authorizer starts.

Cedar and OpenFGA contextual tuples returned the new decision immediately after each request fact
changed. Stored OpenFGA tuples continued returning the old decision until synchronization. Most
importantly, after PostgreSQL removed an active share, stored OpenFGA tuples still allowed both
viewing and management.

OpenFGA contextual tuples avoid that drift, but Streamarr must first load the same signed and
PostgreSQL facts Cedar needs and then make a network request to another service. The OpenFGA
project's own Cedar comparison says that when every check retrieves application data first, Cedar
is the better fit.

Recommendation: use Cedar behind Streamarr's existing AuthorizationService boundary. Keep signed
identity facts and PostgreSQL relationships authoritative, validate policies against a Cedar
schema at startup, and use PostgreSQL for reverse/list queries and transactional invariants. Do
not add a persistent OpenFGA tuple store unless Streamarr later develops deeper recursive
relationships or authorization list queries that justify an outbox and bounded revocation lag.

The Cedar uber JAR ran successfully on macOS ARM64 and contains native libraries for macOS and
Linux on both ARM64 and x86-64. Its Java ecosystem adoption remains limited, so the production
decision should explicitly accept that dependency risk and retain the AuthorizationService seam.
