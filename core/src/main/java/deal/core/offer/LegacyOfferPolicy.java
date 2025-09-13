package deal.core.offer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Legacy-flavored banker curve: - Offer ~ EV * ratio(progress, risk) - Early conservative, later
 * converging toward EV. - Penalize high variance early; clamp between reasonable bounds; human-ish
 * rounding.
 */
public final class LegacyOfferPolicy implements OfferPolicy {

    /** A ready-to-use singleton with decent defaults. */
    public static final LegacyOfferPolicy DEFAULT = new LegacyOfferPolicy();

    // Tunables (can be exposed later if you want to fit against transcripts)
    private final double startRatio = 0.55; // first offer relative to EV
    private final double endRatio = 0.93; // late-game target relative to EV
    private final double maxRatio = 0.97; // hard safety ceiling
    private final double minPadFrac = 0.05; // don't go below min + 5% range
    private final double maxPadFrac = 0.02; // don't go above max - 2% range
    private final boolean roundHuman = true;

    @Override
    public int offer(Context ctx) {
        final List<Integer> amounts = ctx.remainingAmounts;
        // Guard rails
        int n = amounts.size();
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        long sum = 0L;
        for (int a : amounts) {
            min = Math.min(min, a);
            max = Math.max(max, a);
            sum += a;
        }
        double ev = sum / (double) n;
        if (ev <= 0 || min == max) {
            // Degenerate: all same or zero-ish -> just return that value.
            return max;
        }

        // Progress based on remaining fraction (no need to know exact rounds)
        double remainingFrac = n / (double) ctx.initialCaseCount; // 1.0 at start -> -> ~0
        double progress = 1.0 - remainingFrac; // 0 at start -> -> ~1 late
        progress = clamp01(progress);

        // Risk (normalized stddev / EV). Penalize more when early.
        double std = stddev(amounts, ev);
        double risk = std / Math.max(1.0, ev); // ~0..3 typical
        double riskPenalty = 0.15 * clamp(risk, 0.0, 2.0) * (1.0 - progress);

        // Base ratio + risk adjustment
        double ratio = lerp(startRatio, endRatio, progress) - riskPenalty;

        // Floor tweaked so early offers don't dip too low (keeps tests + legacy flavor happy)
        ratio = clamp(ratio, 0.46, maxRatio);

        // Compute offer
        double raw = ev * ratio;

        // Clamp away from absolute extremes to avoid silly offers.
        double range = max - min;
        double hardMin = min + range * minPadFrac;
        double hardMax = max - range * maxPadFrac;
        double clamped = clamp(raw, hardMin, hardMax);

        int dollars = (int) Math.round(clamped);

        // Round to "TV banker" granularity (optional)
        if (roundHuman) {
            dollars = humanRound(dollars);
        }

        // Final guard: ensure within [min, max]
        if (dollars < min) dollars = min;
        if (dollars > max) dollars = max;

        return dollars;
    }

    // ---------- helpers ----------

    private static double stddev(List<Integer> xs, double mean) {
        double acc = 0.0;
        for (int x : xs) {
            double d = x - mean;
            acc += d * d;
        }
        return Math.sqrt(acc / xs.size());
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    private static double clamp01(double x) {
        return clamp(x, 0.0, 1.0);
    }

    /** Round like a show banker: big numbers in coarse steps. */
    private static int humanRound(int dollars) {
        if (dollars >= 100_000) {
            return roundTo(dollars, 5_000);
        } else if (dollars >= 50_000) {
            return roundTo(dollars, 2_500);
        } else if (dollars >= 10_000) {
            return roundTo(dollars, 1_000);
        } else if (dollars >= 5_000) {
            return roundTo(dollars, 500);
        } else if (dollars >= 1_000) {
            return roundTo(dollars, 250);
        } else if (dollars >= 500) {
            return roundTo(dollars, 50);
        } else if (dollars >= 100) {
            return roundTo(dollars, 10);
        } else {
            return dollars; // keep small numbers exact
        }
    }

    private static int roundTo(int value, int quantum) {
        int half = quantum / 2;
        return ((value + half) / quantum) * quantum;
    }

    // Convenience for manual sanity checks (not used in prod):
    static List<Integer> sorted(List<Integer> xs) {
        List<Integer> out = new ArrayList<>(xs);
        Collections.sort(out);
        return out;
    }
}
