package org.raku.nqp.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Optional;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.reprs.AddressInstance;

public class ServerSocketHandle implements IIOBindable, IIOClosable, IIOAddressable {

    ServerSocketChannel listenChan;
    public int listenPort;

    public ServerSocketHandle(ThreadContext tc) {
        try {
            listenChan = ServerSocketChannel.open();
        } catch (IOException e) {
            ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public void bind(final ThreadContext tc, final SocketAddress address, final int backlog) {
        try {
            listenChan.bind(address, backlog);
            listenPort = listenChan.socket().getLocalPort();
        } catch (IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    public SocketHandle accept(ThreadContext tc) {
        try {
            SocketChannel chan = listenChan.accept();
            return chan == null ? null : new SocketHandle(tc, chan);
        } catch (IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public SocketAddress getLocalAddress(final ThreadContext tc) {
        try {
            return Optional.ofNullable(listenChan.getLocalAddress())
                           .orElseThrow(() -> ExceptionHandling.dieInternal(tc,
                               "No local address for this socket exists. " +
                               "It may not be bound yet, or IPv6 may be misconfigured."));
        } catch (ClosedChannelException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        } catch (IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public SocketAddress getRemoteAddress(final ThreadContext tc) {
        throw ExceptionHandling.dieInternal(tc, "Cannot get the remote port of a passive socket");
    }

    @Override
    public void close(ThreadContext tc) {
        try {
            listenChan.close();
        } catch (IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }
}
