package com.example.pendingupdates;

import static com.example.pendingupdates.Fixtures.TEMPLATE;
import static com.example.pendingupdates.Fixtures.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class PendingRuleTest {

    @Test
    void engagementNeverSeenIsNotVerifiedNeverUpToDate() {
        assertEquals(new PendingStatus.NotVerified(), PendingRule.evaluate(Optional.empty(), 21));
    }

    @Test
    void engagementOnTheLatestVersionIsUpToDate() {
        assertEquals(new PendingStatus.UpToDate(21), PendingRule.evaluate(Optional.of(state(1, 21, 0)), 21));
    }

    @Test
    void laggingCatalogReplicaDoesNotReportAnOlderVersionAsPending() {
        // The user just applied v22; this region's catalog replica still says v21.
        assertEquals(new PendingStatus.UpToDate(22), PendingRule.evaluate(Optional.of(state(2, 22, 0)), 21));
    }

    @Test
    void accumulatedUpdatesAreOneEntryWithADirectDiff() {
        var status = PendingRule.evaluate(Optional.of(state(1, 1, 0)), 4);

        var pending = assertInstanceOf(PendingStatus.Pending.class, status);
        assertEquals(3, pending.accumulatedVersions());
        assertEquals(new SummaryKey(TEMPLATE, 1, 4), pending.summaryKey());
        assertEquals(Optional.empty(), pending.newSinceDeclineKey());
    }

    @Test
    void declinedUpToTheLatestIsNotPending() {
        assertEquals(new PendingStatus.Declined(1, 4), PendingRule.evaluate(Optional.of(state(2, 1, 4)), 4));
    }

    @Test
    void newVersionAfterADeclineIsPendingAgainAndHighlightsWhatIsNew() {
        var status = PendingRule.evaluate(Optional.of(state(2, 1, 4)), 5);

        var pending = assertInstanceOf(PendingStatus.Pending.class, status);
        assertEquals(new SummaryKey(TEMPLATE, 1, 5), pending.summaryKey());
        assertEquals(Optional.of(new SummaryKey(TEMPLATE, 4, 5)), pending.newSinceDeclineKey());
    }

    @Test
    void declineBelowTheCurrentVersionNoLongerMatters() {
        // Declined v4, later applied v5; v6 is published.
        var pending = assertInstanceOf(PendingStatus.Pending.class,
                PendingRule.evaluate(Optional.of(state(3, 5, 4)), 6));

        assertEquals(new SummaryKey(TEMPLATE, 5, 6), pending.summaryKey());
        assertEquals(Optional.empty(), pending.newSinceDeclineKey());
    }

    @Test
    void archivedEngagementIsNeverPending() {
        var archived = new EngagementState(Fixtures.ENGAGEMENT,
                new EngagementSnapshot(TEMPLATE, 1, 0, true), 5, Fixtures.at(5));

        assertEquals(new PendingStatus.Archived(), PendingRule.evaluate(Optional.of(archived), 9));
    }
}
