package org.raku.nqp.io;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.ThreadContext;

/* Stores hardcoded protocol families. */
public final class ProtocolFamily {
    public static final int UNSPEC = 0;
    public static final int INET   = 1;
    public static final int INET6  = 2;
    public static final int UNIX   = 3;

    /* Stringifies a protocol family. */
    public static String asString(final ThreadContext tc, final int family) {
        switch (family) {
            case UNSPEC:
                return "PF_UNSPEC";
            case INET:
                return "PF_INET";
            case INET6:
                return "PF_INET6";
            case UNIX:
                return "PF_UNIX";
            default:
                throw ExceptionHandling.dieInternal(tc, "Unknown protocol family: " + family);
        }
    }
}
