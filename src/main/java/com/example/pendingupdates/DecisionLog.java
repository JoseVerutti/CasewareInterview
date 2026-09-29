package com.example.pendingupdates;

import java.util.List;

/**
 * Append-only history of apply and decline decisions.
 *
 * <p>DynamoDB mapping: partition key {@code firmId}, sort key {@code engagementId#stateSeq}, written with
 * {@code attribute_not_exists}; update and delete are denied by IAM.
 */
public interface DecisionLog {

    /**
     * Appends {@code record} unless a record with the same engagement and {@code stateSeq} exists.
     *
     * @return true if newly appended
     */
    boolean appendIfAbsent(DecisionRecord record);

    /** Decisions for one engagement, oldest first. */
    List<DecisionRecord> history(EngagementId id);
}
