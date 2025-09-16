package deal.core;

import deal.core.offer.LegacyOfferPolicy;
import deal.core.offer.OfferPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/** Core game engine: state transitions + banker offer via pluggable policy. */
public final class Engine {
    private final GameConfig cfg;
    private final Random rng;

    public Engine(GameConfig cfg, long seed) {
        this.cfg = cfg;
        this.rng = new Random(seed);
    }

    /** Start a new game: shuffle amounts, build cases, enter PICK_CASE phase. */
    public GameState start() {
        // Resolve the ladder provider from GameConfig (support multiple APIs + reflection).
        PrizeLadderProvider provider = resolveLadderProvider(cfg);
        List<Integer> ladder = provider.amountsFor(cfg.caseCount());
        List<Integer> shuffled = new ArrayList<>(ladder);
        Collections.shuffle(shuffled, rng);

        List<Briefcase> cases = new ArrayList<>(shuffled.size());
        for (int i = 0; i < shuffled.size(); i++) {
            // Briefcase record signature: (id, amountDollars, opened)
            cases.add(new Briefcase(i + 1, shuffled.get(i), false));
        }

        return new GameState(
                Phase.PICK_CASE,
                1,
                cases,
                null, // playerCaseId
                new ArrayList<>(), // openedCaseIds
                null, // currentOfferDollars
                null, // counterOfferDollars
                null, // resultDollars
                0 // toOpenInThisRound
                );
    }

    /** Player picks their personal case during PICK_CASE. */
    public GameState pickPlayerCase(GameState s, int caseId) {
        requirePhase(s, Phase.PICK_CASE);
        requireValidCaseId(s, caseId);
        return s.withPlayerCase(caseId);
    }

    /**
     * Player chooses how many cases to open this round. Validates K against remaining unopened
     * **excluding** the player's case. Transitions to ROUND (stores K countdown).
     */
    public GameState chooseToOpen(GameState s, int k) {
        if (s.phase() != Phase.ROUND && s.phase() != Phase.PICK_CASE) {
            throw new IllegalStateException("chooseToOpen only allowed at round start");
        }
        int available = countUnopenedNonPlayer(s);
        if (k <= 0 || k > Math.max(1, available - 1)) {
            throw new IllegalArgumentException("Invalid K for this round: " + k);
        }
        if (s.phase() == Phase.PICK_CASE) {
            if (s.playerCaseId() == null) throw new IllegalStateException("Pick your case first");
            return s.withRoundK(k);
        }
        return s.withRoundK(k);
    }

    /** Open a specific unopened case (not the player's); decrements K. */
    public GameState openCase(GameState s, int caseId) {
        requirePhase(s, Phase.ROUND);
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

    /** Compute banker offer using the pluggable OfferPolicy (legacy-flavored curve). */
    public GameState computeOffer(GameState s) {
        if (s.toOpenInThisRound() > 0) {
            throw new IllegalStateException(
                    "Still need to open " + s.toOpenInThisRound() + " case(s)");
        }
        var remaining = remainingAmounts(s);
        OfferPolicy.Context ctx =
                new OfferPolicy.Context(
                        s.cases().size(), // initial case count
                        remaining, // unopened amounts
                        s.openedCaseIds().size(), // opened so far
                        s.currentOfferDollars() // last offer (may be null)
                        );
        int offer = LegacyOfferPolicy.DEFAULT.offer(ctx);
        return s.withOffer(offer);
    }

    /** Convenience: entering OFFER just delegates to computeOffer(). */
    public GameState toOffer(GameState s) {
        return computeOffer(s);
    }

    /** Player accepts banker offer. */
    public GameState acceptDeal(GameState s) {
        requirePhase(s, Phase.OFFER);
        return s.withResult(nonNull(s.currentOfferDollars(), "offer not set"));
    }

    /** Player declines banker offer; next round or final reveal if two cases remain. */
    public GameState declineDeal(GameState s) {
        requirePhase(s, Phase.OFFER);
        int remaining = s.cases().size() - s.openedCaseIds().size();
        if (remaining <= 2) return s.toFinalReveal();
        return s.nextRound();
    }

    /**
     * Player proposes a counteroffer during OFFER. Legacy parity: allow any positive number
     * (feasibility/acceptance checked in resolveCounter()).
     */
    public GameState proposeCounter(GameState s, int playerCounterDollars) {
        requirePhase(s, Phase.OFFER);
        if (playerCounterDollars <= 0) {
            throw new IllegalArgumentException("Counter must be positive");
        }
        return s.withCounterOffer(playerCounterDollars);
    }

    /**
     * Banker decision (legacy rule + pragmatic "same-or-less than offer" acceptance): Accept if
     * EITHER: (A) counter ≤ current offer (effectively a DEAL), OR (B) counter ≤ maxRemaining AND
     * counter ≤ ceil(EV * acceptanceFactor), where acceptanceFactor = min(1.10, 0.95 + 0.03 *
     * roundIndex).
     */
    public GameState resolveCounter(GameState s) {
        requirePhase(s, Phase.COUNTEROFFER);
        int counter = nonNull(s.counterOfferDollars(), "counter not set");

        // If player counters at/below the current offer, accept immediately.
        Integer curOffer = s.currentOfferDollars();
        if (curOffer != null && counter <= curOffer) {
            return s.withResult(counter);
        }

        // Legacy feasibility + EV threshold rule.
        List<Integer> rem = remainingAmounts(s);
        int maxRemaining = rem.stream().mapToInt(x -> x).max().orElse(0);
        double ev = rem.stream().mapToDouble(a -> a).average().orElse(0.0);
        double acceptanceFactor = Math.min(1.10, 0.95 + 0.03 * s.roundIndex());
        int threshold = (int) Math.ceil(ev * acceptanceFactor);

        boolean feasible = counter <= maxRemaining;
        boolean reasonable = counter <= threshold;
        boolean accepted = feasible && reasonable;

        if (accepted) {
            return s.withResult(counter);
        } else {
            int remaining = s.cases().size() - s.openedCaseIds().size();
            if (remaining <= 2) return s.toFinalReveal();
            return s.nextRound();
        }
    }

    /**
     * Final reveal: exactly two unopened cases total (player + one other). Optional swap switches
     * to the other case; result is the chosen case's amount.
     */
    public GameState revealFinal(GameState s, boolean swap) {
        requirePhase(s, Phase.FINAL_REVEAL);
        int playerId = nonNull(s.playerCaseId(), "player case not set");

        // Collect all unopened case ids (includes player's case).
        List<Integer> unopened = new ArrayList<>();
        for (var c : s.cases()) {
            if (!s.isOpened(c.id())) unopened.add(c.id());
        }
        if (unopened.size() != 2) {
            throw new IllegalStateException("Final reveal requires exactly 2 unopened cases");
        }

        // Identify the non-player case id.
        int otherId = (unopened.get(0) == playerId) ? unopened.get(1) : unopened.get(0);
        int chosenId = swap ? otherId : playerId;
        int win = amountOf(s, chosenId);
        return s.withResult(win);
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

    /** Resolve a PrizeLadderProvider from GameConfig without assuming field/method names. */
    private static PrizeLadderProvider resolveLadderProvider(GameConfig cfg) {
        // 1) Try common explicit accessor names.
        for (String name :
                new String[] {
                    "ladder", "prizeLadderProvider", "prizeLadder",
                    "getLadder", "getPrizeLadderProvider", "getPrizeLadder"
                }) {
            try {
                Method m = cfg.getClass().getMethod(name);
                if (PrizeLadderProvider.class.isAssignableFrom(m.getReturnType())) {
                    Object v = m.invoke(cfg);
                    if (v instanceof PrizeLadderProvider p) return p;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        // 2) Try any zero-arg method that returns PrizeLadderProvider.
        for (Method m : cfg.getClass().getMethods()) {
            try {
                if (m.getParameterCount() == 0
                        && PrizeLadderProvider.class.isAssignableFrom(m.getReturnType())) {
                    Object v = m.invoke(cfg);
                    if (v instanceof PrizeLadderProvider p) return p;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        // 3) Try fields.
        for (Field f : cfg.getClass().getDeclaredFields()) {
            try {
                if (PrizeLadderProvider.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object v = f.get(cfg);
                    if (v instanceof PrizeLadderProvider p) return p;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        // 4) Fallback.
        return new DefaultPrizeLadder();
    }
}
