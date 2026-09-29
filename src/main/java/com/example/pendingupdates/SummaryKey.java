package com.example.pendingupdates;

/**
 * Identifies one change summary in the shared summary cache.
 *
 * <p>It depends only on the template and the two versions, never on the firm, which is why one
 * generated summary can be served to every firm with an engagement on {@code fromVersion}.
 */
public record SummaryKey(String templateId, int fromVersion, int toVersion) {

    public SummaryKey {
        Checks.text(templateId, "templateId");
        Checks.require(!templateId.contains("#"), "templateId must not contain '#'");
        Checks.require(fromVersion >= 1 && toVersion > fromVersion, "a summary covers fromVersion < toVersion");
    }

    /** Partition key of the summary cache, e.g. {@code audit-ca#12#21}. */
    public String cacheKey() {
        return templateId + "#" + fromVersion + "#" + toVersion;
    }
}
