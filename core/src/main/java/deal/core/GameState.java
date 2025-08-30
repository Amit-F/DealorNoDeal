package deal.core;
import java.util.ArrayList; import java.util.Collections; import java.util.List; import java.util.Objects;

public final class GameState {
  private final Phase phase;
  private final int roundIndex;
  private final List<Briefcase> cases;
  private final Integer playerCaseId;
  private final List<Integer> openedCaseIds;
  private final Integer currentOfferCents;

  public GameState(Phase phase, int roundIndex, List<Briefcase> cases,
                   Integer playerCaseId, List<Integer> openedCaseIds,
                   Integer currentOfferCents) {
    this.phase = Objects.requireNonNull(phase);
    this.roundIndex = roundIndex;
    this.cases = List.copyOf(cases);
    this.playerCaseId = playerCaseId;
    this.openedCaseIds = List.copyOf(openedCaseIds);
    this.currentOfferCents = currentOfferCents;
  }

  public Phase phase() { return phase; }
  public int roundIndex() { return roundIndex; }
  public List<Briefcase> cases() { return cases; }
  public Integer playerCaseId() { return playerCaseId; }
  public List<Integer> openedCaseIds() { return openedCaseIds; }
  public Integer currentOfferCents() { return currentOfferCents; }

  static GameState initial(List<Briefcase> shuffled) {
    return new GameState(Phase.PICK_CASE, 0, shuffled, null, Collections.emptyList(), null);
  }

  public GameState withCases(List<Briefcase> newCases) {
    return new GameState(phase, roundIndex, newCases, playerCaseId, openedCaseIds, currentOfferCents);
  }

  public GameState withOpened(int caseId) {
    var newOpened = new ArrayList<>(openedCaseIds);
    newOpened.add(caseId);
    return new GameState(phase, roundIndex, cases, playerCaseId, List.copyOf(newOpened), currentOfferCents);
  }
}

