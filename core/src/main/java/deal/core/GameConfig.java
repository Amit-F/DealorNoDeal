package deal.core;

import java.util.List;
import java.util.Objects;

/** Player picks total cases; amounts are derived (matches legacy rule). */
public final class GameConfig {
    private final int caseCount;
    private final PrizeLadderProvider ladder;
    private final RoundPolicy roundPolicy;

    public GameConfig(int caseCount, PrizeLadderProvider ladder, RoundPolicy roundPolicy) {
        if (caseCount < 2) throw new IllegalArgumentException("caseCount must be >= 2");
        this.caseCount = caseCount;
        this.ladder = Objects.requireNonNull(ladder, "ladder");
        this.roundPolicy = Objects.requireNonNull(roundPolicy, "roundPolicy");
        var amounts = ladder.amountsFor(caseCount);
        if (amounts.size() != caseCount)
            throw new IllegalArgumentException(
                    "ladder size (" + amounts.size() + ") != caseCount " + caseCount);
    }

    public int caseCount() {
        return caseCount;
    }

    public List<Integer> amountsCents() {
        return List.copyOf(ladder.amountsFor(caseCount));
    }

    public RoundPolicy roundPolicy() {
        return roundPolicy;
    }

    public static GameConfig of(int caseCount) {
        return new GameConfig(caseCount, new DefaultPrizeLadder(), new CustomPerRoundPolicy());
    }
}
