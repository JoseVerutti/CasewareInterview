# Pending template updates: projection and pending rule

Part 2 of the take-home. This slice implements the core the design depends on:

- the **event contract** the Engagement Management System (EMS) writes to its outbox;
- the **projection** that turns those events into one small row per engagement, safe under
  standard SQS (at-least-once, unordered) delivery;
- the append-only **decision log** (apply and decline history);
- the **pending rule** evaluated on every list read, including accumulated updates and declines;
- the **summary cache key**, which contains no firm data.

Out of scope: AWS infrastructure, the Bedrock summary pipeline and the UI. Storage is behind
interfaces with in-memory implementations; the DynamoDB mapping is documented on each interface.

## Run

```
mvn test
```

Java 21, JUnit 5 and jqwik (property-based tests). No other dependencies.

## Design decisions in the code

**Events carry the full state after the change, not a delta.** Each event includes the engagement's
`currentVersion`, `declinedUpTo` and `archived` flag. With the `stateSeq` guard the projection becomes
"newest event wins", which converges under any delivery order. With deltas it does not: if an
"opened" event (seq 3) arrives before an earlier "declined up to v4" event (seq 2), the decline is
dropped as stale and v4 would wrongly show as pending again. This requires the EMS to keep
`declinedUpTo` with the engagement, which it can because it already processes decisions.
See `ProjectionTest.lateDeclineCannotBeLostBecauseNewerSnapshotsCarryIt`.

**Correctness comes from the conditional write, not queue order.** `putIfNewer` maps to a DynamoDB
`PutItem` with `attribute_not_exists(firmId) OR stateSeq < :seq`. That is why standard SQS is enough
and FIFO is not needed. `ConvergenceProperties` checks it: random valid histories, delivered shuffled
with duplicates, always end in the newest state and the same pending status.

**Decisions are logged before the state and regardless of freshness.** A decision that arrives late
is still a real decision, so it belongs in the history even when the state ignores it. Logging is
idempotent by `(engagement, stateSeq)`, and writing it first means a crash between the two writes is
repaired by redelivery.

**Unknown is never "up to date".** An engagement with no row is `NotVerified`. This is how progressive
capture (no backfill) stays honest.

**One entry for accumulated updates.** An engagement on v1 when v4 is published is `Pending` with the
summary key `template#1#4` (direct diff, the net effect). After a decline up to v4, v5 makes it pending
again and `newSinceDeclineKey()` points to `template#4#5` for the "new since you declined" highlight.

**A lagging catalog replica is not "pending".** If the user just applied v22 and the regional replica
still says v21, the engagement is `UpToDate`.

## Tests

| Class | What it covers |
| --- | --- |
| `EngagementEventTest` | Contract validation at the boundary |
| `ProjectionTest` | New rows, duplicates, stale events, the late-decline case, contract violations |
| `ProjectionHandlerTest` | Late decisions, redelivery, history order, conditional write |
| `PendingRuleTest` | Not verified, up to date, lagging replica, accumulated updates, declines, archived |
| `ConvergenceProperties` | Any delivery order with duplicates converges; each decision logged once |

## Layout

```
src/main/java/com/example/pendingupdates
  EngagementEvent, EngagementSnapshot, EngagementId   event contract
  Projection, ProjectionResult, ProjectionHandler     consumer logic
  EngagementStateStore (+ InMemory)                   firm table, conditional write
  DecisionLog (+ InMemory), DecisionRecord            append-only history
  PendingRule, PendingStatus, SummaryKey              list-read logic, cache key
```
