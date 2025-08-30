package deal.core;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List; import org.junit.jupiter.api.Test;

class EngineStartTest {
  @Test void startPhaseIsPickCase() {
    var engine = new Engine(GameConfig.of(10), 42L);
    var s = engine.start();
    assertThat(s.phase()).isEqualTo(Phase.PICK_CASE);
    assertThat(s.cases()).hasSize(10);
  }

  @Test void shuffleIsDeterministicForSeed() {
    var ladder = new PrizeLadderProvider() {
      @Override public List<Integer> amountsFor(int n) { return List.of(1, 10, 100, 1_000).subList(0, n); }
    };
    var cfg = new GameConfig(4, ladder, new CustomPerRoundPolicy());
    var e1 = new Engine(cfg, 123L);
    var e2 = new Engine(cfg, 123L);
    var seq1 = e1.start().cases().stream().map(Briefcase::amountCents).toList();
    var seq2 = e2.start().cases().stream().map(Briefcase::amountCents).toList();
    assertThat(seq1).isEqualTo(seq2);
  }
}

