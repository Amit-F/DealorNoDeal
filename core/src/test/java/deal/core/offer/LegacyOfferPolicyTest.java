package deal.core.offer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class LegacyOfferPolicyTest {

    private static final List<Integer> L25 =
            // mirror DefaultPrizeLadder.LEGACY_25 (lowest->highest)
            List.of(
                    1, 5, 10, 25, 50, 75, 100, 200, 300, 400, 500, 750, 1_000, 5_000, 10_000,
                    25_000, 50_000, 75_000, 100_000, 200_000, 300_000, 400_000, 500_000, 750_000,
                    1_000_000);

    @Test
    void earlyOffer_isConservative_andWithinBounds() {
        var policy = LegacyOfferPolicy.DEFAULT;

        // Start of a 10-case game
        var ctx =
                new OfferPolicy.Context(
                        10, List.of(1, 5, 10, 25, 50, 75, 100, 200, 300, 400), 0, null);

        int offer = policy.offer(ctx);
        int min = 1, max = 400;
        assertTrue(offer >= min && offer <= max, "offer must be within [min,max]");
        double ev = (1 + 5 + 10 + 25 + 50 + 75 + 100 + 200 + 300 + 400) / 10.0;
        double ratio = offer / ev;
        assertTrue(ratio >= 0.45 && ratio <= 0.75, "early offer should be conservative: " + ratio);
    }

    @Test
    void lateOffer_tracksEV_andNeverExceedsMax() {
        var policy = LegacyOfferPolicy.DEFAULT;

        // Near end of 25-case game (2 left)
        var remaining = List.of(1, 1_000_000);
        var ctx = new OfferPolicy.Context(25, remaining, 23, 100_000);

        int offer = policy.offer(ctx);
        int min = 1, max = 1_000_000;
        assertTrue(offer >= min && offer <= max, "offer must be within [min,max]");

        double ev = (1 + 1_000_000) / 2.0;
        double ratio = offer / ev;
        assertTrue(ratio >= 0.80 && ratio <= 0.97, "late offer should be near EV: " + ratio);
    }

    @Test
    void midgame_monotonicity_withHigherEV() {
        var policy = LegacyOfferPolicy.DEFAULT;

        // Two mid-game states with different EVs but similar spread
        var ctxLow = new OfferPolicy.Context(25, L25.subList(0, 8), 17, null); // up to $200
        var ctxHigh = new OfferPolicy.Context(25, L25.subList(0, 12), 13, null); // up to $1_000

        int low = policy.offer(ctxLow);
        int high = policy.offer(ctxHigh);

        assertTrue(high >= low, "offer should increase with EV when other factors comparable");
    }

    @Test
    void clamp_respects_minimum_and_maximum() {
        var policy = LegacyOfferPolicy.DEFAULT;
        var ctx = new OfferPolicy.Context(25, List.of(100, 1_000_000), 10, null);
        int offer = policy.offer(ctx);
        assertTrue(offer >= 100 && offer <= 1_000_000, "offer within min/max");
    }
}
