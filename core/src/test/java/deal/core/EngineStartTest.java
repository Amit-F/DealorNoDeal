package deal.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class EngineStartTest {

    @Test
    void shuffle_is_seeded_and_deterministic() {
        // simple ladder (dollars)
        PrizeLadderProvider ladder = n -> List.of(100, 200, 500, 1_000, 5_000).subList(0, n);
        var cfg = new GameConfig(5, ladder, new CustomPerRoundPolicy());

        var e1 = new Engine(cfg, 123L);
        var e2 = new Engine(cfg, 123L);
        var e3 = new Engine(cfg, 124L);

        var seq1 = e1.start().cases().stream().map(Briefcase::amountDollars).toList();
        var seq2 = e2.start().cases().stream().map(Briefcase::amountDollars).toList();
        var seq3 = e3.start().cases().stream().map(Briefcase::amountDollars).toList();

        assertThat(seq1).isEqualTo(seq2); // same seed -> same shuffle
        assertThat(seq1).isNotEqualTo(seq3); // different seed -> likely different shuffle
    }
}
