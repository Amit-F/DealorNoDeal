package deal.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class Engine {
    private final GameConfig cfg;
    private final Random rng;

    public Engine(GameConfig cfg, long seed) {
        this.cfg = cfg;
        this.rng = new Random(seed);
    }

    public GameState start() {
        List<Integer> shuffled = new ArrayList<>(cfg.amountsCents());
        Collections.shuffle(shuffled, rng);
        List<Briefcase> cases = new ArrayList<>(shuffled.size());
        for (int i = 0; i < shuffled.size(); i++) {
            cases.add(new Briefcase(i + 1, shuffled.get(i), false));
        }
        return GameState.initial(cases);
    }
}
