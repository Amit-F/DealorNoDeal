package deal.cli;

import deal.core.Engine;
import deal.core.GameConfig;
import deal.core.GameState;
import deal.core.Phase;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public final class Main {

    private static final NumberFormat USD = NumberFormat.getCurrencyInstance(Locale.US);

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
        printRemainingAmounts(s);

        System.out.print("Press ENTER to start (or Ctrl+C to quit): ");
        String probe = readLine(in);
        if (probe == null) {
            System.err.println();
            System.err.println("No interactive input detected.");
            System.err.println("Try either:");
            System.err.println("  - Running the app in a terminal, OR");
            System.err.println("  - Using the installed script:");
            System.err.println(
                    "      ./gradlew :cli:installDist && ./cli/build/install/cli/bin/cli --cases=10"
                            + " --seed=42");
            System.exit(1);
            return;
        }

        // PICK_CASE
        while (s.phase() == Phase.PICK_CASE) {
            Integer pick = askInt(in, "Pick your case (1.." + s.cases().size() + "): ");
            if (pick == null) exitNoInput();
            try {
                s = engine.pickPlayerCase(s, pick);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        // Main loop
        while (true) {
            if (s.phase() == Phase.ROUND) {
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
                    continue;
                }

                int kToOpen = s.toOpenInThisRound();
                int openedThisRound = 0;
                while (openedThisRound < kToOpen) {
                    Integer id = askInt(in, "Open which case id? ");
                    if (id == null) exitNoInput();
                    try {
                        int amt = amountOf(s, id);
                        s = engine.openCase(s, id);
                        openedThisRound++;
                        System.out.println("Opened case " + id + " → " + fmt(amt));
                        printRemainingBrief(s);
                        printRemainingAmounts(s);
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }

                s = engine.computeOffer(s);
            }

            if (s.phase() == Phase.OFFER) {
                System.out.println();
                int offer = s.currentOfferDollars() != null ? s.currentOfferDollars() : 0;
                double ev = evOfRemaining(s);
                double ratio = (ev <= 0.0) ? 0.0 : offer / ev;
                System.out.println("Banker offers: " + fmt(offer));
                System.out.println(
                        "Advisor: EV ≈ "
                                + fmt((int) Math.round(ev))
                                + " | Offer/EV ≈ "
                                + String.format("%.2f", ratio));
                System.out.println(
                        "Choose: [d] Deal  |  [n] No Deal  |  [c <amount>] Counteroffer  | "
                                + " [help]");
                System.out.print("> ");
                String line = readLine(in);
                if (line == null) exitNoInput();
                line = line.trim().toLowerCase();

                if (line.equals("help")) {
                    System.out.println(
                            "Commands: d (deal), n (no deal), c <amount> (counteroffer), help");
                    continue;
                }

                if (line.equals("d") || line.startsWith("deal")) {
                    s = engine.acceptDeal(s);
                    System.out.println("DEAL! Winnings: " + fmt(s.resultDollars()));
                    break;
                } else if (line.equals("n") || line.startsWith("no")) {
                    s = engine.declineDeal(s);
                    if (s.phase() != Phase.FINAL_REVEAL) {
                        printRemainingBrief(s);
                        printRemainingAmounts(s);
                    }
                } else if (line.startsWith("c")) {
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
                                            + fmt(s.resultDollars()));
                            break;
                        } else {
                            System.out.println("Banker rejected your counter. Continuing...");
                            if (s.phase() != Phase.FINAL_REVEAL) {
                                printRemainingBrief(s);
                                printRemainingAmounts(s);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                } else {
                    System.out.println("Unrecognized input. Use: d | n | c <amount> | help");
                }
            }

            if (s.phase() == Phase.COUNTEROFFER) {
                continue; // transient
            }

            if (s.phase() == Phase.FINAL_REVEAL) {
                System.out.println();
                System.out.println("FINAL REVEAL: Only two cases remain (including yours).");
                String swapAns = askYesNo(in, "Swap your case? (y/n): ");
                boolean swap = swapAns.startsWith("y");
                s = engine.revealFinal(s, swap);
                System.out.println(
                        (swap ? "You swapped." : "You kept your case.")
                                + " Final winnings: "
                                + fmt(s.resultDollars()));
                break;
            }

            if (s.phase() == Phase.RESULT) {
                System.out.println("Game over. Winnings: " + fmt(s.resultDollars()));
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

    private static void printRemainingAmounts(GameState s) {
        var opened = new HashSet<>(s.openedCaseIds());
        List<Integer> amts = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        for (var c : s.cases()) {
            if (!opened.contains(c.id())) {
                amts.add(c.amountDollars());
                ids.add(c.id());
            }
        }
        Collections.sort(amts);
        Collections.sort(ids);
        System.out.println("Remaining amounts: " + joinMoney(amts));
        System.out.println("Unopened case IDs: " + joinInts(ids));
    }

    private static int amountOf(GameState s, int caseId) {
        for (var c : s.cases()) if (c.id() == caseId) return c.amountDollars();
        throw new IllegalArgumentException("No such case id " + caseId);
    }

    private static double evOfRemaining(GameState s) {
        var opened = new HashSet<>(s.openedCaseIds());
        int sum = 0;
        int count = 0;
        for (var c : s.cases()) {
            if (!opened.contains(c.id())) {
                sum += c.amountDollars();
                count++;
            }
        }
        return count == 0 ? 0.0 : (double) sum / count;
    }

    private static String joinInts(List<Integer> xs) {
        if (xs.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < xs.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(xs.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String joinMoney(List<Integer> xs) {
        if (xs.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < xs.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(fmt(xs.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String fmt(Integer dollars) {
        if (dollars == null) return USD.format(0);
        return USD.format(dollars);
    }
}
