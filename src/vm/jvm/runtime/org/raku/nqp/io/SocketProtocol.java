package org.raku.nqp.io;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SocketProtocol {
    private final String name;
    private final int    value;

    SocketProtocol(final String name, final int value) {
        this.name  = name;
        this.value = value;
    }

    public final String getName() {
        return name;
    }

    public final int getValue() {
        return value;
    }

    public final Boolean equals(final SocketProtocol protocol) {
        return protocol != null && value == protocol.getValue();
    }

    public final String toString() {
        return name;
    }

    private static final Set<SocketProtocol> constants = new HashSet<>() {{
        for (Map.Entry<String, Integer> entry : new HashMap<String, Integer>() {{
            put("ANY", 0);
            put("TCP", 1);
            put("UDP", 2);
        }}.entrySet()) {
            SocketProtocol protocol = new SocketProtocol("IPPROTO_" + entry.getKey(), entry.getValue().intValue());
            add(protocol);
        }
    }};

    public static final Set<SocketProtocol> getConstants() {
        return constants;
    }

    public static final SocketProtocol getByName(final String name) {
        return constants.stream()
                        .filter(p -> p.getName().equals(name))
                        .findFirst()
                        .orElse(null);
    }

    public static final SocketProtocol getByValue(final int value) {
        return constants.stream()
                        .filter(p -> p.getValue() == value)
                        .findFirst()
                        .orElse(null);
    }
}
