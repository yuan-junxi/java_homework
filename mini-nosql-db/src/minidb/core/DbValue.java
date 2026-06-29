package minidb.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DbValue {
    public enum Type {
        STRING, NUMBER, SET, MAP
    }

    private final Type type;
    private final Object value;

    private DbValue(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public static DbValue parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("value body is empty");
        }
        String text = raw.trim();
        int split = text.indexOf(':');
        if (split <= 0) {
            return new DbValue(Type.STRING, text);
        }
        String prefix = text.substring(0, split).trim().toLowerCase();
        String body = text.substring(split + 1);
        switch (prefix) {
            case "string":
                return new DbValue(Type.STRING, body);
            case "number":
                return new DbValue(Type.NUMBER, Double.parseDouble(body.trim()));
            case "set":
                return new DbValue(Type.SET, parseSet(body));
            case "map":
                return new DbValue(Type.MAP, parseMap(body));
            default:
                return new DbValue(Type.STRING, text);
        }
    }

    public DbValue patch(DbValue patch) {
        if (patch == null || this.type != patch.type) {
            return patch;
        }
        if (type == Type.SET) {
            LinkedHashSet<String> merged = new LinkedHashSet<>(asSet());
            merged.addAll(patch.asSet());
            return new DbValue(Type.SET, merged);
        }
        if (type == Type.MAP) {
            LinkedHashMap<String, String> merged = new LinkedHashMap<>(asMap());
            merged.putAll(patch.asMap());
            return new DbValue(Type.MAP, merged);
        }
        return patch;
    }

    public Type getType() {
        return type;
    }

    public String toWire() {
        switch (type) {
            case STRING:
                return "string:" + value;
            case NUMBER:
                double n = (Double) value;
                if (n == Math.rint(n)) {
                    return "number:" + Long.toString((long) n);
                }
                return "number:" + n;
            case SET:
                return "set:" + String.join(",", asSet());
            case MAP:
                List<String> items = new ArrayList<>();
                for (Map.Entry<String, String> entry : asMap().entrySet()) {
                    items.add(entry.getKey() + "=" + entry.getValue());
                }
                return "map:" + String.join(",", items);
            default:
                return String.valueOf(value);
        }
    }

    @Override
    public String toString() {
        return toWire();
    }

    @SuppressWarnings("unchecked")
    private Set<String> asSet() {
        return (Set<String>) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> asMap() {
        return (Map<String, String>) value;
    }

    private static Set<String> parseSet(String body) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (body.trim().isEmpty()) {
            return set;
        }
        for (String item : body.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return set;
    }

    private static Map<String, String> parseMap(String body) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        if (body.trim().isEmpty()) {
            return map;
        }
        for (String item : body.split(",")) {
            int split = item.indexOf('=');
            if (split > 0) {
                String key = item.substring(0, split).trim();
                String val = item.substring(split + 1).trim();
                if (!key.isEmpty()) {
                    map.put(key, val);
                }
            }
        }
        return map;
    }
}

