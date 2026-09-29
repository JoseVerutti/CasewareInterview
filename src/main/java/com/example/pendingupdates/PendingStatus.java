package com.example.pendingupdates;

import java.util.Optional;

/** What the engagement list shows for one engagement. */
public sealed interface PendingStatus {

    /** No version recorded yet. Never shown as up to date. */
    record NotVerified() implements PendingStatus {
    }

    /** Closed or archived: receives no updates. */
    record Archived() implements PendingStatus {
    }

    record UpToDate(int currentVersion) implements PendingStatus {
    }

    /** Every version up to the latest was declined; nothing new to decide. */
    record Declined(int currentVersion, int declinedUpTo) implements PendingStatus {
    }

    /**
     * One entry, however many versions accumulated since {@code currentVersion}.
     *
     * @param declinedUpTo last declined version, or {@link EngagementSnapshot#NONE_DECLINED}
     */
    record Pending(String templateId, int currentVersion, int latestVersion, int declinedUpTo)
            implements PendingStatus {

        /** The summary shown to the user: the direct diff of what applying would change. */
        public SummaryKey summaryKey() {
            return new SummaryKey(templateId, currentVersion, latestVersion);
        }

        /** The part to highlight as "new since you declined", if the user declined before. */
        public Optional<SummaryKey> newSinceDeclineKey() {
            return declinedUpTo > currentVersion
                    ? Optional.of(new SummaryKey(templateId, declinedUpTo, latestVersion))
                    : Optional.empty();
        }

        /** Number of published versions the user has not seen, e.g. 3 for v1 to v4. */
        public int accumulatedVersions() {
            return latestVersion - currentVersion;
        }
    }
}
