package com.example.pendingupdates;

import java.util.Optional;

/**
 * Firm table of engagement states.
 *
 * <p>DynamoDB mapping: partition key {@code firmId}, sort key {@code engagementId}. {@link #putIfNewer}
 * is a {@code PutItem} with
 * {@code ConditionExpression: "attribute_not_exists(firmId) OR stateSeq < :seq"}; a
 * {@code ConditionalCheckFailedException} means {@code false}.
 */
public interface EngagementStateStore {

    Optional<EngagementState> find(EngagementId id);

    /**
     * Stores {@code state} only if there is no row yet or the stored {@code stateSeq} is lower.
     *
     * @return true if the row was written
     */
    boolean putIfNewer(EngagementState state);
}
