package deal.cli;

import deal.core.Engine;
import deal.core.GameConfig;
import deal.core.GameState;
import deal.core.Phase;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;

public final class Main {

    public static void main(String[] args) throws Exception {
        // Parse CLI options (already implemented in Stage 3)
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

        // Build engine & start new game
        Engine engine;
        GameState s;
        try {
            var cfg = GameConfig.of(opt.caseCount);
            engine = new Engine(cfg, opt.seed);
            s = engine.start();
        } catch (IllegalArgumentException ex) {
            System.err.println("Invalid configuration: " + ex.getMessage());
            System.exit(2);
            return;
        }

        var in = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Welcome to Deal or No Deal (v2)");
        System.out.println("Cases: " + s.cases().size());
        printRemainingBrief(s);

        // ---- INTERACTIVITY GUARD ----
        // Some environments (or gradle run invocations) do not attach a proper stdin.
        // If we can't read at least one line, exit gracefully to avoid infinite loops.
        System.out.print("Press ENTER to start (or Ctrl+C to quit): ");
        String probe = readLine(in);
        if (probe == null) {
            System.err.println();
            System.err.println("No interactive input detected.");
            System.err.println("Try either:");
            System.err.println(
                    "  - Running the app in a terminal (not inside a non-interactive shell), OR");
            System.err.println("  - Building the distribution and running the generated script:");
            System.err.println(
                    "      ./gradlew :cli:installDist && ./cli/build/install/cli/bin/cli --cases=10"
                            + " --seed=42");
            System.exit(1);
            return;
        }

        // PHASE: PICK_CASE
        while (s.phase() == Phase.PICK_CASE) {
            Integer pick = askInt(in, "Pick your case (1.." + s.cases().size() + "): ");
            if (pick == null) exitNoInput();
            try {
                s = engine.pickPlayerCase(s, pick);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        // Main loop: ROUND -> OFFER/COUNTEROFFER -> (DECLINE => next round or FINAL_REVEAL)
        while (true) {
            if (s.phase() == Phase.ROUND) {
                // Choose K for this round
                int unopenedNonPlayer = s.cases().size() - 1 - s.openedCaseIds().size();
                System.out.println();
                System.out.println(
                        "Round "
                                + (s.roundIndex() + 1)
                                + " — unopened (excluding your case): "
                                + unopenedNonPlayer);
                Integer k =
                        askInt(
                                in,
                                "How many cases to open this round (1.."
                                        + unopenedNonPlayer
                                        + "): ");
                if (k == null) exitNoInput();
                try {
                    s = engine.chooseToOpen(s, k);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    continue; // reprompt K
                }

                // ---- IMPORTANT: capture K ONCE to avoid changing loop bound mid-iteration ----
                int kToOpen = s.toOpenInThisRound();
                int openedThisRound = 0;
                while (openedThisRound < kToOpen) {
                    Integer id = askInt(in, "Open which case id? ");
                    if (id == null) exitNoInput();
                    try {
                        s = engine.openCase(s, id);
                        openedThisRound++;
                        System.out.println("Opened case " + id);
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }

                // Compute banker offer
                s = engine.computeOffer(s);
            }

            if (s.phase() == Phase.OFFER) {
                System.out.println();
                System.out.println("Banker offers: " + s.currentOfferCents() + " cents");
                System.out.println(
                        "Choose: [d] Deal  |  [n] No Deal  |  [c <amount>] Counteroffer");
                System.out.print("> ");
                String line = readLine(in);
                if (line == null) exitNoInput();
                line = line.trim().toLowerCase();

                if (line.equals("d") || line.startsWith("deal")) {
                    s = engine.acceptDeal(s);
                    System.out.println("DEAL! Winnings: " + s.resultCents() + " cents");
                    break;
                } else if (line.equals("n") || line.startsWith("no")) {
                    s = engine.declineDeal(s);
                    if (s.phase() == Phase.FINAL_REVEAL) {
                        // handled in the FINAL_REVEAL block below
                    } else {
                        printRemainingBrief(s);
                    }
                } else if (line.startsWith("c")) {
                    // counter: formats like "c 25000" or "counter 25000"
                    Integer amount = parseCounter(line);
                    if (amount == null) {
                        System.out.println("Usage: c <amount>   (example: c 25000)");
                        continue;
                    }
                    try {
                        s = engine.proposeCounter(s, amount);
                        s = engine.resolveCounter(s);
                        if (s.phase() == Phase.RESULT) {
                            System.out.println(
                                    "Banker ACCEPTED your counter! Winnings: "
                                            + s.resultCents()
                                            + " cents");
                            break;
                        } else {
                            System.out.println("Banker rejected your counter. Continuing...");
                            if (s.phase() != Phase.FINAL_REVEAL) {
                                printRemainingBrief(s);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                } else {
                    System.out.println("Unrecognized input. Use: d | n | c <amount>");
                }
            }

            if (s.phase() == Phase.COUNTEROFFER) {
                // Transient in this CLI: resolveCounter() is called immediately above.
                continue;
            }

            if (s.phase() == Phase.FINAL_REVEAL) {
                // Two cases left: player's case and one other. Ask to swap.
                System.out.println();
                System.out.println("FINAL REVEAL: Only two cases remain (including yours).");
                String swapAns = askYesNo(in, "Swap your case? (y/n): ");
                boolean swap = swapAns.startsWith("y");
                s = engine.revealFinal(s, swap);
                System.out.println(
                        (swap ? "You swapped." : "You kept your case.")
                                + " Final winnings: "
                                + s.resultCents()
                                + " cents");
                break;
            }

            if (s.phase() == Phase.RESULT) {
                // Should only get here via DEAL or reveal
                System.out.println("Game over. Winnings: " + s.resultCents() + " cents");
                break;
            }
        }
    }

    // ---------- helpers ----------

    private static String readLine(BufferedReader in) {
        try {
            return in.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    private static void exitNoInput() {
        System.err.println();
        System.err.println("No interactive input detected.");
        System.err.println("Try running the installed app script instead:");
        System.err.println("  ./gradlew :cli:installDist");
        System.err.println("  ./cli/build/install/cli/bin/cli --cases=10 --seed=42");
        System.exit(1);
    }

    private static Integer askInt(BufferedReader in, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = readLine(in);
            if (s == null) return null;
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private static String askYesNo(BufferedReader in, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = readLine(in);
            if (s == null) return "n";
            s = s.trim().toLowerCase();
            if (s.startsWith("y") || s.startsWith("n")) return s;
            System.out.println("Please answer y or n.");
        }
    }

    private static Integer parseCounter(String line) {
        // Accept formats: "c 25000", "counter 25000"
        String[] parts = line.split("\\s+");
        if (parts.length < 2) return null;
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void printRemainingBrief(GameState s) {
        var opened = new HashSet<>(s.openedCaseIds());
        int remaining = 0;
        for (var c : s.cases()) if (!opened.contains(c.id())) remaining++;
        System.out.println("Remaining unopened cases (incl. your case): " + remaining);
    }
}
