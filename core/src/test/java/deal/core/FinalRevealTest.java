package deal.core;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class FinalRevealTest {

    @Test
    void final_reveal_with_swap_and_keep_paths() {
        // 3 simple amounts (dollars) to make flow short
        PrizeLadderProvider ladder = n -> List.of(100, 1_000, 5_000).subList(0, n);
        var cfg = new GameConfig(3, ladder, new CustomPerRoundPolicy());
        var engine = new Engine(cfg, 123L);

        var s = engine.start();
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
        assertThat(s.phase()).isEqualTo(Phase.FINAL_REVEAL);

        var keepResult = engine.revealFinal(s, false);
        assertThat(keepResult.phase()).isEqualTo(Phase.RESULT);
        assertThat(keepResult.resultDollars()).isNotNull();

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
        assertThat(swapResult.resultDollars()).isNotNull();
    }
}
