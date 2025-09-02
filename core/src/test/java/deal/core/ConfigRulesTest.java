package deal.core;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigRulesTest {

    @Test
    void ladder_size_matches_case_count() {
        var cfg10 = GameConfig.of(10);
        assertThat(cfg10.amountsDollars()).hasSize(10);
    }

    @Test
    void customPolicyAllowsReasonableKs() {
        // Use a small game to check policy boundaries.
        PrizeLadderProvider ladder = n -> List.of(100, 200, 300, 400, 500).subList(0, n);
        var cfg = new GameConfig(5, ladder, new CustomPerRoundPolicy());
        var engine = new Engine(cfg, 1L);
        var s = engine.start();
        s = engine.pickPlayerCase(s, 1);

        int unopenedNonPlayer = s.cases().size() - 1 - s.openedCaseIds().size();
        assertThat(cfg.roundPolicy().isAllowed(unopenedNonPlayer, 1)).isTrue();
        assertThat(cfg.roundPolicy().isAllowed(unopenedNonPlayer, unopenedNonPlayer - 1)).isTrue();
        assertThat(cfg.roundPolicy().isAllowed(unopenedNonPlayer, 0)).isFalse(); // must open >= 1
        assertThat(cfg.roundPolicy().isAllowed(unopenedNonPlayer, unopenedNonPlayer))
                .isFalse(); // must leave at least one closed (besides player's)
    }
}
