package org.raku.nqp.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.reprs.AddressInstance;

public class ServerSocketHandle implements IIOBindable, IIOClosable, IIOAddressable {

    final int family;
    final int type;
    final int protocol;

    final ServerSocketChannel listenChan;

    public ServerSocketHandle(final ThreadContext tc, final int family, final int type, final int protocol) {
        if (!checkFamily(tc, family, ProtocolFamily.INET) && !checkFamily(tc, family, ProtocolFamily.INET6))
            throw ExceptionHandling.dieInternal(tc,
                "socket expects PF_INET or PF_INET6 (got " + ProtocolFamily.asString(tc, family));
        if (!checkType(tc, type, SocketType.STREAM))
            throw ExceptionHandling.dieInternal(tc,
                "socket expects SOCK_STREAM (got " + SocketType.asString(tc, type) + ")");
        if (!checkProtocol(tc, protocol, ProtocolType.TCP))
            throw ExceptionHandling.dieInternal(tc,
                "socket expects IPPROTO_TCP (got " + ProtocolType.asString(tc, protocol) + ")");

        this.family   = family;
        this.type     = type;
        this.protocol = protocol;
        try {
            this.listenChan = ServerSocketChannel.open();
        } catch (final IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public void bind(final ThreadContext tc, final AddressInstance address, final int backlog) {
        final int addressFamily = address.storage.getFamily();
        if (!checkFamily(tc, addressFamily, family))
            throw ExceptionHandling.dieInternal(tc,
                ProtocolFamily.asString(tc, family) + " socket got a " +
                ProtocolFamily.asString(tc, addressFamily) + " address");

        try {
            listenChan.bind(address.storage.getAddress(), backlog);
        } catch (final IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    public SocketHandle accept(final ThreadContext tc) {
        try {
            SocketChannel chan = listenChan.accept();
            return chan == null ? null : new SocketHandle(tc, family, type, protocol, chan);
        } catch (final IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public void close(final ThreadContext tc) {
        try {
            listenChan.close();
        } catch (final IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public AddressInstance getSocketAddress(final ThreadContext tc) {
        try {
            return toAddress(tc, listenChan.getLocalAddress());
        } catch (final Exception e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
    }

    @Override
    public AddressInstance getPeerAddress(final ThreadContext tc) {
        throw ExceptionHandling.dieInternal(tc, "Cannot get the peer address of a connection");
    }
}
