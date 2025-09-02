package deal.core;

import java.util.List;

/**
 * Prize ladders in **dollars** (integers). TODO: Replace LEGACY_10 and LEGACY_25 with your exact
 * legacy amounts (ascending).
 */
public final class DefaultPrizeLadder implements PrizeLadderProvider {

    // TODO(you): paste your exact legacy 10-case ladder here (dollars, ascending).
    private static final List<Integer> LEGACY_10 =
            List.of(1, 10, 50, 100, 500, 1_000, 5_000, 10_000, 25_000, 50_000);

    // TODO(you): paste your exact legacy 25-case ladder here (dollars, ascending).
    private static final List<Integer> LEGACY_25 =
            List.of(
                    1, 10, 25, 50, 75, 100, 200, 300, 400, 500, 750, 1_000, 2_500, 5_000, 7_500,
                    10_000, 15_000, 25_000, 50_000, 75_000, 100_000, 200_000, 300_000, 400_000,
                    500_000);

    @Override
    public List<Integer> amountsFor(int caseCount) {
        return switch (caseCount) {
            case 10 -> LEGACY_10;
            case 25 -> LEGACY_25;
            default -> throw new IllegalArgumentException("Unsupported case count: " + caseCount);
        };
    }
}
