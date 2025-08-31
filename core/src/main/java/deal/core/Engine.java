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
        List<Integer> shuffled = new ArrayList<>(cfg.amountsCents());
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
        if (s.toOpenInThisRound() <= 0) {
            throw new IllegalStateException("Nothing left to open this round");
        }
        requireValidCaseId(s, caseId);
        if (s.playerCaseId() != null && caseId == s.playerCaseId()) {
            throw new IllegalArgumentException("Cannot open the player's own case");
        }
        if (s.isOpened(caseId)) {
            throw new IllegalArgumentException("Case already opened: " + caseId);
        }

        var s1 = s.withOpened(caseId);
        return new GameState(
                s1.phase(),
                s1.roundIndex(),
                s1.cases(),
                s1.playerCaseId(),
                s1.openedCaseIds(),
                s1.currentOfferCents(),
                s1.toOpenInThisRound() - 1);
    }

    /** Compute banker offer based on remaining EV and a simple risk factor. */
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

    public GameState acceptDeal(GameState s) {
        requirePhase(s, Phase.OFFER);
        return new GameState(
                Phase.RESULT,
                s.roundIndex(),
                s.cases(),
                s.playerCaseId(),
                s.openedCaseIds(),
                s.currentOfferCents(),
                0);
    }

    public GameState declineDeal(GameState s) {
        requirePhase(s, Phase.OFFER);
        return s.nextRound(); // choose K again next loop
    }

    // ---- helpers ----

    private static void requirePhase(GameState s, Phase expected) {
        if (s.phase() != expected) {
            throw new IllegalStateException("Expected phase " + expected + " but was " + s.phase());
        }
    }

    private static void requireValidCaseId(GameState s, int caseId) {
        if (caseId < 1 || caseId > s.cases().size()) {
            throw new IllegalArgumentException("Invalid case id: " + caseId);
        }
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
        for (var c : s.cases()) if (!opened.contains(c.id())) out.add(c.amountCents());
        return out;
    }
}
