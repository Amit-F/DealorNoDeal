package deal.core;

import java.util.List;

/**
 * Prize ladders in **dollars** (integers). TODO: Replace LEGACY_10 and LEGACY_25 with your exact
 * legacy amounts (ascending).
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
        return switch (caseCount) {
            case 10 -> LEGACY_10;
            case 25 -> LEGACY_25;
            default -> throw new IllegalArgumentException("Unsupported case count: " + caseCount);
        };
    }
}
