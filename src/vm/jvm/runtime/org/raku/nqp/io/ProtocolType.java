package org.raku.nqp.io;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.ThreadContext;

/* Stores hardcoded protocol types. */
public final class ProtocolType {
    public static final int ANY = 0;
    public static final int TCP = 1;
    public static final int UDP = 2;

    /* Stringifies a protocol type. */
    public static String asString(final ThreadContext tc, final int protocol) {
        switch (protocol) {
            case ANY:
                return "IPPROTO_ANY";
            case TCP:
                return "IPPROTO_TCP";
            case UDP:
                return "IPPROTO_UDP";
            default:
                throw ExceptionHandling.dieInternal(tc, "Unknown protocol type: " + protocol);
        }
    }
}
