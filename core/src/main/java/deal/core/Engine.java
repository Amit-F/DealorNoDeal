package deal.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public final class Engine {
    private final GameConfig cfg;
    private final Random rng;

    public Engine(GameConfig cfg, long seed) {
        this.cfg = cfg;
        this.rng = new Random(seed);
    }

    public GameState start() {
        List<Integer> shuffled = new ArrayList<>(cfg.amountsDollars());
        Collections.shuffle(shuffled, rng);
        List<Briefcase> cases = new ArrayList<>(shuffled.size());
        for (int i = 0; i < shuffled.size(); i++) {
            cases.add(new Briefcase(i + 1, shuffled.get(i), false));
        }
        return GameState.initial(cases);
    }

    public GameState pickPlayerCase(GameState s, int caseId) {
        requirePhase(s, Phase.PICK_CASE);
        requireValidCaseId(s, caseId);
        return s.withPlayerCase(caseId);
    }

    /** Choose K to open in this round (validated by RoundPolicy). */
    public GameState chooseToOpen(GameState s, int k) {
        requirePhase(s, Phase.ROUND);
        int unopened = countUnopenedNonPlayer(s);
        if (!cfg.roundPolicy().isAllowed(unopened, k)) {
            throw new IllegalArgumentException(
                    "K not allowed for this round: " + k + " (unopened=" + unopened + ")");
        }
        return s.withRoundK(k);
    }

    /** Open a non-player unopened case; decreases remaining K. */
    public GameState openCase(GameState s, int caseId) {
        requirePhase(s, Phase.ROUND);
        if (s.toOpenInThisRound() <= 0)
            throw new IllegalStateException("Nothing left to open this round");
        requireValidCaseId(s, caseId);
        if (s.playerCaseId() != null && caseId == s.playerCaseId()) {
            throw new IllegalArgumentException("Cannot open the player's own case");
        }
        if (s.isOpened(caseId))
            throw new IllegalArgumentException("Case already opened: " + caseId);

        var s1 = s.withOpened(caseId);
        return new GameState(
                s1.phase(),
                s1.roundIndex(),
                s1.cases(),
                s1.playerCaseId(),
                s1.openedCaseIds(),
                s1.currentOfferDollars(),
                s1.counterOfferDollars(),
                s1.resultDollars(),
                s1.toOpenInThisRound() - 1);
    }

    /** Compute banker offer based on remaining EV and a simple risk factor (deterministic). */
    public GameState computeOffer(GameState s) {
        if (s.toOpenInThisRound() > 0) {
            throw new IllegalStateException(
                    "Still need to open " + s.toOpenInThisRound() + " case(s)");
        }
        var remaining = remainingAmounts(s);
        double ev = remaining.stream().mapToDouble(a -> a).average().orElse(0.0);
        double riskFactor = Math.max(0.5, 0.75 + 0.05 * s.roundIndex());
        int offer = (int) Math.round(ev * riskFactor);
        return s.withOffer(offer);
    }

    /** Player accepts banker offer. */
    public GameState acceptDeal(GameState s) {
        requirePhase(s, Phase.OFFER);
        return s.withResult(nonNull(s.currentOfferDollars(), "offer not set"));
    }

    /** Player declines banker offer; go to next round or final reveal if two cases remain. */
    public GameState declineDeal(GameState s) {
        requirePhase(s, Phase.OFFER);
        int remaining = s.cases().size() - s.openedCaseIds().size();
        if (remaining <= 2) return s.toFinalReveal();
        return s.nextRound();
    }

    /** Player proposes a counteroffer during OFFER; enters COUNTEROFFER phase. */
    public GameState proposeCounter(GameState s, int playerCounterDollars) {
        requirePhase(s, Phase.OFFER);
        if (playerCounterDollars <= 0)
            throw new IllegalArgumentException("Counter must be positive");
        int maxRemaining = remainingAmounts(s).stream().mapToInt(x -> x).max().orElse(0);
        if (playerCounterDollars > maxRemaining) {
            throw new IllegalArgumentException("Counter exceeds maximum possible prize");
        }
        return s.withCounterOffer(playerCounterDollars);
    }

    /**
     * Banker decides whether to accept the counter. Deterministic rule: accept if counter <= EV *
     * acceptanceFactor, where acceptanceFactor = min(1.10, 0.95 + 0.03 * roundIndex).
     */
    public GameState resolveCounter(GameState s) {
        requirePhase(s, Phase.COUNTEROFFER);
        int counter = nonNull(s.counterOfferDollars(), "counter not set");
        double ev = remainingAmounts(s).stream().mapToDouble(a -> a).average().orElse(0.0);
        double acceptanceFactor = Math.min(1.10, 0.95 + 0.03 * s.roundIndex());
        boolean accepted = counter <= ev * acceptanceFactor;
        if (accepted) {
            return s.withResult(counter);
        } else {
            int remaining = s.cases().size() - s.openedCaseIds().size();
            if (remaining <= 2) return s.toFinalReveal();
            return s.nextRound();
        }
    }

    /** Final reveal: if two cases remain, keep or swap; reveal winnings and end. */
    public GameState revealFinal(GameState s, boolean swap) {
        requirePhase(s, Phase.FINAL_REVEAL);
        int playerId = nonNull(s.playerCaseId(), "playerCase not chosen");
        var remainingIds = s.remainingUnopenedIds();
        if (!remainingIds.contains(playerId))
            throw new IllegalStateException("Player case was opened");
        if (remainingIds.size() != 2)
            throw new IllegalStateException("Final reveal requires exactly 2 unopened cases");

        int otherId = remainingIds.get(0) == playerId ? remainingIds.get(1) : remainingIds.get(0);
        int chosenId = swap ? otherId : playerId;
        int win = amountOf(s, chosenId);
        return s.withResult(win);
    }

    // ---- helpers ----

    private static void requirePhase(GameState s, Phase expected) {
        if (s.phase() != expected)
            throw new IllegalStateException("Expected phase " + expected + " but was " + s.phase());
    }

    private static void requireValidCaseId(GameState s, int caseId) {
        if (caseId < 1 || caseId > s.cases().size())
            throw new IllegalArgumentException("Invalid case id: " + caseId);
    }

    private static int countUnopenedNonPlayer(GameState s) {
        int total = 0;
        for (var c : s.cases()) {
            if (s.playerCaseId() != null && c.id() == s.playerCaseId()) continue;
            if (!s.isOpened(c.id())) total++;
        }
        return total;
    }

    private static List<Integer> remainingAmounts(GameState s) {
        var opened = new HashSet<>(s.openedCaseIds());
        List<Integer> out = new ArrayList<>();
        for (var c : s.cases()) if (!opened.contains(c.id())) out.add(c.amountDollars());
        return out;
    }

    private static int amountOf(GameState s, int caseId) {
        for (var c : s.cases()) if (c.id() == caseId) return c.amountDollars();
        throw new IllegalArgumentException("No such case id " + caseId);
    }

    private static <T> T nonNull(T v, String msg) {
        if (v == null) throw new IllegalStateException(msg);
        return v;
    }
}
