package deal.core;
import java.util.List;
/** Provides the prize amounts (in cents) for a given total number of cases. */
public interface PrizeLadderProvider { List<Integer> amountsFor(int caseCount); }

