package deal.core.offer;

import java.util.List;

/** Strategy interface for computing banker offers, in dollars (integers). */
public interface OfferPolicy {

    /** Immutable input for offer computation. */
    public static final class Context {
        /** The number of cases at game start (e.g., 10 or 25). */
        public final int initialCaseCount;

        /** Remaining unopened amounts, in dollars (unsorted ok). Must be non-empty. */
        public final List<Integer> remainingAmounts;

        /** How many cases have been opened so far (monotone, can be 0). */
        public final int openedSoFar;

        /** The most recent banker offer, or null if none yet. */
        public final Integer lastOffer;

        public Context(
                int initialCaseCount,
                List<Integer> remainingAmounts,
                int openedSoFar,
                Integer lastOffer) {
            if (initialCaseCount < 2 || initialCaseCount > 25) {
                throw new IllegalArgumentException(
                        "initialCaseCount must be in [2..25], got " + initialCaseCount);
            }
            if (remainingAmounts == null || remainingAmounts.isEmpty()) {
                throw new IllegalArgumentException("remainingAmounts must be non-empty");
            }
            this.initialCaseCount = initialCaseCount;
            this.remainingAmounts = List.copyOf(remainingAmounts);
            this.openedSoFar = Math.max(0, openedSoFar);
            this.lastOffer = lastOffer;
        }
    }

    /** Compute the banker offer for the given context (whole dollars). */
    int offer(Context ctx);
}
