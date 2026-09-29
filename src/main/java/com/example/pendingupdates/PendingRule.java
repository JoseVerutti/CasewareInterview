package com.example.pendingupdates;

import java.util.Objects;
import java.util.Optional;

/**
 * The pending rule, evaluated on every list read: {@code latest > max(currentVersion, declinedUpTo)}.
 *
 * <p>It reads one small row and one number from the version catalog, so opening the list never
 * loads an engagement file, however many engagements the firm has.
 */
public final class PendingRule {

    private PendingRule() {
    }

    /**
     * @param state         the engagement's row, empty if it has not been seen since launch
     * @param latestVersion latest published version of the engagement's template (regional catalog)
     */
    public static PendingStatus evaluate(Optional<EngagementState> state, int latestVersion) {
        Objects.requireNonNull(state, "state");
        Checks.require(latestVersion >= 1, "latestVersion must be >= 1");

        if (state.isEmpty()) {
            return new PendingStatus.NotVerified();
        }
        EngagementSnapshot snapshot = state.get().snapshot();
        if (snapshot.archived()) {
            return new PendingStatus.Archived();
        }
        if (latestVersion <= snapshot.currentVersion()) {
            // Also covers a catalog replica that lags behind a just-applied update.
            return new PendingStatus.UpToDate(snapshot.currentVersion());
        }
        if (latestVersion <= snapshot.declinedUpTo()) {
            return new PendingStatus.Declined(snapshot.currentVersion(), snapshot.declinedUpTo());
        }
        return new PendingStatus.Pending(snapshot.templateId(), snapshot.currentVersion(), latestVersion,
                snapshot.declinedUpTo());
    }
}
