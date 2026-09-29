package com.example.pendingupdates;

import static com.example.pendingupdates.Fixtures.ENGAGEMENT;
import static com.example.pendingupdates.Fixtures.applied;
import static com.example.pendingupdates.Fixtures.created;
import static com.example.pendingupdates.Fixtures.declined;
import static com.example.pendingupdates.Fixtures.loaded;
import static com.example.pendingupdates.Fixtures.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProjectionHandlerTest {

    private final InMemoryEngagementStateStore store = new InMemoryEngagementStateStore();
    private final InMemoryDecisionLog decisions = new InMemoryDecisionLog();
    private final ProjectionHandler handler = new ProjectionHandler(store, decisions);

    @Test
    void lateDecisionReachesTheHistoryButNotTheState() {
        handler.handle(created(1, 1));
        handler.handle(loaded(3, 2, 0));
        var result = handler.handle(applied(2, 1, 2, 0));

        assertEquals(new ProjectionResult.Ignored(ProjectionResult.Reason.STALE), result);
        assertEquals(Optional.of(state(3, 2, 0)), store.find(ENGAGEMENT));
        assertEquals(1, decisions.history(ENGAGEMENT).size());
    }

    @Test
    void redeliveredDecisionIsLoggedOnce() {
        handler.handle(created(1, 1));
        handler.handle(declined(2, 1, 4, 4));
        handler.handle(declined(2, 1, 4, 4));

        assertEquals(1, decisions.history(ENGAGEMENT).size());
    }

    @Test
    void historyIsOrderedByStateSeqWhateverTheDeliveryOrder() {
        handler.handle(applied(3, 1, 5, 4));
        handler.handle(declined(2, 1, 4, 4));

        List<DecisionRecord> history = decisions.history(ENGAGEMENT);
        assertEquals(DecisionRecord.Decision.DECLINED, history.get(0).decision());
        assertEquals(DecisionRecord.Decision.APPLIED, history.get(1).decision());
    }

    @Test
    void conditionalWriteKeepsTheNewestState() {
        assertTrue(store.putIfNewer(state(5, 3, 0)));
        assertFalse(store.putIfNewer(state(4, 2, 0)));
        assertFalse(store.putIfNewer(state(5, 9, 0)));

        assertEquals(Optional.of(state(5, 3, 0)), store.find(ENGAGEMENT));
    }
}
