package deal.cli;

import deal.analytics.TranscriptWriter;
import deal.analytics.TranscriptWriter.Step;
import deal.core.Engine;
import deal.core.GameConfig;
import deal.core.GameState;
import deal.core.Phase;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * v2 CLI with legacy parity + Stage 5c polish: - Player chooses how many cases to open each round
 * (no hard-coded schedule). - Immediate reveal after each open; board reprinted after each open. -
 * Banker offer after the chosen opens; EV/advisor line can be hidden with --show-ev=false. -
 * Optional transcript export via --transcript=out.json | out.csv. - --cases=custom prompts for any
 * integer in 2..25 before starting.
 */
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

        // Resolve case count (support --cases=custom via prompt)
        int caseCount = opt.caseCount;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        if (caseCount < 0) {
            caseCount = askIntMin(in, "Enter total number of cases (2..25): ", 2);
            if (caseCount <= 0) exitNoInput(); // defensive
        }

        // Build engine with resolved case count
        Engine engine;
        GameState s;
        try {
            var cfg = GameConfig.of(caseCount);
            engine = new Engine(cfg, opt.seed);
            s = engine.start();
        } catch (IllegalArgumentException ex) {
            System.err.println("Invalid configuration: " + ex.getMessage());
            System.exit(2);
            return;
        }

        // Transcript (created after we know the final, resolved case count)
        TranscriptWriter tx = null;
        int step = 0;
        try {
            if (opt.transcriptPath != null && !opt.transcriptPath.isBlank()) {
                tx = TranscriptWriter.fromPath(Paths.get(opt.transcriptPath));
                String header =
                        "{\"cases\":"
                                + caseCount
                                + ",\"seed\":"
                                + opt.seed
                                + ",\"timestamp\":\""
                                + Instant.now().toString()
                                + "\"}";
                tx.writeHeader(header);
            }

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
                System.err.println(
                        "  - Using run-v2.sh / run-v2.bat with --seed to replay"
                                + " deterministically.");
                System.exit(2);
                return;
            }

            // Pick player case
            while (s.phase() == Phase.PICK_CASE) {
                Integer playerId = askInt(in, "Pick your case id (1.." + s.cases().size() + "): ");
                if (playerId == null) exitNoInput();
                try {
                    s = engine.pickPlayerCase(s, playerId);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
                printRemainingBrief(s);
                printRemainingAmounts(s);
            }

            int round = 1;
            outer:
            while (true) {
                if (s.phase() == Phase.FINAL_REVEAL) {
                    System.out.println();
                    System.out.println("=== Final Reveal ===");
                    String yn = askYesNo(in, "Swap your case with the last unopened one? [y/n]: ");
                    boolean swap = yn != null && yn.toLowerCase(Locale.ROOT).startsWith("y");
                    s = engine.revealFinal(s, swap);

                    if (tx != null) {
                        tx.append(
                                new Step(
                                        ++step,
                                        round,
                                        "final_result",
                                        null,
                                        s.resultDollars(),
                                        remainingCaseIds(s),
                                        remainingAmountsList(s),
                                        null,
                                        null,
                                        null,
                                        null));
                    }

                    System.out.println("Result: " + fmt(s.resultDollars()));
                    break;
                }
                if (s.phase() == Phase.RESULT) {
                    System.out.println("Result: " + fmt(s.resultDollars()));
                    break;
                }

                // Round start (non-terminal)
                System.out.println();
                System.out.println("=== Round " + round + " ===");
                printRemainingBrief(s);
                printRemainingAmounts(s);

                if (tx != null) {
                    tx.append(
                            new Step(
                                    ++step,
                                    round,
                                    "start_round",
                                    null,
                                    null,
                                    remainingCaseIds(s),
                                    remainingAmountsList(s),
                                    null,
                                    null,
                                    null,
                                    null));
                }

                // Choose how many to open (validated by engine policy)
                Integer k = askInt(in, "How many cases to open this round? ");
                if (k == null) exitNoInput();
                try {
                    s = engine.chooseToOpen(s, k);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    continue; // re-ask at same round
                }

                int toOpen = s.toOpenInThisRound();
                int opened = 0;
                while (opened < toOpen) {
                    Integer id = askInt(in, "Open which case id? ");
                    if (id == null) exitNoInput();
                    int prize = amountOf(s, id);
                    try {
                        s = engine.openCase(s, id);
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                        continue; // retry this pick
                    }
                    System.out.println("Opened case #" + id + " \u2192 " + fmt(prize));
                    printRemainingAmounts(s);

                    if (tx != null) {
                        tx.append(
                                new Step(
                                        ++step,
                                        round,
                                        "open_case",
                                        id,
                                        prize,
                                        remainingCaseIds(s),
                                        remainingAmountsList(s),
                                        null,
                                        null,
                                        null,
                                        null));
                    }

                    opened++;
                }

                // Offer
                s = engine.computeOffer(s);
                int offer = s.currentOfferDollars() != null ? s.currentOfferDollars() : 0;
                double ev = evOfRemaining(s);
                double ratio = (ev <= 0.0) ? 0.0 : offer / ev;

                System.out.println("Banker offers: " + fmt(offer));
                if (opt.showEv) {
                    System.out.println(
                            "Advisor: EV \u2248 "
                                    + fmt((int) Math.round(ev))
                                    + " | Offer/EV \u2248 "
                                    + String.format(Locale.US, "%.2f", ratio));
                }
                System.out.println(
                        "Choose: [d] Deal  |  [n] No Deal  |  [c <amount>] Counteroffer  | "
                                + " [help]");
                System.out.print("> ");

                if (tx != null) {
                    tx.append(
                            new Step(
                                    ++step,
                                    round,
                                    "offer",
                                    null,
                                    null,
                                    remainingCaseIds(s),
                                    remainingAmountsList(s),
                                    offer,
                                    ev,
                                    null,
                                    null));
                }

                String line = readLine(in);
                if (line == null) exitNoInput();
                String lower = line.trim().toLowerCase(Locale.ROOT);

                if (lower.equals("d") || lower.equals("deal")) {
                    s = engine.acceptDeal(s);

                    if (tx != null) {
                        tx.append(
                                new Step(
                                        ++step,
                                        round,
                                        "deal",
                                        null,
                                        s.resultDollars(),
                                        remainingCaseIds(s),
                                        remainingAmountsList(s),
                                        offer,
                                        ev,
                                        true,
                                        null));
                    }

                    System.out.println("DEAL! You took " + fmt(s.resultDollars()));
                    break;
                } else if (lower.equals("n") || lower.equals("nodeal") || lower.equals("no deal")) {
                    s = engine.declineDeal(s);

                    if (tx != null) {
                        tx.append(
                                new Step(
                                        ++step,
                                        round,
                                        "nodeal",
                                        null,
                                        null,
                                        remainingCaseIds(s),
                                        remainingAmountsList(s),
                                        offer,
                                        ev,
                                        false,
                                        null));
                    }

                    round++;
                    continue;
                } else if (lower.startsWith("c")) {
                    Integer counter = parseCounter(line);
                    if (counter == null || counter <= 0) {
                        System.out.println("Usage: c <amount>  (example: c 75000)");
                        continue;
                    }
                    try {
                        s = engine.proposeCounter(s, counter);
                        s = engine.resolveCounter(s);

                        if (tx != null) {
                            tx.append(
                                    new Step(
                                            ++step,
                                            round,
                                            "counteroffer",
                                            null,
                                            null,
                                            remainingCaseIds(s),
                                            remainingAmountsList(s),
                                            offer,
                                            ev,
                                            null,
                                            counter));
                        }

                        if (s.phase() == Phase.RESULT) {
                            System.out.println(
                                    "Counter accepted! You win " + fmt(s.resultDollars()));
                            break;
                        } else if (s.phase() == Phase.FINAL_REVEAL) {
                            System.out.println("Counter rejected. Proceeding to final reveal...");
                            // loop naturally moves to FINAL_REVEAL at top
                        } else {
                            System.out.println("Counter rejected. Next round...");
                            round++;
                        }
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                } else if (lower.equals("help")) {
                    System.out.println("Commands:");
                    System.out.println("  d / deal            — accept the current offer");
                    System.out.println("  n / nodeal          — decline the offer and continue");
                    System.out.println("  c <amount>          — counteroffer the banker");
                    System.out.println("Tips:");
                    System.out.println("  • You pick how many cases to open each round.");
                    System.out.println("  • Every opened case reveals its amount immediately.");
                } else {
                    System.out.println("Unknown command. Type 'help' for options.");
                }
            }
        } finally {
            if (tx != null) {
                try {
                    tx.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    // ---------------- helpers ----------------

    private static String readLine(BufferedReader in) {
        try {
            return in.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    private static void exitNoInput() {
        System.err.println();
        System.err.println("No interactive input detected. Exiting.");
        System.exit(2);
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

    private static int askIntMin(BufferedReader in, String prompt, int min) {
        while (true) {
            Integer v = askInt(in, prompt);
            if (v == null) return -1;
            if (v >= min) return v;
            System.out.println("Please enter an integer >= " + min + ".");
        }
    }

    private static String askYesNo(BufferedReader in, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = readLine(in);
            if (s == null) return "n";
            s = s.trim().toLowerCase(Locale.ROOT);
            if (s.startsWith("y")) return "y";
            if (s.startsWith("n")) return "n";
            System.out.println("Please answer y/n.");
        }
    }

    private static Integer parseCounter(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 2) return null;
        try {
            return Integer.parseInt(parts[1].replaceAll("[_,]", ""));
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

    private static List<Integer> remainingAmountsList(GameState s) {
        var opened = new HashSet<>(s.openedCaseIds());
        List<Integer> amts = new ArrayList<>();
        for (deal.core.Briefcase c : s.cases())
            if (!opened.contains(c.id())) amts.add(c.amountDollars());
        Collections.sort(amts);
        return amts;
    }

    private static List<Integer> remainingCaseIds(GameState s) {
        var opened = new HashSet<>(s.openedCaseIds());
        List<Integer> ids = new ArrayList<>();
        for (deal.core.Briefcase c : s.cases()) if (!opened.contains(c.id())) ids.add(c.id());
        Collections.sort(ids);
        return ids;
    }

    private static int amountOf(GameState s, int caseId) {
        for (var c : s.cases()) if (c.id() == caseId) return c.amountDollars();
        throw new IllegalArgumentException("No such case id " + caseId);
    }

    private static double evOfRemaining(GameState s) {
        var opened = new HashSet<>(s.openedCaseIds());
        long sum = 0;
        int n = 0;
        for (var c : s.cases()) {
            if (!opened.contains(c.id())) {
                sum += c.amountDollars();
                n++;
            }
        }
        return (n == 0) ? 0.0 : (sum * 1.0) / n;
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
