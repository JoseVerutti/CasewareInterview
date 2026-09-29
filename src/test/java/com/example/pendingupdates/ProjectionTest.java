package com.example.pendingupdates;

import static com.example.pendingupdates.Fixtures.created;
import static com.example.pendingupdates.Fixtures.declined;
import static com.example.pendingupdates.Fixtures.loaded;
import static com.example.pendingupdates.Fixtures.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProjectionTest {

    @Test
    void firstEventCreatesTheRow() {
        var result = Projection.apply(Optional.empty(), created(1, 3));

        var updated = assertInstanceOf(ProjectionResult.Updated.class, result);
        assertEquals(state(1, 3, 0), updated.state());
    }

    @Test
    void normalOpenOfAnEngagementUnknownSinceLaunchRecordsItsVersion() {
        var result = Projection.apply(Optional.empty(), loaded(40, 7, 0));

        var updated = assertInstanceOf(ProjectionResult.Updated.class, result);
        assertEquals(7, updated.state().snapshot().currentVersion());
    }

    @Test
    void newerEventReplacesTheState() {
        var result = Projection.apply(Optional.of(state(1, 1, 0)), declined(2, 1, 4, 4));

        var updated = assertInstanceOf(ProjectionResult.Updated.class, result);
        assertEquals(state(2, 1, 4), updated.state());
    }

    @Test
    void redeliveredEventIsADuplicate() {
        var result = Projection.apply(Optional.of(state(2, 1, 4)), declined(2, 1, 4, 4));

        assertEquals(new ProjectionResult.Ignored(ProjectionResult.Reason.DUPLICATE), result);
    }

    @Test
    void olderEventIsStale() {
        var result = Projection.apply(Optional.of(state(3, 5, 0)), created(1, 1));

        assertEquals(new ProjectionResult.Ignored(ProjectionResult.Reason.STALE), result);
    }

    @Test
    void lateDeclineCannotBeLostBecauseNewerSnapshotsCarryIt() {
        // History: created (1), declined up to v4 (2), opened (3). SQS delivers 3 before 2.
        var afterOpen = Projection.apply(Optional.empty(), loaded(3, 1, 4));
        var stored = assertInstanceOf(ProjectionResult.Updated.class, afterOpen).state();

        var lateDecline = Projection.apply(Optional.of(stored), declined(2, 1, 4, 4));

        assertInstanceOf(ProjectionResult.Ignored.class, lateDecline);
        assertEquals(4, stored.snapshot().declinedUpTo());
    }

    @Test
    void eventForAnotherEngagementIsRejected() {
        var other = new EngagementId("firm-a", "eng-2");
        var otherState = new EngagementState(other, Fixtures.snapshot(1, 0), 1, Fixtures.at(1));

        assertThrows(IllegalArgumentException.class,
                () -> Projection.apply(Optional.of(otherState), loaded(2, 1, 0)));
    }

    @Test
    void templateChangeIsAProducerBug() {
        var otherTemplate = new EngagementState(Fixtures.ENGAGEMENT,
                new EngagementSnapshot("review-us", 1, 0, false), 1, Fixtures.at(1));

        assertThrows(IllegalStateException.class,
                () -> Projection.apply(Optional.of(otherTemplate), loaded(2, 1, 0)));
    }
}
