package org.raku.nqp.io;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SocketType {
    private final String name;
    private final int    value;

    SocketType(final String name, final int value) {
        this.name  = name;
        this.value = value;
    }

    public final String getName() {
        return name;
    }

    public final int getValue() {
        return value;
    }

    public final Boolean equals(final SocketType type) {
        return type != null && value == type.getValue();
    }

    public final String toString() {
        return name;
    }

    private static final Set<SocketType> constants = new HashSet<>() {{
        for (Map.Entry<String, Integer> entry : new HashMap<String, Integer>() {{
            put("ANY", 0);
            put("STREAM", 1);
            put("DGRAM", 2);
            put("RAW", 3);
            put("RDM", 4);
            put("SEQPACKET", 5);
        }}.entrySet()) {
            SocketType type = new SocketType("SOCK_" + entry.getKey(), entry.getValue().intValue());
            add(type);
        }
    }};

    public static final Set<SocketType> getConstants() {
        return constants;
    }

    public static final SocketType getByName(final String name) {
        return constants.stream()
                        .filter(t -> t.getName().equals(name))
                        .findFirst()
                        .orElse(null);
    }

    public static final SocketType getByValue(final int value) {
        return constants.stream()
                        .filter(t -> t.getValue() == value)
                        .findFirst()
                        .orElse(null);
    }
}
