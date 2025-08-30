package deal.core;

import java.util.List;

public final class DefaultPrizeLadder implements PrizeLadderProvider {
    @Override
    public List<Integer> amountsFor(int caseCount) {
        // TODO: replace with your exact legacy ladders
        return switch (caseCount) {
            case 10 -> List.of(1, 10, 50, 100, 500, 1_000, 5_000, 10_000, 25_000, 50_000);
            case 25 ->
                    List.of(
                            1, 10, 25, 50, 75, 100, 200, 300, 400, 500, 750, 1_000, 2_500, 5_000,
                            7_500, 10_000, 15_000, 25_000, 50_000, 75_000, 100_000, 200_000,
                            300_000, 400_000, 500_000);
            default -> throw new IllegalArgumentException("Unsupported case count: " + caseCount);
        };
    }
}
