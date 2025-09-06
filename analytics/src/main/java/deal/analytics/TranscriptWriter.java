package deal.analytics;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Minimal transcript interface. CSV/JSON writers live in this module. */
public interface TranscriptWriter extends Closeable {

    /** One row/event in the transcript. All dollar amounts are integers (USD). */
    final class Step {
        public final int step; // monotonic counter
        public final int round; // 1-based round index when relevant
        public final String
                action; // start_round, open_case, offer, deal, nodeal, counteroffer, final_result
        public final Integer openedCaseId; // nullable
        public final Integer openedPrize; // nullable
        public final List<Integer> remainingCases;
        public final List<Integer> remainingAmounts;
        public final Integer offer; // nullable
        public final Double ev; // nullable
        public final Boolean accepted; // nullable
        public final Integer counteroffer; // nullable

        public Step(
                int step,
                int round,
                String action,
                Integer openedCaseId,
                Integer openedPrize,
                List<Integer> remainingCases,
                List<Integer> remainingAmounts,
                Integer offer,
                Double ev,
                Boolean accepted,
                Integer counteroffer) {
            this.step = step;
            this.round = round;
            this.action = action;
            this.openedCaseId = openedCaseId;
            this.openedPrize = openedPrize;
            this.remainingCases = remainingCases;
            this.remainingAmounts = remainingAmounts;
            this.offer = offer;
            this.ev = ev;
            this.accepted = accepted;
            this.counteroffer = counteroffer;
        }
    }

    /** Write header/configuration preface (free-form JSON string is fine). */
    void writeHeader(String gameConfigJson) throws IOException;

    /** Append one step. */
    void append(Step s) throws IOException;

    /** Close underlying stream. */
    @Override
    void close() throws IOException;

    static TranscriptWriter fromPath(Path path) throws IOException {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv")) return new CsvTranscriptWriter(path);
        return new JsonTranscriptWriter(path); // default
    }
}
