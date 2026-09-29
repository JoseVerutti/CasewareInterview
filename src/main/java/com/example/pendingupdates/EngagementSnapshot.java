package com.example.pendingupdates;

/**
 * What this feature needs to know about an engagement, as the EMS knows it right after a change.
 *
 * <p>Every event carries a full snapshot rather than a delta. Combined with the {@code stateSeq}
 * guard this makes the projection "newest event wins": whatever order events arrive in, the final
 * state is the snapshot of the event with the highest sequence number. With deltas, a late "loaded"
 * event arriving before an earlier "declined" event would silently drop the decline.
 *
 * @param templateId     product template the engagement was created from; never changes
 * @param currentVersion template version whose content the engagement currently holds
 * @param declinedUpTo   highest version the user explicitly declined, or {@link #NONE_DECLINED}
 * @param archived       closed or archived engagements receive no updates
 */
public record EngagementSnapshot(String templateId, int currentVersion, int declinedUpTo, boolean archived) {

    public static final int NONE_DECLINED = 0;

    public EngagementSnapshot {
        Checks.text(templateId, "templateId");
        Checks.require(!templateId.contains("#"), "templateId must not contain '#'");
        Checks.require(currentVersion >= 1, "currentVersion must be >= 1");
        Checks.require(declinedUpTo >= NONE_DECLINED, "declinedUpTo must be >= 0");
    }

    /** Highest version the user has either applied or explicitly declined. */
    public int baseline() {
        return Math.max(currentVersion, declinedUpTo);
    }
}
