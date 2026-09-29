package com.example.pendingupdates;

import java.util.Objects;
import java.util.Optional;

/**
 * Pure projection from EMS events to the stored engagement state.
 *
 * <p>Rule: the event with the highest {@code stateSeq} wins. Because events carry full snapshots,
 * this is enough for duplicates and out-of-order delivery to converge on the same state, which is
 * why standard SQS (at-least-once, unordered) is sufficient.
 */
public final class Projection {

    private Projection() {
    }

    public static ProjectionResult apply(Optional<EngagementState> current, EngagementEvent event) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(event, "event");

        if (current.isEmpty()) {
            return new ProjectionResult.Updated(EngagementState.from(event));
        }

        EngagementState stored = current.get();
        if (!stored.id().equals(event.id())) {
            throw new IllegalArgumentException("event for " + event.id() + " applied to state of " + stored.id());
        }
        if (!stored.snapshot().templateId().equals(event.snapshot().templateId())) {
            // An engagement never changes product; this is a producer bug and must go to the dead-letter queue.
            throw new IllegalStateException("template changed for " + event.id() + ": "
                    + stored.snapshot().templateId() + " -> " + event.snapshot().templateId());
        }
        if (event.stateSeq() == stored.stateSeq()) {
            return new ProjectionResult.Ignored(ProjectionResult.Reason.DUPLICATE);
        }
        if (event.stateSeq() < stored.stateSeq()) {
            return new ProjectionResult.Ignored(ProjectionResult.Reason.STALE);
        }
        return new ProjectionResult.Updated(EngagementState.from(event));
    }
}
