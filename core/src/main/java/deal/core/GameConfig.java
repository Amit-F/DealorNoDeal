package deal.core;

import java.util.List;

public final class GameConfig {
    private final int caseCount;
    private final PrizeLadderProvider ladder;
    private final RoundPolicy policy;

    public GameConfig(int caseCount, PrizeLadderProvider ladder, RoundPolicy policy) {
        this.caseCount = caseCount;
        this.ladder = ladder;
        this.policy = policy;
        if (caseCount < 2) throw new IllegalArgumentException("Need at least 2 cases");
        if (ladder.amountsFor(caseCount).size() != caseCount) {
            throw new IllegalArgumentException("Ladder size != caseCount");
        }
    }

    public static GameConfig of(int caseCount) {
        return new GameConfig(caseCount, new DefaultPrizeLadder(), new CustomPerRoundPolicy());
    }

    public int caseCount() {
        return caseCount;
    }

    public RoundPolicy roundPolicy() {
        return policy;
    }

    /** Prize amounts in dollars. */
    public List<Integer> amountsDollars() {
        return ladder.amountsFor(caseCount);
    }
}
