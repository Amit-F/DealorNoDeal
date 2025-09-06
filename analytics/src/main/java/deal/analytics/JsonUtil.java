package deal.analytics;

import java.lang.reflect.Field;

/** Micro JSON serializer for flat POJOs and Lists. */
final class JsonUtil {
    private JsonUtil() {}

    static String toJson(Object pojo) {
        if (pojo == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Field f : pojo.getClass().getFields()) {
            try {
                Object v = f.get(pojo);
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(f.getName()).append("\":").append(stringify(v));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        sb.append("}");
        return sb.toString();
    }

    static String stringify(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        if (v instanceof String) return "\"" + escape((String) v) + "\"";
        if (v instanceof java.util.List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object o : list) {
                if (!first) sb.append(",");
                first = false;
                sb.append(stringify(o));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escape(String.valueOf(v)) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
