package deal.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class GameState {
    private final Phase phase;
    private final int roundIndex;
    private final List<Briefcase> cases;
    private final Integer playerCaseId; // null until chosen
    private final List<Integer> openedCaseIds;
    private final Integer currentOfferCents; // null until set
    private final Integer counterOfferCents; // null until set (Stage 5)
    private final Integer resultCents; // null until RESULT
    private final int toOpenInThisRound; // remaining K to open this round

    public GameState(
            Phase phase,
            int roundIndex,
            List<Briefcase> cases,
            Integer playerCaseId,
            List<Integer> openedCaseIds,
            Integer currentOfferCents,
            Integer counterOfferCents,
            Integer resultCents,
            int toOpenInThisRound) {
        this.phase = Objects.requireNonNull(phase);
        this.roundIndex = roundIndex;
        this.cases = List.copyOf(cases);
        this.playerCaseId = playerCaseId;
        this.openedCaseIds = List.copyOf(openedCaseIds);
        this.currentOfferCents = currentOfferCents;
        this.counterOfferCents = counterOfferCents;
        this.resultCents = resultCents;
        this.toOpenInThisRound = toOpenInThisRound;
    }

    static GameState initial(List<Briefcase> shuffled) {
        return new GameState(Phase.PICK_CASE, 0, shuffled, null, List.of(), null, null, null, 0);
    }

    public Phase phase() {
        return phase;
    }

    public int roundIndex() {
        return roundIndex;
    }

    public List<Briefcase> cases() {
        return cases;
    }

    public Integer playerCaseId() {
        return playerCaseId;
    }

    public List<Integer> openedCaseIds() {
        return openedCaseIds;
    }

    public Integer currentOfferCents() {
        return currentOfferCents;
    }

    public Integer counterOfferCents() {
        return counterOfferCents;
    }

    public Integer resultCents() {
        return resultCents;
    }

    public int toOpenInThisRound() {
        return toOpenInThisRound;
    }

    /** Convenience: unopened cases (including player's own if not opened). */
    public List<Briefcase> unopened() {
        var opened = new HashSet<>(openedCaseIds);
        return cases.stream().filter(c -> !opened.contains(c.id())).collect(Collectors.toList());
    }

    /** Convenience: just ids of unopened cases. */
    public List<Integer> remainingUnopenedIds() {
        var opened = new HashSet<>(openedCaseIds);
        List<Integer> out = new ArrayList<>();
        for (var c : cases) if (!opened.contains(c.id())) out.add(c.id());
        return out;
    }

    public boolean isOpened(int id) {
        return openedCaseIds.contains(id);
    }

    public GameState withOffer(int cents) {
        return new GameState(
                Phase.OFFER, roundIndex, cases, playerCaseId, openedCaseIds, cents, null, null, 0);
    }

    public GameState withCounterOffer(int cents) {
        return new GameState(
                Phase.COUNTEROFFER,
                roundIndex,
                cases,
                playerCaseId,
                openedCaseIds,
                currentOfferCents,
                cents,
                null,
                0);
    }

    public GameState withRoundK(int k) {
        return new GameState(
                Phase.ROUND, roundIndex, cases, playerCaseId, openedCaseIds, null, null, null, k);
    }

    public GameState nextRound() {
        return new GameState(
                Phase.ROUND,
                roundIndex + 1,
                cases,
                playerCaseId,
                openedCaseIds,
                null,
                null,
                null,
                0);
    }

    public GameState withOpened(int caseId) {
        var newOpened = new ArrayList<>(openedCaseIds);
        newOpened.add(caseId);
        var newCases = new ArrayList<Briefcase>(cases.size());
        for (var c : cases)
            newCases.add(c.id() == caseId ? new Briefcase(c.id(), c.amountCents(), true) : c);
        return new GameState(
                phase,
                roundIndex,
                List.copyOf(newCases),
                playerCaseId,
                List.copyOf(newOpened),
                currentOfferCents,
                counterOfferCents,
                resultCents,
                toOpenInThisRound);
    }

    public GameState withPlayerCase(int id) {
        return new GameState(
                Phase.ROUND, roundIndex, cases, id, openedCaseIds, null, null, null, 0);
    }

    public GameState toFinalReveal() {
        return new GameState(
                Phase.FINAL_REVEAL,
                roundIndex,
                cases,
                playerCaseId,
                openedCaseIds,
                null,
                null,
                null,
                0);
    }

    public GameState withResult(int cents) {
        return new GameState(
                Phase.RESULT,
                roundIndex,
                cases,
                playerCaseId,
                openedCaseIds,
                currentOfferCents,
                counterOfferCents,
                cents,
                0);
    }
}
