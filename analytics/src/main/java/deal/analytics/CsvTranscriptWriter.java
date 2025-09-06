package deal.analytics;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

/** Simple CSV with a header row. Lists are space-separated inside a single cell. */
final class CsvTranscriptWriter implements TranscriptWriter {
    private final Writer out;

    CsvTranscriptWriter(Path path) throws IOException {
        this.out = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8);
    }

    @Override
    public void writeHeader(String gameConfigJson) throws IOException {
        out.write(
                "step,round,action,openedCaseId,openedPrize,remainingCases,remainingAmounts,offer,ev,accepted,counteroffer\n");
        if (gameConfigJson != null && !gameConfigJson.isBlank()) {
            out.write("# config: " + gameConfigJson.replace("\n", " ") + "\n");
        }
    }

    @Override
    public void append(Step s) throws IOException {
        out.write(
                s.step
                        + ","
                        + s.round
                        + ","
                        + q(s.action)
                        + ","
                        + n(s.openedCaseId)
                        + ","
                        + n(s.openedPrize)
                        + ","
                        + q(join(s.remainingCases))
                        + ","
                        + q(join(s.remainingAmounts))
                        + ","
                        + n(s.offer)
                        + ","
                        + n(s.ev)
                        + ","
                        + n(s.accepted)
                        + ","
                        + n(s.counteroffer)
                        + "\n");
    }

    @Override
    public void close() throws IOException {
        out.flush();
        out.close();
    }

    private static String q(String s) {
        return "\"" + (s == null ? "" : s.replace("\"", "\"\"")) + "\"";
    }

    private static String n(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String join(Collection<?> xs) {
        if (xs == null) return "";
        return xs.stream().map(Object::toString).collect(Collectors.joining(" "));
    }
}
