package deal.core;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class CounterOfferTest {

    @Test
    void banker_accepts_reasonable_counter() {
        // small, known ladder in dollars
        PrizeLadderProvider ladder = n -> List.of(100, 200, 500, 1_000, 5_000).subList(0, n);
        var cfg = new GameConfig(5, ladder, new CustomPerRoundPolicy());
        var engine = new Engine(cfg, 1L);

        var s = engine.start();
        s = engine.pickPlayerCase(s, 1);

        s = engine.chooseToOpen(s, 2);
        int opened = 0;
        for (var c : s.cases()) {
            if (c.id() != s.playerCaseId() && opened < 2) {
                s = engine.openCase(s, c.id());
                opened++;
            }
        }
        s = engine.computeOffer(s);

        int counter =
                Math.max(150, s.currentOfferDollars() != null ? s.currentOfferDollars() : 200);
        s = engine.proposeCounter(s, counter);
        s = engine.resolveCounter(s);

        assertThat(s.phase()).isEqualTo(Phase.RESULT);
        assertThat(s.resultDollars()).isEqualTo(counter);
    }

    @Test
    void banker_rejects_extreme_counter_and_game_continues() {
        PrizeLadderProvider ladder = n -> List.of(100, 200, 500, 1_000, 5_000).subList(0, n);
        var cfg = new GameConfig(5, ladder, new CustomPerRoundPolicy());
        var engine = new Engine(cfg, 2L);

        var s = engine.start();
        s = engine.pickPlayerCase(s, 2);

        s = engine.chooseToOpen(s, 1);
        for (var c : s.cases()) {
            if (c.id() != s.playerCaseId() && !s.isOpened(c.id())) {
                s = engine.openCase(s, c.id());
                break;
            }
        }
        s = engine.computeOffer(s);

        // compute remaining amounts without lambdas (avoid non-final capture issues)
        var openedIds = new HashSet<>(s.openedCaseIds());
        List<Integer> remaining = new ArrayList<>();
        for (var c : s.cases()) {
            if (!openedIds.contains(c.id())) remaining.add(c.amountDollars());
        }

        double ev = 0.0;
        int maxRemaining = 0;
        for (int amt : remaining) {
            ev += amt;
            if (amt > maxRemaining) maxRemaining = amt;
        }
        ev = remaining.isEmpty() ? 0.0 : ev / remaining.size();

        double acceptanceFactor = Math.min(1.10, 0.95 + 0.03 * s.roundIndex());

        int threshold = (int) Math.ceil(ev * acceptanceFactor);
        int counter = Math.min(maxRemaining, threshold + 1_000);
        if (counter <= threshold) counter = maxRemaining;

        s = engine.proposeCounter(s, counter);
        var s2 = engine.resolveCounter(s);

        assertThat(s2.phase() == Phase.ROUND || s2.phase() == Phase.FINAL_REVEAL).isTrue();
    }
}
