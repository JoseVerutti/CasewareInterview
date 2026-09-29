package com.example.pendingupdates;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Thread-safe in-memory store with the same conditional-write semantics as the DynamoDB table. */
public final class InMemoryEngagementStateStore implements EngagementStateStore {

    private final ConcurrentMap<EngagementId, EngagementState> rows = new ConcurrentHashMap<>();

    @Override
    public Optional<EngagementState> find(EngagementId id) {
        return Optional.ofNullable(rows.get(id));
    }

    @Override
    public boolean putIfNewer(EngagementState state) {
        AtomicBoolean written = new AtomicBoolean(false);
        rows.compute(state.id(), (id, existing) -> {
            if (existing == null || existing.stateSeq() < state.stateSeq()) {
                written.set(true);
                return state;
            }
            return existing;
        });
        return written.get();
    }
}
