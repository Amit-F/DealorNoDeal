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
    private final int toOpenInThisRound; // remaining K to open this round

    public GameState(
            Phase phase,
            int roundIndex,
            List<Briefcase> cases,
            Integer playerCaseId,
            List<Integer> openedCaseIds,
            Integer currentOfferCents,
            int toOpenInThisRound) {
        this.phase = Objects.requireNonNull(phase);
        this.roundIndex = roundIndex;
        this.cases = List.copyOf(cases);
        this.playerCaseId = playerCaseId;
        this.openedCaseIds = List.copyOf(openedCaseIds);
        this.currentOfferCents = currentOfferCents;
        this.toOpenInThisRound = toOpenInThisRound;
    }

    static GameState initial(List<Briefcase> shuffled) {
        return new GameState(Phase.PICK_CASE, 0, shuffled, null, List.of(), null, 0);
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

    public int toOpenInThisRound() {
        return toOpenInThisRound;
    }

    /** Convenience: unopened cases (including player's own if not opened). */
    public List<Briefcase> unopened() {
        var opened = new HashSet<>(openedCaseIds);
        return cases.stream().filter(c -> !opened.contains(c.id())).collect(Collectors.toList());
    }

    public boolean isOpened(int id) {
        return openedCaseIds.contains(id);
    }

    public GameState withOffer(int cents) {
        return new GameState(Phase.OFFER, roundIndex, cases, playerCaseId, openedCaseIds, cents, 0);
    }

    public GameState withRoundK(int k) {
        return new GameState(Phase.ROUND, roundIndex, cases, playerCaseId, openedCaseIds, null, k);
    }

    public GameState nextRound() {
        return new GameState(
                Phase.ROUND, roundIndex + 1, cases, playerCaseId, openedCaseIds, null, 0);
    }

    public GameState withOpened(int caseId) {
        var newOpened = new ArrayList<>(openedCaseIds);
        newOpened.add(caseId);
        var newCases = new ArrayList<Briefcase>(cases.size());
        for (var c : cases) {
            newCases.add(c.id() == caseId ? new Briefcase(c.id(), c.amountCents(), true) : c);
        }
        return new GameState(
                phase,
                roundIndex,
                List.copyOf(newCases),
                playerCaseId,
                List.copyOf(newOpened),
                currentOfferCents,
                toOpenInThisRound);
    }

    public GameState withPlayerCase(int id) {
        return new GameState(Phase.ROUND, roundIndex, cases, id, openedCaseIds, null, 0);
    }
}
