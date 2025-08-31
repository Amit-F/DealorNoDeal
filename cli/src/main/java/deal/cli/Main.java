package deal.cli;

import deal.core.Engine;
import deal.core.GameConfig;

public final class Main {
    public static void main(String[] args) {
        Args parsed = Args.parse(args);
        CliOptions opt;
        try {
            opt = CliOptions.from(parsed);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.err.println();
            System.err.println(CliOptions.usage());
            System.exit(2);
            return; // unreachable, but keeps javac happy
        }

        if (opt.help) {
            System.out.println(CliOptions.usage());
            return;
        }

        var s = new Engine(GameConfig.of(opt.caseCount), opt.seed).start();
        System.out.println("v2 engine: cases=" + s.cases().size() + ", phase=" + s.phase());
        System.out.println("Legacy v1: ./run-legacy.sh");
    }
}
