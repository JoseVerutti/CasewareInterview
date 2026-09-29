package com.example.pendingupdates;

/** Outcome of applying one event to the stored state. */
public sealed interface ProjectionResult {

    /** The event is the newest seen for its engagement; this is the state to store. */
    record Updated(EngagementState state) implements ProjectionResult {
    }

    /** The event changes nothing: a redelivery, or older than what is stored. */
    record Ignored(Reason reason) implements ProjectionResult {
    }

    enum Reason {
        DUPLICATE,
        STALE
    }
}
