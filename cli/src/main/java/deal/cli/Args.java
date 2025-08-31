package deal.cli;

import java.util.*;

/** Minimal, dependency-free parser for --key=value, --flag and positional args. */
final class Args {
    private final Map<String, List<String>> map = new LinkedHashMap<>();
    private final List<String> positionals = new ArrayList<>();

    static Args parse(String[] argv) {
        Args a = new Args();
        for (String s : argv) {
            if (s == null || s.isEmpty()) continue;
            if (s.startsWith("--")) {
                String body = s.substring(2);
                int eq = body.indexOf('=');
                if (eq < 0) {
                    // boolean flag like --verbose
                    a.map.computeIfAbsent(body, k -> new ArrayList<>()).add("true");
                } else {
                    String key = body.substring(0, eq);
                    String val = body.substring(eq + 1);
                    a.map.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
                }
            } else {
                // treat non-- tokens as positionals (we can extend to -h/-v later if needed)
                a.positionals.add(s);
            }
        }
        return a;
    }

    boolean has(String key) {
        return map.containsKey(key);
    }

    /** First value for a key or null. */
    String get(String key) {
        List<String> v = map.get(key);
        return (v == null || v.isEmpty()) ? null : v.get(0);
    }

    /** Parse an int option with default and validation hook. */
    int getInt(String key, int defaultValue, IntValidator validator) {
        String v = get(key);
        if (v == null) return defaultValue;
        try {
            int x = Integer.parseInt(v.trim());
            if (validator != null) validator.check(x);
            return x;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer for --" + key + ": " + v);
        }
    }

    List<String> positionals() {
        return Collections.unmodifiableList(positionals);
    }

    @FunctionalInterface
    interface IntValidator {
        void check(int value);
    }
}
