package org.raku.nqp.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.ThreadContext;

public class SocketHandle extends SyncHandle {

    public SocketHandle(ThreadContext tc) {
        try {
            chan = SocketChannel.open();
            setEncoding(tc, Charset.forName("UTF-8"));
        } catch (IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    public SocketHandle(ThreadContext tc, SocketChannel existing) {
        chan = existing;
        setEncoding(tc, Charset.forName("UTF-8"));
    }

    public void connect(final ThreadContext tc, final SocketAddress address) {
        try {
            ((SocketChannel)chan).connect(address);
        } catch (IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public void flush(ThreadContext tc) {
        // Not provided.
    }
}
