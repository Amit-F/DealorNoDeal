package deal.core;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class CounterOfferTest {

    @Test
    void banker_accepts_reasonable_counter() {
        // Small, known ladder to make EV easy.
        PrizeLadderProvider ladder = n -> List.of(100, 200, 500, 1000, 5000).subList(0, n);
        var cfg = new GameConfig(5, ladder, new CustomPerRoundPolicy());
        var engine = new Engine(cfg, 1L);

        var s = engine.start();
        s = engine.pickPlayerCase(s, 1);

        // Choose to open 2, open two cases, then compute offer
        s = engine.chooseToOpen(s, 2);
        int opened = 0;
        for (var c : s.cases()) {
            if (c.id() != s.playerCaseId() && opened < 2) {
                s = engine.openCase(s, c.id());
                opened++;
            }
        }
        s = engine.computeOffer(s);

        // Reasonable counter: at or below typical acceptance threshold -> banker accepts
        // deterministically
        int counter = Math.max(150, s.currentOfferCents() != null ? s.currentOfferCents() : 200);
        s = engine.proposeCounter(s, counter);
        s = engine.resolveCounter(s);

        assertThat(s.phase()).isEqualTo(Phase.RESULT);
        assertThat(s.resultCents()).isEqualTo(counter);
    }

    @Test
    void banker_rejects_extreme_counter_and_game_continues() {
        PrizeLadderProvider ladder = n -> List.of(100, 200, 500, 1000, 5000).subList(0, n);
        var cfg = new GameConfig(5, ladder, new CustomPerRoundPolicy());
        var engine = new Engine(cfg, 2L);

        var s = engine.start();
        s = engine.pickPlayerCase(s, 2);

        s = engine.chooseToOpen(s, 1);
        // Open one non-player case
        for (var c : s.cases()) {
            if (c.id() != s.playerCaseId() && !s.isOpened(c.id())) {
                s = engine.openCase(s, c.id());
                break;
            }
        }
        s = engine.computeOffer(s);

        // Build remaining amounts WITHOUT lambdas to avoid capturing a non-final 's'
        var openedIds = new HashSet<>(s.openedCaseIds());
        List<Integer> remaining = new ArrayList<>();
        for (var c : s.cases()) {
            if (!openedIds.contains(c.id())) remaining.add(c.amountCents());
        }

        double ev = 0.0;
        int maxRemaining = 0;
        for (int amt : remaining) {
            ev += amt;
            if (amt > maxRemaining) maxRemaining = amt;
        }
        ev = remaining.isEmpty() ? 0.0 : ev / remaining.size();

        double acceptanceFactor = Math.min(1.10, 0.95 + 0.03 * s.roundIndex());

        // Pick a counter just ABOVE the acceptance threshold but not above maxRemaining.
        int threshold = (int) Math.ceil(ev * acceptanceFactor);
        int counter = Math.min(maxRemaining, threshold + 1_000);
        if (counter <= threshold) {
            // Fallback: if +1000 didn't exceed threshold (due to bounds), use maxRemaining (still >
            // threshold).
            counter = maxRemaining;
        }

        s = engine.proposeCounter(s, counter);
        var s2 = engine.resolveCounter(s);

        // If rejected, gameplay continues: either next ROUND or FINAL_REVEAL when <= 2 cases
        // remain.
        assertThat(s2.phase() == Phase.ROUND || s2.phase() == Phase.FINAL_REVEAL).isTrue();
    }
}
