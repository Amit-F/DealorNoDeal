package deal.core;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class RoundFlowTest {

    @Test
    void pickCase_then_chooseK_then_openK_then_offer() {
        PrizeLadderProvider ladder = n -> List.of(1, 100, 1_000, 10_000, 50_000).subList(0, n);
        var cfg = new GameConfig(5, ladder, new CustomPerRoundPolicy());
        var engine = new Engine(cfg, 123L);

        var s0 = engine.start();
        assertThat(s0.phase()).isEqualTo(Phase.PICK_CASE);

        var s1 = engine.pickPlayerCase(s0, 3);
        assertThat(s1.playerCaseId()).isEqualTo(3);
        assertThat(s1.phase()).isEqualTo(Phase.ROUND);

        var s2 = engine.chooseToOpen(s1, 2);
        assertThat(s2.toOpenInThisRound()).isEqualTo(2);

        int any1 =
                s2.cases().stream()
                        .map(Briefcase::id)
                        .filter(i -> i != 3)
                        .findFirst()
                        .orElseThrow();
        var s3 = engine.openCase(s2, any1);
        assertThat(s3.toOpenInThisRound()).isEqualTo(1);

        int any2 =
                s3.cases().stream()
                        .map(Briefcase::id)
                        .filter(i -> i != 3 && !s3.openedCaseIds().contains(i))
                        .findFirst()
                        .orElseThrow();
        var s4 = engine.openCase(s3, any2);
        assertThat(s4.toOpenInThisRound()).isEqualTo(0);

        var s5 = engine.computeOffer(s4);
        assertThat(s5.phase()).isEqualTo(Phase.OFFER);
        assertThat(s5.currentOfferCents()).isNotNull();
        assertThat(s5.currentOfferCents()).isGreaterThan(0);
    }

    @Test
    void policy_enforced_when_chooseToOpen() {
        var cfg = GameConfig.of(10);
        var engine = new Engine(cfg, 42L);
        var s0 = engine.start();
        var sPicked = engine.pickPlayerCase(s0, 1); // final/effectively final snapshot

        // not allowed: 0 or absurdly large K
        assertThatThrownBy(() -> engine.chooseToOpen(sPicked, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> engine.chooseToOpen(sPicked, 9_999))
                .isInstanceOf(IllegalArgumentException.class);

        // allowed: 1..(unopened-1)
        var ok = engine.chooseToOpen(sPicked, 3);
        assertThat(ok.toOpenInThisRound()).isEqualTo(3);
    }
}
