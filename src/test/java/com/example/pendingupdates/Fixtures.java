package com.example.pendingupdates;

import java.time.Instant;

/** Builders for EMS events on one engagement of template {@code audit-ca}. */
final class Fixtures {

    static final EngagementId ENGAGEMENT = new EngagementId("firm-a", "eng-1");
    static final String TEMPLATE = "audit-ca";
    static final String USER = "user-1";

    private static final Instant EPOCH = Instant.parse("2026-01-01T00:00:00Z");

    private Fixtures() {
    }

    static Instant at(long stateSeq) {
        return EPOCH.plusSeconds(stateSeq);
    }

    static EngagementSnapshot snapshot(int currentVersion, int declinedUpTo) {
        return new EngagementSnapshot(TEMPLATE, currentVersion, declinedUpTo, false);
    }

    static EngagementEvent.Created created(long seq, int version) {
        return new EngagementEvent.Created(ENGAGEMENT, seq, at(seq), snapshot(version, 0));
    }

    static EngagementEvent.Loaded loaded(long seq, int version, int declinedUpTo) {
        return new EngagementEvent.Loaded(ENGAGEMENT, seq, at(seq), snapshot(version, declinedUpTo));
    }

    static EngagementEvent.UpdateApplied applied(long seq, int from, int to, int declinedUpTo) {
        return new EngagementEvent.UpdateApplied(ENGAGEMENT, seq, at(seq), snapshot(to, declinedUpTo), from, USER);
    }

    /** {@code declinedUpTo} is the value after the decline, so it is at least {@code declinedVersion}. */
    static EngagementEvent.UpdateDeclined declined(long seq, int current, int declinedVersion, int declinedUpTo) {
        return new EngagementEvent.UpdateDeclined(ENGAGEMENT, seq, at(seq), snapshot(current, declinedUpTo),
                declinedVersion, USER);
    }

    static EngagementEvent.Archived archived(long seq, int current, int declinedUpTo) {
        return new EngagementEvent.Archived(ENGAGEMENT, seq, at(seq),
                new EngagementSnapshot(TEMPLATE, current, declinedUpTo, true));
    }

    static EngagementState state(long seq, int current, int declinedUpTo) {
        return new EngagementState(ENGAGEMENT, snapshot(current, declinedUpTo), seq, at(seq));
    }
}
