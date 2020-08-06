package org.raku.nqp.io;

import java.net.SocketAddress;

import org.raku.nqp.runtime.ThreadContext;

public interface IIOAddressable {
    SocketAddress getLocalAddress(final ThreadContext tc);
    SocketAddress getRemoteAddress(final ThreadContext tc);
}
