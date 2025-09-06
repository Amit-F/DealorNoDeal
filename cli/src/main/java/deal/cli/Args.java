package deal.cli;

import java.util.*;

/** Minimal, dependency-free CLI args parser supporting --key=value, --flag, and positionals. */
final class Args {
    private final Map<String, List<String>> map = new LinkedHashMap<>();
    private final List<String> positionals = new ArrayList<>();

    static Args parse(String[] argv) {
        Args a = new Args();
        if (argv == null) return a;
        for (String s : argv) {
            if (s == null || s.isEmpty()) continue;
            if (s.startsWith("--")) {
                String body = s.substring(2);
                int eq = body.indexOf('=');
                String k = (eq >= 0) ? body.substring(0, eq) : body;
                String v = (eq >= 0) ? body.substring(eq + 1) : "true";
                a.map.computeIfAbsent(k, __ -> new ArrayList<>()).add(v);
            } else {
                a.positionals.add(s);
            }
        }
        return a;
    }

    boolean has(String key) {
        return map.containsKey(key);
    }

    /** Returns the first value for a key, or null if absent. */
    String get(String key) {
        List<String> xs = map.get(key);
        return (xs == null || xs.isEmpty()) ? null : xs.get(0);
    }

    /** Returns the first value for a key, or the provided default if absent. */
    String getOne(String key, String def) {
        String v = get(key);
        return v == null ? def : v;
    }

    /** Returns all values for a key (possibly empty). */
    List<String> getAll(String key) {
        List<String> xs = map.get(key);
        return xs == null ? List.of() : Collections.unmodifiableList(xs);
    }

    /** Returns immutable list of positional args. */
    List<String> positionals() {
        return Collections.unmodifiableList(positionals);
    }
}
