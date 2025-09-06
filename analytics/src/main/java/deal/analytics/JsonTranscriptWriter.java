package deal.analytics;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Very small JSON array writer: {"config":{...},"steps":[ ... ]} */
final class JsonTranscriptWriter implements TranscriptWriter {
    private final Writer out;
    private boolean first = true;

    JsonTranscriptWriter(Path path) throws IOException {
        this.out = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8);
    }

    @Override
    public void writeHeader(String gameConfigJson) throws IOException {
        out.write("{\"config\":");
        out.write(gameConfigJson == null ? "null" : gameConfigJson);
        out.write(",\"steps\":[");
    }

    @Override
    public void append(Step s) throws IOException {
        if (!first) out.write(",");
        first = false;
        out.write(JsonUtil.toJson(s));
    }

    @Override
    public void close() throws IOException {
        out.write("]}");
        out.flush();
        out.close();
    }
}
