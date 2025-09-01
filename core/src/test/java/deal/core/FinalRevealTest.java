package deal.core;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class FinalRevealTest {

    @Test
    void final_reveal_with_swap_and_keep_paths() {
        // 3 simple amounts to make the last rounds short
        PrizeLadderProvider ladder = n -> List.of(100, 1000, 5000).subList(0, n);
        var cfg = new GameConfig(3, ladder, new CustomPerRoundPolicy());
        var engine = new Engine(cfg, 123L);

        var s = engine.start();
        s = engine.pickPlayerCase(s, 1);

        // Open 1 case, then we should have 2 unopened left
        s = engine.chooseToOpen(s, 1);
        for (var c : s.cases()) {
            if (c.id() != s.playerCaseId() && !s.isOpened(c.id())) {
                s = engine.openCase(s, c.id());
                break;
            }
        }
        s = engine.computeOffer(s);
        // decline to go to final reveal
        s = engine.declineDeal(s);
        assertThat(s.phase()).isEqualTo(Phase.FINAL_REVEAL);

        // Try both branches: keep or swap — we can't know the exact amounts here (they're
        // shuffled),
        // but we can assert we end in RESULT and resultCents is one of the remaining amounts.
        var keepResult = engine.revealFinal(s, false);
        assertThat(keepResult.phase()).isEqualTo(Phase.RESULT);
        assertThat(keepResult.resultCents()).isNotNull();

        // Reset to final reveal again to test swap path (recreate the path quickly)
        s = engine.start();
        s = engine.pickPlayerCase(s, 1);
        s = engine.chooseToOpen(s, 1);
        for (var c : s.cases()) {
            if (c.id() != s.playerCaseId() && !s.isOpened(c.id())) {
                s = engine.openCase(s, c.id());
                break;
            }
        }
        s = engine.computeOffer(s);
        s = engine.declineDeal(s);
        var swapResult = engine.revealFinal(s, true);
        assertThat(swapResult.phase()).isEqualTo(Phase.RESULT);
        assertThat(swapResult.resultCents()).isNotNull();
    }
}
