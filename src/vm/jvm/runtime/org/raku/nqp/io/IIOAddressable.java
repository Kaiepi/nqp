package org.raku.nqp.io;

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.SocketAddress;

import org.raku.nqp.runtime.ExceptionHandling;
import org.raku.nqp.runtime.ThreadContext;
import org.raku.nqp.sixmodel.SixModelObject;
import org.raku.nqp.sixmodel.reprs.AddressInstance;

public interface IIOAddressable {
    public AddressInstance getSocketAddress(final ThreadContext tc);

    public AddressInstance getPeerAddress(final ThreadContext tc);

    default AddressInstance toAddress(final ThreadContext tc, final SocketAddress socketAddress) {
        if (socketAddress instanceof InetSocketAddress) {
            final SixModelObject  BOOTAddress   = tc.gc.BOOTAddress;
            final AddressInstance address       = (AddressInstance)BOOTAddress.st.REPR.allocate(tc, BOOTAddress.st);
            final InetAddress     nativeAddress = ((InetSocketAddress)socketAddress).getAddress();
            final int             port          = ((InetSocketAddress)socketAddress).getPort();
            address.storage = nativeAddress instanceof Inet6Address
                            ? new IPv6AddressStorage((Inet6Address)nativeAddress, port)
                            : new IPv4AddressStorage((Inet4Address)nativeAddress, port);
            return address;
        }
        else
            throw ExceptionHandling.dieInternal(tc, "Attempted to get an unknkown family of address");
    }

    default boolean checkFamily(final ThreadContext tc, final int family, final int to) {
        switch (family) {
            case ProtocolFamily.UNSPEC:
            case ProtocolFamily.INET:
            case ProtocolFamily.INET6:
            case ProtocolFamily.UNIX:
                return to == ProtocolFamily.UNSPEC || family == to;
            default:
                return false;
        }
    }

    default boolean checkType(final ThreadContext tc, final int type, final int to) {
        switch (type) {
            case SocketType.ANY:
            case SocketType.STREAM:
            case SocketType.DGRAM:
            case SocketType.RAW:
            case SocketType.RDM:
            case SocketType.SEQPACKET:
                return to == SocketType.ANY || type == to;
            default:
                return false;
        }
    }

    default boolean checkProtocol(final ThreadContext tc, final int protocol, final int to) {
        switch (protocol) {
            case ProtocolType.ANY:
            case ProtocolType.TCP:
            case ProtocolType.UDP:
                return to == ProtocolType.ANY || protocol == to;
            default:
                return false;
        }
    }
}
