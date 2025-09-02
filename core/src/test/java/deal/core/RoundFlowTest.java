package deal.core;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class RoundFlowTest {

    @Test
    void basic_round_flow_and_offer() {
        // small, known ladder (dollars)
        PrizeLadderProvider ladder = n -> List.of(100, 200, 500, 1_000, 5_000).subList(0, n);
        var cfg = new GameConfig(5, ladder, new CustomPerRoundPolicy());
        var engine = new Engine(cfg, 7L);

        var s1 = engine.start();
        var s2 = engine.pickPlayerCase(s1, 1);

        // Choose to open 2 cases
        var s3 = engine.chooseToOpen(s2, 2);

        // Open two valid non-player cases
        int opened = 0;
        var s4 = s3;
        for (var c : s3.cases()) {
            if (c.id() != s3.playerCaseId()
                    && !s4.isOpened(c.id())
                    && opened < s3.toOpenInThisRound()) {
                s4 = engine.openCase(s4, c.id());
                opened++;
            }
        }
        assertThat(opened).isEqualTo(2);

        // Compute banker offer
        var s5 = engine.computeOffer(s4);
        assertThat(s5.currentOfferDollars()).isNotNull();
        assertThat(s5.currentOfferDollars()).isGreaterThan(0);

        // Decline deal -> either next ROUND (if >2 remain) or FINAL_REVEAL (if only 2 remain)
        var s6 = engine.declineDeal(s5);
        assertThat(s6.phase() == Phase.ROUND || s6.phase() == Phase.FINAL_REVEAL).isTrue();
    }

    @Test
    void invalid_K_values_are_rejected() {
        PrizeLadderProvider ladder = n -> List.of(100, 200, 500, 1_000, 5_000).subList(0, n);
        var cfg = new GameConfig(5, ladder, new CustomPerRoundPolicy());
        var engine = new Engine(cfg, 11L);

        var s = engine.start();
        s = engine.pickPlayerCase(s, 2);

        // Take a final snapshot of state for lambda assertions.
        final var sAt = s;

        // unopened non-player at start = 4
        assertThatThrownBy(() -> engine.chooseToOpen(sAt, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> engine.chooseToOpen(sAt, 4))
                .isInstanceOf(IllegalArgumentException.class);

        // allowed: 1..3
        assertThatCode(() -> engine.chooseToOpen(sAt, 1)).doesNotThrowAnyException();
        assertThatCode(() -> engine.chooseToOpen(sAt, 3)).doesNotThrowAnyException();
    }
}
