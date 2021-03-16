package org.raku.nqp.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.reprs.AddressInstance;

public class SocketHandle extends SyncHandle implements IIOAddressable {

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

    public void connect(ThreadContext tc, String host, int port) {
        try {
            InetSocketAddress addr = new InetSocketAddress(host, port);
            ((SocketChannel)chan).connect(addr);
        } catch (IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public void flush(ThreadContext tc) {
        // Not provided.
    }

    @Override
    public AddressInstance getSocketAddress(final ThreadContext tc) {
        try {
            return toAddress(tc, ((SocketChannel)chan).getLocalAddress());
        } catch (final Exception e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public AddressInstance getPeerAddress(final ThreadContext tc) {
        try {
            return toAddress(tc, ((SocketChannel)chan).getRemoteAddress());
        } catch (final Exception e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }
}
