package deal.core;

public final class CustomPerRoundPolicy implements RoundPolicy {
    @Override
    public boolean isAllowed(int unopenedCases, int chosenToOpen) {
        if (chosenToOpen < 1) return false;
        return chosenToOpen < unopenedCases; // leave at least one closed
    }
}
