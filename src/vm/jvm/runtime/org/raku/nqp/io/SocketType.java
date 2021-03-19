package org.raku.nqp.io;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.ThreadContext;

/* Stores hardcoded socket types. */
public final class SocketType {
    public static final int ANY       = 0;
    public static final int STREAM    = 1;
    public static final int DGRAM     = 2;
    public static final int RAW       = 3;
    public static final int RDM       = 4;
    public static final int SEQPACKET = 5;

    /* Stringifies a socket type. */
    public static String asString(final ThreadContext tc, final int type) {
        switch (type) {
            case ANY:
                return "SOCK_ANY";
            case STREAM:
                return "SOCK_STREAM";
            case DGRAM:
                return "SOCK_DGRAM";
            case RAW:
                return "SOCK_RAW";
            case RDM:
                return "SOCK_RDM";
            case SEQPACKET:
                return "SOCK_SEQPACKET";
            default:
                throw ExceptionHandling.dieInternal(tc, "Unknown socket type: " + type);
        }
    }
}
