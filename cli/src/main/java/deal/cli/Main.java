package deal.cli;

import deal.core.Engine;
import deal.core.GameConfig;
import deal.core.GameState;
import deal.core.Phase;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class Main {
    public static void main(String[] args) throws Exception {
        Args parsed = Args.parse(args);
        CliOptions opt;
        try {
            opt = CliOptions.from(parsed);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.err.println();
            System.err.println(CliOptions.usage());
            System.exit(2);
            return;
        }

        if (opt.help) {
            System.out.println(CliOptions.usage());
            return;
        }

        GameState s;
        try {
            s = new Engine(GameConfig.of(opt.caseCount), opt.seed).start();
        } catch (IllegalArgumentException ex) {
            System.err.println(
                    "Invalid case count: " + opt.caseCount + " (supported: 10 or 25 for now)");
            System.exit(2);
            return;
        }

        var in = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Welcome to Deal or No Deal (v2)");
        System.out.println("Cases: " + s.cases().size());

        // PICK_CASE
        while (s.phase() == Phase.PICK_CASE) {
            System.out.print("Pick your case (1.." + s.cases().size() + "): ");
            int pick = parseInt(in.readLine());
            try {
                s =
                        new deal.core.Engine(GameConfig.of(opt.caseCount), opt.seed)
                                .pickPlayerCase(s, pick);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        // rounds
        var engine = new deal.core.Engine(GameConfig.of(opt.caseCount), opt.seed);
        while (true) {
            int unopenedNonPlayer = s.cases().size() - 1 - s.openedCaseIds().size();
            System.out.print("How many cases to open this round (1.." + unopenedNonPlayer + "): ");
            int k = parseInt(in.readLine());
            try {
                s = engine.chooseToOpen(s, k);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                continue;
            }

            for (int i = 0; i < k; i++) {
                System.out.print("Open which case id? ");
                int id = parseInt(in.readLine());
                try {
                    s = engine.openCase(s, id);
                    System.out.println("Opened case " + id);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    i--; // retry same index
                }
            }

            s = engine.computeOffer(s);
            System.out.println("Banker offers: " + s.currentOfferCents() + " cents");
            System.out.print("Deal or No Deal? (d/n): ");
            String ans = in.readLine().trim().toLowerCase();
            if (ans.startsWith("d")) {
                s = engine.acceptDeal(s);
                System.out.println("DEAL! Winnings: " + s.currentOfferCents() + " cents");
                break;
            } else {
                s = engine.declineDeal(s);
                System.out.println("No Deal! Next round...");
            }

            int remaining = s.cases().size() - s.openedCaseIds().size();
            if (remaining == 1) {
                System.out.println("Only your case remains! (Final reveal coming in Stage 5.)");
                break;
            }
        }
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return Integer.MIN_VALUE; // will be rejected by engine validation
        }
    }
}
