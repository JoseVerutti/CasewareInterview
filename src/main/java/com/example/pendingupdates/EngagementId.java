package com.example.pendingupdates;

/**
 * Identifies one engagement inside one firm.
 *
 * <p>The firm is part of every key, matching the DynamoDB partition key {@code firmId} that the
 * IAM {@code dynamodb:LeadingKeys} condition restricts. No row can be addressed without it.
 */
public record EngagementId(String firmId, String engagementId) {

    public EngagementId {
        Checks.text(firmId, "firmId");
        Checks.text(engagementId, "engagementId");
    }
}
