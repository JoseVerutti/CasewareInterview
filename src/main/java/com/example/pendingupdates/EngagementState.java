package com.example.pendingupdates;

import java.time.Instant;
import java.util.Objects;

/** One row of the firm table: the newest known snapshot of an engagement. */
public record EngagementState(EngagementId id, EngagementSnapshot snapshot, long stateSeq, Instant updatedAt) {

    public EngagementState {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(updatedAt, "updatedAt");
        Checks.require(stateSeq >= 1, "stateSeq must be >= 1");
    }

    public static EngagementState from(EngagementEvent event) {
        return new EngagementState(event.id(), event.snapshot(), event.stateSeq(), event.occurredAt());
    }
}
