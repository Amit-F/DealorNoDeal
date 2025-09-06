package deal.core;

import java.util.List;

/**
 * Prize ladders in dollars (integers). Supports classic 10 & 25 case games and custom case counts
 * from 2..25 by taking the lowest N amounts from the 25-case ladder. Example: N=8 -> highest $200;
 * N=11 -> highest $500.
 */
public final class DefaultPrizeLadder implements PrizeLadderProvider {

    private static final List<Integer> LEGACY_10 =
            List.of(1, 5, 10, 25, 50, 75, 100, 200, 300, 400);

    private static final List<Integer> LEGACY_25 =
            List.of(
                    1, 5, 10, 25, 50, 75, 100, 200, 300, 400, 500, 750, 1_000, 5_000, 10_000,
                    25_000, 50_000, 75_000, 100_000, 200_000, 300_000, 400_000, 500_000, 750_000,
                    1_000_000);

    @Override
    public List<Integer> amountsFor(int caseCount) {
        if (caseCount < 2 || caseCount > 25) {
            throw new IllegalArgumentException(
                    "Unsupported case count: " + caseCount + " (allowed 2..25)");
        }
        if (caseCount == 25) return LEGACY_25;
        if (caseCount == 10) return LEGACY_10; // explicit parity for the classic 10-case game
        // For any other N in 2..24, use the first N amounts from the 25-case ladder.
        return List.copyOf(LEGACY_25.subList(0, caseCount));
    }
}
