package deal.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Smoke test for the v2 CLI in legacy mode.
 *
 * <p>Player chooses K cases to open; immediate reveal after each open; banker offer printed; we
 * accept DEAL to terminate. The script is intentionally short to keep CI fast and deterministic.
 */
final class CliFlowTest {

    private PrintStream origOut;
    private PrintStream origErr;
    private java.io.InputStream origIn;

    @BeforeEach
    void saveStd() {
        origOut = System.out;
        origErr = System.err;
        origIn = System.in;
    }

    @AfterEach
    void restoreStd() {
        System.setOut(origOut);
        System.setErr(origErr);
        System.setIn(origIn);
    }

    @Test
    void scriptedRun_tenCases_seed42_immediateReveal_andDeal() throws Exception {
        // Scripted inputs:
        // <ENTER> to start,
        // "1" pick player case #1,
        // "1" open 1 case this round,
        // "2" open case #2,
        // "deal" accept the first offer and exit.
        String script =
                String.join(
                                "\n",
                                "", // press ENTER to start
                                "1", // pick case #1
                                "1", // open K=1 this round
                                "2", // open case #2
                                "deal" // accept offer, terminate
                                )
                        + "\n";

        ByteArrayInputStream in =
                new ByteArrayInputStream(script.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream(64 * 1024);
        PrintStream out = new PrintStream(outBuf, true, java.nio.charset.StandardCharsets.UTF_8);

        System.setIn(in);
        System.setOut(out);
        System.setErr(out);

        // Run CLI with deterministic seed, no transcript, default show-ev (either is fine)
        Main.main(new String[] {"--cases=10", "--seed=42"});

        String output = outBuf.toString(java.nio.charset.StandardCharsets.UTF_8);
        // Structural assertions (don’t overfit to exact dollar values):
        assertTrue(output.contains("Welcome to Deal or No Deal"), "should greet");
        assertTrue(output.contains("Pick your case id"), "should ask to pick player case");
        assertTrue(output.contains("How many cases to open"), "should let player choose K");
        assertTrue(output.contains("Open which case id?"), "should prompt for a case to open");
        assertTrue(output.contains("Opened case #2"), "should immediately reveal opened case #2");
        assertTrue(output.contains("Banker offers"), "should show banker offer");
        assertTrue(output.contains("DEAL! You took"), "should print deal outcome");
    }
}
