package com.example.pendingupdates;

import java.time.Instant;
import java.util.Objects;

/**
 * Events the Engagement Management System (EMS) writes to its transactional outbox.
 *
 * <p>Contract for producers:
 * <ul>
 *   <li>{@code stateSeq} increases by at least one with every change to the engagement.</li>
 *   <li>{@code snapshot} is the engagement's state after the change, not a delta.</li>
 *   <li>Events are delivered at least once and in any order (standard SQS).</li>
 * </ul>
 */
public sealed interface EngagementEvent
        permits EngagementEvent.Created, EngagementEvent.Loaded, EngagementEvent.UpdateApplied,
                EngagementEvent.UpdateDeclined, EngagementEvent.Archived {

    EngagementId id();

    long stateSeq();

    Instant occurredAt();

    EngagementSnapshot snapshot();

    /** A new engagement was created from a product template. */
    record Created(EngagementId id, long stateSeq, Instant occurredAt, EngagementSnapshot snapshot)
            implements EngagementEvent {

        public Created {
            requireCommon(id, stateSeq, occurredAt, snapshot);
            Checks.require(!snapshot.archived(), "a new engagement cannot be archived");
            Checks.require(snapshot.declinedUpTo() == EngagementSnapshot.NONE_DECLINED,
                    "a new engagement has no declines");
        }
    }

    /** The engagement was opened for normal work. Records its version at no extra load cost. */
    record Loaded(EngagementId id, long stateSeq, Instant occurredAt, EngagementSnapshot snapshot)
            implements EngagementEvent {

        public Loaded {
            requireCommon(id, stateSeq, occurredAt, snapshot);
        }
    }

    /** The user applied an update; the snapshot holds the new version. */
    record UpdateApplied(EngagementId id, long stateSeq, Instant occurredAt, EngagementSnapshot snapshot,
                         int fromVersion, String userId) implements EngagementEvent {

        public UpdateApplied {
            requireCommon(id, stateSeq, occurredAt, snapshot);
            Checks.text(userId, "userId");
            Checks.require(fromVersion >= 1, "fromVersion must be >= 1");
            Checks.require(snapshot.currentVersion() > fromVersion, "an applied update must move to a newer version");
            Checks.require(!snapshot.archived(), "an archived engagement cannot be updated");
        }

        public int toVersion() {
            return snapshot.currentVersion();
        }
    }

    /** The user declined every version up to {@code declinedVersion}. */
    record UpdateDeclined(EngagementId id, long stateSeq, Instant occurredAt, EngagementSnapshot snapshot,
                          int declinedVersion, String userId) implements EngagementEvent {

        public UpdateDeclined {
            requireCommon(id, stateSeq, occurredAt, snapshot);
            Checks.text(userId, "userId");
            Checks.require(declinedVersion > snapshot.currentVersion(),
                    "only a version newer than the current one can be declined");
            Checks.require(snapshot.declinedUpTo() >= declinedVersion, "the snapshot must include the decline");
            Checks.require(!snapshot.archived(), "an archived engagement cannot decline updates");
        }
    }

    /** The engagement was closed or archived and no longer receives updates. */
    record Archived(EngagementId id, long stateSeq, Instant occurredAt, EngagementSnapshot snapshot)
            implements EngagementEvent {

        public Archived {
            requireCommon(id, stateSeq, occurredAt, snapshot);
            Checks.require(snapshot.archived(), "an archived event must carry an archived snapshot");
        }
    }

    private static void requireCommon(EngagementId id, long stateSeq, Instant occurredAt,
                                      EngagementSnapshot snapshot) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(snapshot, "snapshot");
        Checks.require(stateSeq >= 1, "stateSeq must be >= 1");
    }
}
