package com.example.pendingupdates;

import static com.example.pendingupdates.Fixtures.ENGAGEMENT;
import static com.example.pendingupdates.Fixtures.USER;
import static com.example.pendingupdates.Fixtures.at;
import static com.example.pendingupdates.Fixtures.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** The event contract rejects inconsistent events at the boundary instead of storing them. */
class EngagementEventTest {

    @Test
    void appliedUpdateMustMoveToANewerVersion() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngagementEvent.UpdateApplied(ENGAGEMENT, 2, at(2), snapshot(4, 0), 4, USER));
    }

    @Test
    void declineMustBeIncludedInTheSnapshot() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngagementEvent.UpdateDeclined(ENGAGEMENT, 2, at(2), snapshot(1, 0), 4, USER));
    }

    @Test
    void onlyNewerVersionsCanBeDeclined() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngagementEvent.UpdateDeclined(ENGAGEMENT, 2, at(2), snapshot(4, 4), 4, USER));
    }

    @Test
    void newEngagementCarriesNoDecline() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngagementEvent.Created(ENGAGEMENT, 1, at(1), snapshot(1, 3)));
    }

    @Test
    void stateSeqMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngagementEvent.Loaded(ENGAGEMENT, 0, at(0), snapshot(1, 0)));
    }

    @Test
    void summaryKeyHasNoFirmData() {
        assertEquals("audit-ca#12#21", new SummaryKey("audit-ca", 12, 21).cacheKey());
    }
}
