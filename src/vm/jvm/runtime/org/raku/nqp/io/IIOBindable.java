package org.raku.nqp.io;

import java.net.SocketAddress;

import org.raku.nqp.runtime.ThreadContext;

public interface IIOBindable {
    void bind(final ThreadContext tc, final SocketAddress address, final int backlog);
}
