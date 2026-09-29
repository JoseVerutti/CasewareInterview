package com.example.pendingupdates;

import static com.example.pendingupdates.Fixtures.applied;
import static com.example.pendingupdates.Fixtures.archived;
import static com.example.pendingupdates.Fixtures.created;
import static com.example.pendingupdates.Fixtures.declined;
import static com.example.pendingupdates.Fixtures.loaded;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Simulates the EMS producing a valid history for one engagement, and SQS delivering it. */
final class HistoryGenerator {

    private HistoryGenerator() {
    }

    /** A valid history in stateSeq order: created, then opens, applies and declines, maybe archived. */
    static List<EngagementEvent> history(Random random) {
        long seq = 1;
        int current = random.nextInt(1, 4);
        int declinedUpTo = 0;
        List<EngagementEvent> events = new ArrayList<>();
        events.add(created(seq, current));

        int steps = random.nextInt(0, 12);
        for (int i = 0; i < steps; i++) {
            seq++;
            switch (random.nextInt(3)) {
                case 0 -> events.add(loaded(seq, current, declinedUpTo));
                case 1 -> {
                    int to = current + random.nextInt(1, 4);
                    events.add(applied(seq, current, to, declinedUpTo));
                    current = to;
                }
                default -> {
                    int declinedVersion = current + random.nextInt(1, 4);
                    declinedUpTo = Math.max(declinedUpTo, declinedVersion);
                    events.add(declined(seq, current, declinedVersion, declinedUpTo));
                }
            }
        }
        if (random.nextInt(5) == 0) {
            events.add(archived(seq + 1, current, declinedUpTo));
        }
        return events;
    }

    /** At-least-once, unordered delivery: every event at least once, some twice, shuffled. */
    static List<EngagementEvent> shuffledWithDuplicates(Random random, List<EngagementEvent> history) {
        List<EngagementEvent> delivery = new ArrayList<>(history);
        int duplicates = random.nextInt(0, history.size() + 1);
        for (int i = 0; i < duplicates; i++) {
            delivery.add(history.get(random.nextInt(history.size())));
        }
        Collections.shuffle(delivery, random);
        return delivery;
    }
}
