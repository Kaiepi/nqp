package org.raku.nqp.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.reprs.AddressInstance;

public class SocketHandle extends SyncHandle implements IIOAddressable {

    final int family;
    final int type;
    final int protocol;

    public SocketHandle(
        final ThreadContext tc,
        final int           family,
        final int           type,
        final int           protocol
    ) {
        if (!checkFamily(tc, family, ProtocolFamily.INET) && !checkFamily(tc, family, ProtocolFamily.INET6))
            throw ExceptionHandling.dieInternal(tc,
                "socket expects PF_INET or PF_INET6 (got " + ProtocolFamily.asString(tc, family) + ")");
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
            this.chan = SocketChannel.open();
        } catch (final IOException e) {
            throw ExceptionHandling.dieInternal(tc, e);
        }
        setEncoding(tc, Charset.forName("UTF-8"));
    }

    public SocketHandle(
        final ThreadContext tc,
        final int           family,
        final int           type,
        final int           protocol,
        final SocketChannel existing
    ) {
        if (!checkFamily(tc, family, ProtocolFamily.INET) && !checkFamily(tc, family, ProtocolFamily.INET6))
            throw ExceptionHandling.dieInternal(tc,
                "socket expects PF_INET or PF_INET6 (got " + ProtocolFamily.asString(tc, family) + ")");
        if (!checkType(tc, type, SocketType.STREAM))
            throw ExceptionHandling.dieInternal(tc,
                "socket expects SOCK_STREAM (got " + SocketType.asString(tc, type) + ")");
        if (!checkProtocol(tc, protocol, ProtocolType.TCP))
            throw ExceptionHandling.dieInternal(tc,
                "socket expects IPPROTO_TCP (got " + ProtocolType.asString(tc, protocol) + ")");

        this.family   = family;
        this.type     = type;
        this.protocol = protocol;
        this.chan     = existing;
        setEncoding(tc, Charset.forName("UTF-8"));
    }

    public void connect(final ThreadContext tc, final AddressInstance address) {
        final int addressFamily = address.storage.getFamily();
        if (!checkFamily(tc, addressFamily, family))
            throw ExceptionHandling.dieInternal(tc,
                ProtocolFamily.asString(tc, family) + " socket got a " +
                ProtocolFamily.asString(tc, addressFamily) + " address");

        try {
            ((SocketChannel)chan).connect(address.storage.getAddress());
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
