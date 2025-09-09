package deal.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Verifies that --transcript writes a JSON file with steps. */
final class CliTranscriptTest {

    @Test
    void transcriptJson_isCreated_andNonEmpty() throws Exception {
        Path tmp = Files.createTempFile("deal-transcript-", ".json");
        try {
            String script =
                    String.join(
                                    "\n", "", // ENTER to start
                                    "1", // pick case #1
                                    "1", // open K=1 this round
                                    "2", // open case #2
                                    "deal")
                            + "\n";

            ByteArrayInputStream in =
                    new ByteArrayInputStream(
                            script.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            ByteArrayOutputStream outBuf = new ByteArrayOutputStream(64 * 1024);
            PrintStream out =
                    new PrintStream(outBuf, true, java.nio.charset.StandardCharsets.UTF_8);

            System.setIn(in);
            System.setOut(out);
            System.setErr(out);

            Main.main(new String[] {"--cases=10", "--seed=42", "--transcript=" + tmp.toString()});

            byte[] bytes = Files.readAllBytes(tmp);
            String txt = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(bytes.length > 0, "transcript should be non-empty");
            assertTrue(txt.startsWith("{"), "json should start with '{'");
            assertTrue(txt.contains("\"steps\":"), "json should contain steps array");
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (Exception ignore) {
            }
        }
    }
}
