package deal.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

/** Verifies that --show-ev=false suppresses the advisor line. */
final class CliEvToggleTest {

    @Test
    void advisorHidden_whenFlagFalse() throws Exception {
        String script =
                String.join(
                                "\n", "", // ENTER to start
                                "1", // pick case #1
                                "1", // open K=1
                                "2", // open case #2
                                "deal")
                        + "\n";

        ByteArrayInputStream in =
                new ByteArrayInputStream(script.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream(64 * 1024);
        PrintStream out = new PrintStream(outBuf, true, java.nio.charset.StandardCharsets.UTF_8);

        System.setIn(in);
        System.setOut(out);
        System.setErr(out);

        Main.main(new String[] {"--cases=10", "--seed=42", "--show-ev=false"});

        String output = outBuf.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(output.contains("Banker offers"), "offer should appear");
        assertFalse(output.contains("Advisor:"), "advisor line should be hidden");
    }
}
