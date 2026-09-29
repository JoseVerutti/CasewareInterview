package com.example.pendingupdates;

import java.util.Objects;

/**
 * SQS consumer logic: records decisions, then updates the engagement state.
 *
 * <p>Safe under at-least-once, unordered delivery and concurrent consumers:
 * <ol>
 *   <li>The decision is appended first and unconditionally (idempotent by stateSeq). A decision that
 *       arrives late is still a real decision, so history must not depend on state freshness, and a
 *       crash between the two writes is repaired by redelivery.</li>
 *   <li>The state is written with a conditional put, so the newest stateSeq always wins.</li>
 * </ol>
 */
public final class ProjectionHandler {

    private final EngagementStateStore store;
    private final DecisionLog decisions;

    public ProjectionHandler(EngagementStateStore store, DecisionLog decisions) {
        this.store = Objects.requireNonNull(store, "store");
        this.decisions = Objects.requireNonNull(decisions, "decisions");
    }

    public ProjectionResult handle(EngagementEvent event) {
        DecisionRecord.of(event).ifPresent(decisions::appendIfAbsent);

        ProjectionResult result = Projection.apply(store.find(event.id()), event);
        if (result instanceof ProjectionResult.Updated updated && !store.putIfNewer(updated.state())) {
            // Another consumer stored an equal or newer state between our read and our write.
            return Projection.apply(store.find(event.id()), event);
        }
        return result;
    }
}
