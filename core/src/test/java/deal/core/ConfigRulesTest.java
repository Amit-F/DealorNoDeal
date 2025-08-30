package deal.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ConfigRulesTest {
    @Test
    void ladderMatchesCaseCount() {
        assertThat(GameConfig.of(10).amountsCents()).hasSize(10);
        assertThatThrownBy(() -> GameConfig.of(11)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void customPolicyAllowsReasonableKs() {
        var p = new CustomPerRoundPolicy();
        assertThat(p.isAllowed(10, 1)).isTrue();
        assertThat(p.isAllowed(10, 9)).isTrue();
        assertThat(p.isAllowed(10, 0)).isFalse();
        assertThat(p.isAllowed(10, 10)).isFalse();
    }
}
