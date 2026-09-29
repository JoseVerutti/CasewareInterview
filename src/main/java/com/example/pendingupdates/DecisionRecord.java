package com.example.pendingupdates;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** One entry of a firm's append-only apply/decline history. */
public record DecisionRecord(EngagementId id, long stateSeq, String templateId, int fromVersion, int toVersion,
                             Decision decision, String userId, Instant decidedAt) {

    public enum Decision {
        APPLIED,
        DECLINED
    }

    public DecisionRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(decidedAt, "decidedAt");
        Checks.text(templateId, "templateId");
        Checks.text(userId, "userId");
        Checks.require(stateSeq >= 1, "stateSeq must be >= 1");
        Checks.require(fromVersion >= 1 && toVersion > fromVersion, "a decision covers fromVersion < toVersion");
    }

    /** The decision an event records, if it records one. */
    public static Optional<DecisionRecord> of(EngagementEvent event) {
        return switch (event) {
            case EngagementEvent.UpdateApplied applied -> Optional.of(new DecisionRecord(
                    applied.id(), applied.stateSeq(), applied.snapshot().templateId(),
                    applied.fromVersion(), applied.toVersion(), Decision.APPLIED,
                    applied.userId(), applied.occurredAt()));
            case EngagementEvent.UpdateDeclined declined -> Optional.of(new DecisionRecord(
                    declined.id(), declined.stateSeq(), declined.snapshot().templateId(),
                    declined.snapshot().currentVersion(), declined.declinedVersion(), Decision.DECLINED,
                    declined.userId(), declined.occurredAt()));
            case EngagementEvent.Created created -> Optional.empty();
            case EngagementEvent.Loaded loaded -> Optional.empty();
            case EngagementEvent.Archived archived -> Optional.empty();
        };
    }
}
