# GraphQL subscription conventions

**Status:** proposed with [ADR 0042](../adr/0042-graphql-subscriptions-run-over-websocket-until-token-expiry.adoc). These conventions govern new Streamarr subscriptions across the server, web client and Apple client. They describe the intended contract, not functionality already implemented.

## 1. Queries establish state; subscriptions keep it current

A normal GraphQL query loads a screen. A mutation response can also provide the state the screen needs. Subscriptions then deliver changes after registration. Reconnecting clients fetch current state through ordinary queries; subscriptions do not replay missed history.

We accept the small gap between the initial query and subscription registration. Neither a universal initial snapshot nor an operation-readiness handshake is required. A WebSocket `connection_ack` acknowledges the connection, not registration of an individual operation. Revalidation after recovery has the same best-effort boundary; it is not an atomic snapshot-and-watch protocol.

The default promise is current UI state, not a durable event history, exactly-once delivery, or observation of every intermediate transition. A feature needing stronger guarantees must state that requirement and its recovery behavior explicitly.

## 2. Use explicit fields and useful payloads

Name each subscription for the domain change it delivers. Prefer separate, purpose-specific fields over a catch-all event stream. For library contents, retain:

```graphql
libraryItemAdded(libraryId: ID!)
libraryItemUpdated(libraryId: ID!)
libraryItemRemoved(libraryId: ID!)
```

These are field names and arguments, not complete SDL definitions. Each field has a typed result appropriate to its purpose. A screen may use all three through the same shared WebSocket client. Sharing a socket does not require combining schema fields.

Use scope arguments such as `libraryId`. Resolve authenticated identity, including the selected Profile, from the authenticated session rather than trusting a caller-supplied identity argument.

Addition and update payloads return the same entity types and selectable fields used by queries and mutation responses. Clients select stable identity and the fields needed to update the screen, preferably through shared fragments. “Same payload” means compatible entity data, not copying a mutation's `userErrors` envelope into every event. A removal returns a typed receipt containing the identity and scope needed to remove the item, without trying to resolve a deleted entity.

Payloads must support the promised UI update without a follow-up query for each event. Include fields needed for sorting and filtering even when those fields are not displayed. Explicit aggregate updates cover visible counts and navigation where those are part of the live feature. An ID-only notification that makes every consumer refetch is not the default.

## 3. Use ordinary normalized caching

Use Apollo's normal entity normalization and cache updates. Every entity shared across queries, mutations and subscriptions must have a consistent cache identity. A complete cached query can satisfy navigation back to a screen; subscriptions do not impose a universal refetch-on-navigation policy.

Normalized field merging does not maintain connection membership, sort order or aggregates. Consumers must update those explicitly. Reapplying an addition must not create a duplicate edge; removing an already absent item is harmless.

Do not require revision fields, deletion tombstones, or custom stale-response guards for every subscription. An older HTTP response can overwrite fields from a newer event under this baseline. We accept that limitation; a later event or revalidation can correct it. A feature that must never regress displayed state needs its own justified ordering/version contract across all contributing responses.

Profile- and Household-dependent cache data follows the existing identity-switch rules. Sharing an entity ID is not permission to share one Profile's watch state with another.

## 4. Update lists and their visible aggregates automatically

Apply changes without a “new items available” prompt. Respect the active filter and sort, including changes from watch-state subscriptions: marking a movie watched on another device removes it from an Unwatched list even if its library metadata did not change.

Maintain the portion of the list the user has loaded:

- An addition within the loaded range grows it. If 48 titles cover A–M and more pages remain, a new B title makes 49; it does not evict an existing title just to preserve 48.
- A new title beyond that range arrives through normal pagination. If the user has reached the end, matching additions can extend the list.
- Removal, or an item ceasing to match the filter, shrinks the list. Removing one of 48 leaves 47; do not fetch a replacement just to fill the original page size.
- A relevant update changes ordering and membership using the same semantics as the query. Keep stable item identities while updating the list.
- Visible counts and alphabet navigation receive their authoritative updates too. An entity update alone does not update those values.

The per-feature contract must supply enough information for these operations, including usable pagination information where membership changes affect it. Do not invent Relay cursors on the client or treat a cursor as a change-log position. Page size describes a query result, not an invariant that a live cached list must keep forever.

Deliver aggregate changes when their authoritative values change. Subscription delivery does not change the domain's recomputation schedule; for example, [ADR 0027](../adr/0027-library-operations-are-durable-jobs.adoc) defines when library operations recount the alphabet index. Declare that timing in the feature contract.

## 5. Own subscriptions at the screen's useful lifetime

Keep the current library's subscriptions active while navigating from its list to a movie and back. Release them when leaving that library scope. This preserves a useful library cache while the list component is unmounted; a component's lifetime is not necessarily the subscription's lifetime.

Do not explicitly pause subscriptions when a browser tab becomes hidden. Browsers and operating systems may suspend or discard a page regardless, so ongoing delivery while suspended is not guaranteed. On return, use the recovery behavior below instead of maintaining a custom suspension state machine.

Temporary interruptions leave the page usable with its existing data. Do not add a connection, reconnecting or stale-data indicator. Permanent authentication or authorization failures use the application's existing access behavior.

## 6. Recover silently with queries and existing transport machinery

Silently revalidate relevant queries after a WebSocket reconnect, when the browser returns online, and when the tab becomes visible. Include the current library's cached list while a movie is open; limiting recovery to mounted queries would miss it. Overlapping triggers share one recovery path rather than starting redundant refreshes. Native clients apply equivalent foreground and connection-recovery behavior through their lifecycle APIs.

These recovery queries are distinct from fetching after every ordinary event. Do not add periodic polling as a default safety net.

Use the transport library's reconnection machinery. Retry recoverable connection failures without an attempt limit, with exponential backoff and jitter capped at approximately 30 seconds. Retain fatal-error handling. Logout, permanent authentication failure, cancellation and an identity change stop obsolete recovery work.

For the web client's `graphql-ws`, configure `retryAttempts: Infinity` and a capped `retryWait`. Its default delay doubles without a cap; changing only the attempt limit would eventually leave minutes or hours between attempts. This is a client configuration, not a second reconnect loop. Configure and test equivalent intended behavior on the installed Apollo iOS transport; its defaults must not be assumed to match the web library.

Restoring a connection restores only operations that the client still considers active. A protocol `error` or `complete` ends an operation. If a documented recoverable operation failure requires resubscription, the consumer must recreate that operation and revalidate; a socket reconnect alone cannot revive it. Normal scope completion and permanent refusal do not trigger blind resubscription.

## 7. Authenticate the handshake and stop delivery at expiry

Use `graphql-transport-ws` on `/graphql`. Queries and mutations stay on HTTP; HTTP and server-sent-event subscriptions are refused. Share a WebSocket client within an authenticated client session, rather than opening a socket per consumer.

The opening HTTP handshake uses the existing access cookie for browsers or `Authorization: Bearer` for native clients, retaining the origin check. Do not send another token in `connection_init`. The server keeps the authenticated identity and expiry needed for delivery, without retaining or logging raw credentials in event payloads or diagnostics.

At the opening token's expiry, stop all data delivery and close the socket with a documented, client-tested signal. **Do not send `complete` for its active subscriptions first.** `complete` means the operation has finished, so it prevents the usual restoration of active subscriptions.

Coordinate credential renewal through the existing session machinery and obtain fresh handshake credentials before opening the replacement socket. Refreshing an HTTP cookie does not reauthenticate an existing WebSocket; `connection_init` is not a repeatable reauthentication message. A browser's `connectionParams` callback runs after the opening handshake and is too late to refresh that handshake's cookie.

Authorize each subscription through the service-layer authorization contract and apply query-equivalent data visibility. Existing authority remains bounded by the opening token's expiry under [ADR 0016](../adr/0016-authentication-mechanisms-and-session-security.adoc); this convention does not introduce immediate revocation everywhere. A denied operation can end while other authorized operations continue on the socket. No data may be delivered using expired authority or after a fail-closed authorization decision.

A Profile or Household switch disposes the old operations, reconnects with the new identity and prevents old events from repopulating the new identity's cache. Removal payloads, counts and error details must obey the same disclosure rules as ordinary query data. Each feature documents how a resource leaving its readable scope is removed from the affected view without disclosing hidden resources.

## 8. Separate domain state from subscription failure

| Outcome | Contract |
| --- | --- |
| Domain failure, such as a failed scan | Ordinary state data, such as `UNHEALTHY`; continue observing. |
| Error resolving a field in one result | A GraphQL execution result may contain `data` and `errors`; apply only usable data under the field's nullability contract. This does not inherently terminate the operation. |
| Invalid or unauthorized subscription | Sanitized GraphQL operation errors with documented machine-readable codes; do not retry a permanent refusal. |
| Terminal source failure | End the operation with a sanitized error and document whether the consumer may recreate it. |
| Scope finished or removed | Normal `complete`; do not automatically resurrect the scope. |
| Socket loss or token expiry | Restore the connection and desired active operations, then revalidate current state. |

Reuse Streamarr's established error vocabulary where the meaning matches. Keep internal diagnostics on the server. Do not add mutation-style `userErrors` to every successful subscription result or turn an ordinary domain failure into a broken connection.

## 9. Publish committed changes and bound resource use

Publish ordinary domain/application events where state changes. For transactional writes, deliver only after commit; refused operations and rolled-back transactions publish nothing. Domain events remain independent of GraphQL payload types. Virtual threads remain the application concurrency model; any required reactive adaptation stays at the GraphQL boundary.

Publish changes as they happen without a default debounce window, batch timer or latest-value-only buffer. A high-volume feature can justify an explicit coalescing contract later. This simple publishing rule does not imply a universal guarantee of every transition, global commit ordering or historical replay. Each field documents what its ordering actually covers.

Use bounded queues and the framework's transport/resource controls. Publishing must not wait for a slow client to drain its queue. If a subscriber falls behind, terminate the affected operation or connection and recover through the documented path. The failure boundary may be the shared socket, so clients cannot assume other operations will survive it. Do not silently discard events as an undocumented overflow strategy.

Cancellation, completion, expiry, failure and disconnection release registrations, buffers, timers and execution work. Verify the whole delivery path, including serialization and socket writes; bounding one early queue does not bound later queues. Pick numerical capacity limits from representative tests and configuration.

The initial deployment remains one active server per database with in-process delivery. Horizontal deployment needs cross-instance event distribution and resolution of the application's other single-instance assumptions. WebSocket support alone does not make Streamarr horizontally scalable.

## 10. Document and verify every new subscription

Each field's SDL and implementation issue must specify:

| Contract | Required detail |
| --- | --- |
| Purpose and scope | Which change, resource and identity scope it covers; who may subscribe and receive each result. |
| Initial state | The ordinary query or mutation response that populates the screen. Any stronger startup guarantee is an explicit exception. |
| Payload | Shared entity fields, removal receipts, required filter/sort data, and related aggregates/pagination data. |
| Delivery | Publish points, after-commit behavior, ordering scope, and any justified coalescing or revision requirement. |
| Lifetime | Navigation ownership, scope completion, identity changes and token expiry. |
| Recovery | Revalidation targets, retryable failures, permanent failures and buffer-overflow behavior. |

Use failing tests when implementing the feature. Verify payload-driven updates without per-event refetching; list insertion/removal/filter/sort behavior; authoritative aggregate changes; rollback and unauthorized delivery; cleanup; bounded slow-consumer handling; and recovery after disconnection and token expiry with the actual web and Apple transports. Do not test a universal no-gap startup or highest-revision rule that this convention does not promise.

The first applications are [library status (#411)](https://github.com/streamarr/streamarr-server/issues/411), [Profile watch state (#165)](https://github.com/streamarr/streamarr-server/issues/165), and [library contents (#166)](https://github.com/streamarr/streamarr-server/issues/166). Their field contracts must cover the UI behavior above; entity normalization alone does not finish the list-update work.

## Sources and precedents

- [GraphQL subscription execution](https://spec.graphql.org/September2025/#sec-Subscription) and the [WebSocket protocol](https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md) define execution and wire messages, not atomic startup or replay.
- [Apollo cache normalization](https://www.apollographql.com/docs/react/caching/overview) explains entity field merging; [cache interaction](https://www.apollographql.com/docs/react/caching/cache-interaction) covers explicit collection updates.
- [Relay connections](https://relay.dev/graphql/connections.htm) define query pagination; they do not require a live cache to retain its original page size.
- [Apollo event-based refetching](https://www.apollographql.com/docs/react/data/event-based-refetching) provides opt-in web recovery mechanisms, including visibility and online events. The application's recovery query set still needs configuration.
- [graphql-ws options](https://the-guild.dev/graphql/ws/docs/client/interfaces/ClientOptions) and [client implementation](https://github.com/enisdenjo/graphql-ws/blob/master/src/client.ts) define retry, completion and handshake timing. [Apollo iOS WebSocket transport](https://www.apollographql.com/docs/ios/networking/websocket-transport) has its own lifecycle and configuration.
- [GitLab's shared consumer](https://github.com/gitlabhq/gitlabhq/blob/master/app/assets/javascripts/actioncable_consumer.js) and [frontend guidance](https://docs.gitlab.com/development/fe_guide/graphql/) are precedents for socket sharing and recovery. GitLab uses Action Cable, so its transport defaults are not evidence of graphql-ws or Apollo iOS defaults.

These sources inform the choices; the Streamarr decisions above are the contract. In particular, no universal snapshot, revision guard, polling fallback or deliberate coalescing policy is inferred from another application's implementation.
