package deal.cli;

final class CliOptions {
    final int caseCount;
    final long seed;
    final boolean help;

    private CliOptions(int caseCount, long seed, boolean help) {
        this.caseCount = caseCount;
        this.seed = seed;
        this.help = help;
    }

    static CliOptions from(Args args) {
        boolean help = args.has("help") || args.has("h");
        int caseCount =
                args.getInt(
                        "cases",
                        10,
                        v -> {
                            if (v < 2) throw new IllegalArgumentException("--cases must be >= 2");
                        });
        long seed;
        String seedStr = args.get("seed");
        if (seedStr == null) {
            seed = 42L;
        } else {
            try {
                seed = Long.parseLong(seedStr.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid long for --seed: " + seedStr);
            }
        }
        return new CliOptions(caseCount, seed, help);
    }

    static String usage() {
        return String.join(
                System.lineSeparator(),
                "Deal or No Deal (v2 CLI)",
                "Usage:",
                "  --cases=<N>      Number of briefcases (e.g., 10 or 25).",
                "  --seed=<long>    RNG seed for deterministic shuffles (default: 42).",
                "  --help | -h      Show this help.");
    }
}
