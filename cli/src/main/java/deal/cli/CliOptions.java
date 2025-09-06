package deal.cli;

final class CliOptions {
    final int caseCount; // >= 2 for fixed, or -1 means "custom" (prompt)
    final long seed;
    final boolean help;
    final boolean showEv; // used by Main.java
    final String transcriptPath; // used by Main.java

    private CliOptions(
            int caseCount, long seed, boolean help, boolean showEv, String transcriptPath) {
        this.caseCount = caseCount;
        this.seed = seed;
        this.help = help;
        this.showEv = showEv;
        this.transcriptPath = transcriptPath;
    }

    static CliOptions from(Args args) {
        // help
        boolean help = args.has("help") || args.has("h");

        // --cases (default 25). If "custom", we return -1 and Main will prompt.
        String casesStr = args.getOne("cases", "25");
        int caseCount;
        if ("custom".equalsIgnoreCase(casesStr)) {
            caseCount = -1; // sentinel for interactive prompt
        } else {
            try {
                caseCount = Integer.parseInt(casesStr);
                if (caseCount < 2)
                    throw new IllegalArgumentException(
                            "cases must be >= 2 (got " + caseCount + ")");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid integer for --cases: " + casesStr);
            }
        }

        // --seed (default 42)
        String seedStr = args.getOne("seed", "42");
        long seed;
        try {
            seed = Long.parseLong(seedStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer for --seed: " + seedStr);
        }

        // --show-ev (default true). Accept true/false/1/0 (case-insensitive)
        String sev = args.getOne("show-ev", "true");
        boolean showEv;
        String v = sev.trim().toLowerCase();
        if (v.equals("false") || v.equals("0")) showEv = false;
        else if (v.equals("true") || v.equals("1")) showEv = true;
        else showEv = true; // lenient default

        // --transcript (optional path; blank -> null)
        String transcriptPath = args.getOne("transcript", "");
        if (transcriptPath != null && transcriptPath.isBlank()) transcriptPath = null;

        return new CliOptions(caseCount, seed, help, showEv, transcriptPath);
    }

    static String usage() {
        return String.join(
                System.lineSeparator(),
                "Deal or No Deal (v2 CLI)",
                "Usage:",
                "  --cases=<N|custom>     Number of briefcases (e.g., 10 or 25), or 'custom' for a"
                        + " prompt.",
                "  --seed=<long>          RNG seed for deterministic shuffles (default: 42).",
                "  --show-ev=<true|false> Show/hide EV & offer/EV advisor line (default: true).",
                "  --transcript=<file>    Export a transcript to .json or .csv.",
                "  --help | -h            Show this help.");
    }
}
