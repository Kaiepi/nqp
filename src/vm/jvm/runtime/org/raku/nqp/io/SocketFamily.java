package org.raku.nqp.io;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SocketFamily {
    private final String name;
    private final short  value;

    SocketFamily(final String name, final short value) {
        this.name  = name;
        this.value = value;
    }

    public final String getName() {
        return name;
    }

    public final short getValue() {
        return value;
    }

    public final Boolean equals(final SocketFamily family) {
        return family != null && value == family.getValue();
    }

    public final String toString() {
        return name;
    }

    private static final Set<SocketFamily> constants = new HashSet<>() {{
        for (Map.Entry<String, Integer> entry : new HashMap<String, Integer>() {{
            put("UNSPEC", 0);
            put("INET", 1);
            put("INET6", 2);
            put("UNIX", 3);
        }}.entrySet()) {
            SocketFamily family = new SocketFamily("PF_" + entry.getKey(), entry.getValue().shortValue());
            add(family);
        }
    }};

    public static final Set<SocketFamily> getConstants() {
        return constants;
    }

    public static final SocketFamily getByName(final String name) {
        return constants.stream()
                        .filter(f -> f.getName().equals(name))
                        .findFirst()
                        .orElse(null);
    }

    public static final SocketFamily getByValue(final short value) {
        return constants.stream()
                        .filter(f -> f.getValue() == value)
                        .findFirst()
                        .orElse(null);
    }
}
