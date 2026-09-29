package com.example.pendingupdates;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

/** Thread-safe in-memory decision log keyed by engagement and stateSeq. */
public final class InMemoryDecisionLog implements DecisionLog {

    private final ConcurrentMap<EngagementId, ConcurrentSkipListMap<Long, DecisionRecord>> byEngagement =
            new ConcurrentHashMap<>();

    @Override
    public boolean appendIfAbsent(DecisionRecord record) {
        return byEngagement
                .computeIfAbsent(record.id(), id -> new ConcurrentSkipListMap<>())
                .putIfAbsent(record.stateSeq(), record) == null;
    }

    @Override
    public List<DecisionRecord> history(EngagementId id) {
        ConcurrentSkipListMap<Long, DecisionRecord> records = byEngagement.get(id);
        return records == null ? List.of() : List.copyOf(records.values());
    }
}
