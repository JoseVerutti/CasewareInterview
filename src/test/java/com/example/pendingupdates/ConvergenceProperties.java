package com.example.pendingupdates;

import static com.example.pendingupdates.Fixtures.ENGAGEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

/**
 * Why standard SQS is enough: whatever the delivery order and however many duplicates, the projection
 * ends in the same state and the decision log holds every decision exactly once.
 */
class ConvergenceProperties {

    @Property(tries = 1000)
    void anyDeliveryOrderWithDuplicatesEndsInTheNewestState(@ForAll long seed) {
        Random random = new Random(seed);
        List<EngagementEvent> history = HistoryGenerator.history(random);
        List<EngagementEvent> delivery = HistoryGenerator.shuffledWithDuplicates(random, history);

        Replay inOrder = Replay.of(history);
        Replay outOfOrder = Replay.of(delivery);

        EngagementState newest = EngagementState.from(history.get(history.size() - 1));
        assertEquals(Optional.of(newest), outOfOrder.state());
        assertEquals(inOrder.state(), outOfOrder.state());

        int latest = random.nextInt(1, 20);
        assertEquals(PendingRule.evaluate(inOrder.state(), latest), PendingRule.evaluate(outOfOrder.state(), latest));
    }

    @Property(tries = 1000)
    void everyDecisionIsLoggedExactlyOnceInStateSeqOrder(@ForAll long seed) {
        Random random = new Random(seed);
        List<EngagementEvent> history = HistoryGenerator.history(random);
        List<EngagementEvent> delivery = HistoryGenerator.shuffledWithDuplicates(random, history);

        List<DecisionRecord> expected = history.stream()
                .map(DecisionRecord::of)
                .flatMap(Optional::stream)
                .toList();

        assertEquals(expected, Replay.of(delivery).decisions());
    }

    private record Replay(Optional<EngagementState> state, List<DecisionRecord> decisions) {

        static Replay of(List<EngagementEvent> delivery) {
            var store = new InMemoryEngagementStateStore();
            var log = new InMemoryDecisionLog();
            var handler = new ProjectionHandler(store, log);
            delivery.forEach(handler::handle);
            return new Replay(store.find(ENGAGEMENT), log.history(ENGAGEMENT));
        }
    }
}
