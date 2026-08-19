# PROTOTYPE — Cedar versus OpenFGA

This throwaway prototype asks one question: can Cedar or OpenFGA express Streamarr's agreed
authorization model while PostgreSQL remains the domain source of truth, and what happens when an
active profile-share change has not yet reached a separate authorization store?

"Streamarr request facts" combine the signed access-token authority snapshot with live domain
relationships loaded from PostgreSQL for the protected resource. Household role and ServerAdmin
are token facts; active share and direct ProfileManager are domain relationships.

The interactive comparison models these baseline rules:

- Household Admin implies Household Member.
- An Admin can edit a profile only while it is actively shared with their household.
- Every household member can view an actively shared profile.
- A direct Profile Manager can edit a profile but cannot view it unless it is also shared locally.
- A ServerAdmin can edit every profile but does not receive viewing access.

The Cedar contract tests additionally model the refined authority boundary:

- Authority derived from an active Household share may edit the Profile.
- Derived Household authority cannot offer another share, invite a direct ProfileManager, initiate
  a Profile claim, or migrate the Profile lifecycle.
- A direct ProfileManager or ServerAdmin may perform those portable relationship actions.
- The removed `OWNER` role fails closed.

The Cedar schema groups portable relationship mutations in one action hierarchy. Policies are
schema-validated at startup, and one `forbid` guardrail covers the group so the permit and deny
paths cannot drift through duplicated action lists.

Run from this repository's root (Maven 3.9+ and JDK 25 on the PATH):

```shell
mvn -q -f spikes/rebac-engine-comparison/pom.xml compile exec:java
```

Docker must be running. The prototype starts a disposable OpenFGA 1.18.3 container with its
in-memory datastore and removes it on exit. It does not use Streamarr's PostgreSQL database.

The Cedar contract tests do not require Docker:

```shell
mvn -q -f spikes/rebac-engine-comparison/pom.xml test
```

This spike is preserved as the evidence behind [ADR 0025](../../adr/0025-cedar-decides-authorization.adoc); its conclusion is in `NOTES.md` and the decisive tests are `CedarAuthorizerTest`.
